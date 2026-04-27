## Context

EvolveLog is a Spring Boot 4.0 / Java 25 / Gradle application backed by PostgreSQL with Flyway migrations (currently at V9). The frontend is React + Vite + TypeScript. The existing codebase already follows consistent patterns: layered architecture (controller → service → repository), Flyway-managed schema, `StorageService`/`LocalStorageService` abstraction for file storage (a direct precedent for the AI provider abstraction), SSE is not yet used anywhere.

The app has rich data across domains: workout sessions, exercise progression, blood tests, body measurements, supplement logs, and Withings health metrics. All of these are potential AI tool targets. Existing repositories already have the query methods needed — no schema changes to existing tables.

No Anthropic SDK or HTTP client library beyond Spring's `RestTemplate` is currently present.

---

## Goals / Non-Goals

**Goals:**
- Introduce a provider-agnostic AI abstraction so business logic never references Anthropic types directly — the adapter is the only Anthropic-aware code
- Automatic model routing: the app selects the cheapest appropriate model per task, invisible to the user
- Tool calling: AI queries specific data on demand rather than receiving a bulk data dump
- DB-persisted chat history: conversations survive server restarts and page refreshes
- Nightly pre-computed monthly aggregates for fast multi-year historical queries
- Scheduled insight generation (daily + weekly) stored as Markdown, displayed instantly on load
- SSE streaming with typed events so the frontend can show tool execution status in real time
- System prompts stored as classpath resources — version-controlled, never exposed via API

**Non-Goals:**
- Vector search / RAG — data volume for personal use fits in a single context window; SQL-based tools are more precise and cheaper
- Multi-turn tool chaining deeper than 3 rounds per request — diminishing returns, adds latency
- Real-time push of new insights to the frontend — polling on the Insights page is sufficient
- Supporting more than one AI provider per user simultaneously

---

## Decisions

### D1 — AI Provider abstraction: interface + adapter, using `java.net.http.HttpClient`

**Decision:** Define a provider-agnostic interface in `ai.provider`. The `ClaudeAdapter` is the only class that knows about Anthropic. Use Java 25's built-in `java.net.http.HttpClient` for all HTTP calls — no SDK dependency.

**Why not the Anthropic Java SDK?** The SDK's types (`MessageParam`, `ContentBlock`, etc.) would bleed into the adapter boundary even if wrapped, because streaming requires SDK-specific handlers. Using raw HTTP with `HttpClient.sendAsync()` + `HttpResponse.BodyHandlers.ofLines()` gives full control over the SSE parsing, no extra dependency, and keeps the adapter boundary clean. Java 25's HttpClient is mature and handles chunked streaming natively.

**Why not OpenAI SDK or LangChain4j?** LangChain4j supports multiple providers but introduces its own opinionated abstractions that conflict with our domain model. Building a thin custom interface costs less than fighting a framework.

**Provider interface:**
```java
public interface AiProvider {
    AiResponse complete(AiRequest request);                  // blocking, for scheduled jobs
    void stream(AiRequest request, AiStreamSink sink);       // SSE, for chat
    String providerId();                                     // "anthropic" | "openai" | "google"
}
```

`AiRequest` carries: model ID, system prompt, `List<AiMessage>` (role + content), `List<AiToolDefinition>`, temperature.
`AiStreamSink` is a callback interface: `onToken(String)`, `onToolUse(String name, Map<String,Object> input)`, `onDone()`, `onError(Throwable)`.
The controller's `SseEmitter` implements `AiStreamSink`.

---

### D2 — Model routing: deterministic for scheduled tasks, heuristic for chat

**Decision:** `ModelRouter` is a Spring `@Component` with two routing paths:

1. **Task-based (deterministic):** `selectModel(AiTaskType)` uses a `switch` expression — no runtime cost, no external call.
   ```
   DAILY_SUMMARY, POST_WORKOUT  →  claude-haiku-4-5-20251001
   WEEKLY_REPORT                →  claude-sonnet-4-6
   BLOOD_ANALYSIS               →  claude-opus-4-6
   ```

2. **Chat (heuristic-first, classifier fallback):** Check message length + keyword patterns first. If confidence is high, route immediately. If ambiguous, call Haiku to classify into `SIMPLE | ANALYTICAL | MEDICAL` (one small API call, ~100 tokens) then route.
   ```
   SIMPLE    → Haiku   (short factual questions, < 80 chars, no analytical keywords)
   ANALYTICAL → Sonnet  (trend analysis, multi-week, progression, comparison)
   MEDICAL   → Opus    (blood markers, HRV, fatigue correlation, health audit)
   ```

**Why heuristics first?** The classifier call adds ~300ms latency. For obvious cases (short factual questions) this is wasteful. Heuristics handle ~80% of real-world gym queries correctly.

---

### D3 — Tool calling: native provider tool calling, custom `AiTool` interface for registration

**Decision:** Each tool implements:
```java
public interface AiTool {
    String name();                      // snake_case, e.g. "get_exercise_history"
    String description();               // natural language for the model
    Map<String, Object> inputSchema();  // JSON Schema as Map (serialized by adapter)
    Object execute(Map<String, Object> args, UUID userId);
}
```

The `AiToolRegistry` collects all `@Component`-annotated `AiTool` beans via `List<AiTool>` injection. The `ClaudeAdapter` converts `AiToolDefinition` to Anthropic's native tool schema format before the API call, and routes `tool_use` blocks back to the registry for execution.

**Why native tool calling over manual prompting?** Native tool calling (Anthropic's `tools` parameter) is more reliable — the model is fine-tuned to invoke tools precisely rather than generating structured text we parse. It also allows multi-step tool use in one response (e.g., fetch PRs then fetch volume, then synthesize).

**Tools to implement:**
| Tool | Data source | Pre-computation needed |
|------|-------------|----------------------|
| `get_exercise_history` | `ExerciseRepository.findProgressByUserIdAndExerciseName` | none |
| `get_personal_records` | `ExerciseRepository.findPersonalRecordsByUserId` | none |
| `get_volume_stats` | `ExerciseRepository.findWeeklyVolumeByMuscle` | none |
| `get_monthly_aggregates` | `MonthlyAggregateRepository` | nightly job |
| `get_blood_results` | `BloodTestResultRepository` | none |
| `get_measurements` | `MeasurementRepository` | none |
| `get_recent_workouts` | `WorkoutSessionRepository` | none |

**Determinism rule:** Every tool returns pre-computed numerical results from SQL. The AI interprets these numbers — it never receives raw lists and is never asked to compute sums, averages, or percentages itself.

---

### D4 — Chat history: DB-persisted, conversation scoped, last 10 messages in context

**Decision:** `ai_chat_history` table stores all messages. On each chat request, the backend fetches the last 10 messages for the given `conversation_id` and prepends them to the `AiRequest` messages list. `conversation_id` is a UUID generated by the frontend on first message and stored in `localStorage` — it persists across refreshes but can be cleared by the user ("New chat").

**Why 10 messages?** Balances context quality vs. token cost. 10 messages ≈ 2,000–4,000 tokens of history, well within all model limits. Enough to maintain "A co z tym drugim?" (anaphoric reference) continuity.

**Why not full history?** Token cost and latency grow linearly. 10 messages covers the practical conversational window.

---

### D5 — SSE streaming: `SseEmitter` (servlet stack, not WebFlux)

**Decision:** Keep Spring MVC. Use `SseEmitter` with a background thread (`ExecutorService`) that calls `ClaudeAdapter.stream()` and emits typed events.

**SSE event protocol:**
```
event: tool_use
data: {"tool":"get_exercise_history","label":"Checking your bench press history…"}

event: token
data: {"text":"Based on your last 8 sessions, "}

event: tool_result
data: {"tool":"get_exercise_history","rowCount":8}

event: done
data: {}

event: error
data: {"message":"API key invalid or quota exceeded"}
```

**Why not WebFlux?** The existing application is servlet-based. Migrating to reactive for a single streaming endpoint is disproportionate. `SseEmitter` with a thread pool works perfectly for low-concurrency personal use.

**Timeout:** `SseEmitter(60_000L)` — 60 seconds max per response. Opus on complex queries can take 20–30s; 60s is safe.

---

### D6 — Prompt storage: classpath resources, not DB

**Decision:** System prompts stored as `src/main/resources/prompts/{task_type}.txt`. Loaded at startup by `PromptLoader` into a `Map<AiTaskType, String>`. Template variables use `{{variable}}` syntax, replaced at render time.

**Why not DB?** Requirements explicitly state prompts are invisible to users. DB storage would require a migration per change and adds an admin UI concern. Classpath means prompts are version-controlled alongside code — diffs, reviews, and rollbacks work naturally with git.

**Context injection:** At runtime, `PromptContextBuilder` assembles a structured data block appended to the system prompt:
```
--- ATHLETE DATA ---
Name: Mateusz
Recent 7-day volume: Chest 3,200 kg | Back 4,100 kg | Legs 5,800 kg
Last session: 2026-04-26 — Push (Bench 100kg × 4 × 6)
```
Java computes all numbers; the prompt receives only formatted results.

---

### D7 — API key encryption: custom `AttributeConverter`, AES-256-GCM

**Decision:** `EncryptedStringConverter implements AttributeConverter<String, String>` using `javax.crypto` (AES-256-GCM, 96-bit IV, 128-bit auth tag). Encryption key loaded from env var `AI_ENCRYPTION_KEY` (base64, 32 bytes). No Jasypt dependency.

**Why GCM over CBC?** GCM provides authenticated encryption — tampering with the ciphertext is detectable. CBC requires a separate HMAC. GCM is standard and supported natively in Java.

**Key stored where:** Environment variable only, never in application properties files or git. At startup, `AiEncryptionConfig` validates the key presence and length; app fails fast if missing.

---

### D8 — Monthly aggregates: nightly `@Scheduled` job, UPSERT pattern

**Decision:** `InsightScheduler` runs at 02:00 daily. The monthly aggregate job computes the previous month's stats if not already present (UPSERT on `UNIQUE(user_id, exercise_name, year_month)`). This is idempotent — safe to re-run.

**Scope:** Aggregate over `exercises` joined to `workout_sessions`. Compute: `MAX(weight_kg)`, `SUM(sets * reps * weight_kg)` as total volume, `COUNT(DISTINCT workout_session_id)`, `SUM(sets)`. Store result in `monthly_exercise_aggregates`. Native SQL UPSERT via `@Modifying` repository method.

---

## Database Schema (V10 migration)

```sql
-- V10__ai_feature.sql

CREATE TABLE ai_settings (
    user_id           UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    provider          VARCHAR(50)  NOT NULL DEFAULT 'anthropic',
    api_key_encrypted TEXT         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE ai_insights (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(50) NOT NULL,   -- DAILY_SUMMARY | WEEKLY_REPORT
    period_start  DATE        NOT NULL,
    period_end    DATE        NOT NULL,
    content       TEXT        NOT NULL,   -- Markdown
    model_used    VARCHAR(100) NOT NULL,
    generated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ai_insights UNIQUE (user_id, type, period_start)
);

CREATE INDEX idx_ai_insights_user_type ON ai_insights(user_id, type, period_start DESC);

CREATE TABLE ai_chat_history (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID        NOT NULL,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_chat_conv ON ai_chat_history(user_id, conversation_id, created_at DESC);

CREATE TABLE monthly_exercise_aggregates (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_name         VARCHAR(255)  NOT NULL,
    exercise_definition_id UUID         REFERENCES exercise_definitions(id),
    year_month            DATE          NOT NULL,  -- first day of month
    max_weight_kg         DECIMAL(8,2),
    total_volume_kg       DECIMAL(12,2),
    session_count         INT           NOT NULL DEFAULT 0,
    total_sets            INT           NOT NULL DEFAULT 0,
    computed_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_monthly_agg UNIQUE (user_id, exercise_name, year_month)
);

CREATE INDEX idx_monthly_agg_lookup ON monthly_exercise_aggregates(user_id, exercise_name, year_month DESC);
```

---

## Package Structure

```
com.deska.evolvelog.ai
├── provider/
│   ├── AiProvider.java              (interface)
│   ├── AiRequest.java               (record)
│   ├── AiResponse.java              (record)
│   ├── AiMessage.java               (record: role, content)
│   ├── AiToolDefinition.java        (record: name, description, inputSchema)
│   ├── AiStreamSink.java            (interface: onToken, onToolUse, onToolResult, onDone, onError)
│   └── anthropic/
│       └── ClaudeAdapter.java       (implements AiProvider — only Anthropic-aware class)
├── router/
│   ├── AiTaskType.java              (enum)
│   └── ModelRouter.java             (@Component)
├── tools/
│   ├── AiTool.java                  (interface)
│   ├── AiToolRegistry.java          (@Component, collects List<AiTool>)
│   └── impl/
│       ├── GetExerciseHistoryTool.java
│       ├── GetPersonalRecordsTool.java
│       ├── GetVolumeStatsTool.java
│       ├── GetMonthlyAggregatesTool.java
│       ├── GetRecentWorkoutsTool.java
│       ├── GetBloodResultsTool.java
│       └── GetMeasurementsTool.java
├── prompt/
│   ├── PromptLoader.java            (loads classpath resources at startup)
│   └── PromptContextBuilder.java    (assembles data context for each task type)
├── service/
│   ├── AiChatService.java           (orchestrates: history → context → stream → persist)
│   ├── AiInsightService.java        (generates and stores insights)
│   └── AiSettingsService.java       (CRUD for api_settings, key validation)
├── scheduled/
│   └── InsightScheduler.java        (@Scheduled: daily + weekly + monthly aggregates)
├── security/
│   └── EncryptedStringConverter.java (AttributeConverter, AES-256-GCM)
├── domain/
│   ├── AiSettings.java              (@Entity)
│   ├── AiInsight.java               (@Entity)
│   ├── AiChatMessage.java           (@Entity)
│   └── MonthlyExerciseAggregate.java (@Entity)
├── repository/
│   ├── AiSettingsRepository.java
│   ├── AiInsightRepository.java
│   ├── AiChatHistoryRepository.java
│   └── MonthlyAggregateRepository.java
└── controller/
    ├── AiChatController.java        (POST /api/ai/chat → SseEmitter)
    ├── AiInsightController.java     (GET /api/ai/insights, POST /api/ai/insights/generate)
    └── AiSettingsController.java    (GET/PUT /api/ai/settings)
```

---

## API Contracts

### Chat (SSE)
```
POST /api/ai/chat
Content-Type: application/json
Accept: text/event-stream

{
  "message": "How has my squat progressed over the last 3 months?",
  "conversationId": "uuid-or-null",
  "pageContext": "progress"    // dashboard | workouts | blood | progress | null
}
```
Response: SSE stream with typed events (see D5).

### Insights
```
GET  /api/ai/insights?type=WEEKLY_REPORT&limit=4
POST /api/ai/insights/generate   { "type": "WEEKLY_REPORT" }   // manual trigger
```

### Settings
```
GET /api/ai/settings         // returns { provider, hasApiKey: true, apiKeyHint: "sk-ant-...****" }
PUT /api/ai/settings         { "provider": "anthropic", "apiKey": "sk-ant-..." }
```
The GET response never returns the actual key — only whether one is configured and the last 4 characters as hint.

---

## Frontend Architecture

### New dependency
```
react-markdown        — render stored Markdown insight content
```
No new SSE library needed — native `EventSource` API handles typed events.

### Components
- `InsightsPage` — fetches `GET /api/ai/insights`, renders `InsightCard` per result, "Refresh" button calls the generate endpoint
- `InsightCard` — renders Markdown, "Ask about this" button sets `chatContext` and opens overlay
- `ChatOverlay` — floating panel, `EventSource` for streaming, `ToolStatusBanner` shown during `tool_use` events, messages rendered with role-based styling
- `QuickActions` — per-page contextual prompts rendered above chat input (e.g. on `/progress` page: "How was my volume this week?", "Am I overtraining chest?")
- `AiSettingsSection` — inside existing Settings page, single API key field with masked display

### Page context injection
Frontend sends `pageContext` string with each message. Backend `PromptContextBuilder` maps it to a pre-fetched data snapshot injected into the system prompt. The mapping:

| pageContext | Data injected |
|---|---|
| `dashboard` | Last 7 days: session count, volume by muscle, any PRs |
| `workouts` | Last 5 workout sessions with exercises |
| `progress` | Last 4 weeks volume + progressive overload for top 3 exercises |
| `blood` | Most recent blood test report results |
| `null` | User profile only (name, goals if set) |

---

## Risks / Trade-offs

**[Risk] Long Opus response delays (20–30s) may feel frozen** → Mitigation: SSE `tool_use` events give immediate feedback. Add a "thinking" animation that starts on request and clears on first `token` event. Set aggressive timeout (60s) with friendly error message.

**[Risk] Monthly aggregate job fails silently overnight** → Mitigation: Wrap in try-catch, log to application logs with `ERROR` level, expose last-computed-at timestamp on `GET /api/ai/insights` so stale data is visible.

**[Risk] API key encrypted with lost encryption key renders settings unusable** → Mitigation: Document clearly that `AI_ENCRYPTION_KEY` must be backed up. If key is lost, user re-enters their Claude API key (no data loss, just reconfiguration).

**[Risk] Tool call loop — model keeps calling tools without generating text** → Mitigation: Hard limit of 5 tool calls per request in `AiChatService`. After limit, inject system message "Please answer with the data you have gathered."

**[Risk] Insight generation fails when user has no API key configured** → Mitigation: `InsightScheduler` checks for presence of `ai_settings` before attempting. Log skip with `INFO` level, do not throw.

**[Trade-off] In-process SSE with blocking `HttpClient`** → For personal use (1 concurrent user), a dedicated thread per SSE response is acceptable. If this ever needed to scale, migration to WebFlux + `WebClient` reactive streaming would be the path.

---

## Migration Plan

1. Add Flyway `V10__ai_feature.sql` — new tables only, no existing table changes
2. Add `AI_ENCRYPTION_KEY` env var to local `.env` / deployment config
3. Deploy backend — new endpoints return 404 until frontend is deployed (no breaking changes to existing endpoints)
4. Deploy frontend — Insights page visible but empty until user saves API key
5. User navigates to Settings → enters Claude API key → feature activates
6. First insights generated on next scheduled run (or via manual trigger button)

**Rollback:** Drop V10 tables, remove env var, redeploy previous frontend build. No existing functionality affected.

---

## Open Questions

- Should `GET /api/ai/insights` support pagination or always return the latest N per type?
- Should the Insights page auto-trigger generation if no insights exist yet (vs. showing an empty state with manual trigger)?
- What's the desired retention for `ai_chat_history`? Soft-delete old conversations or keep forever?
- Should `pageContext` for the blood test page pass the specific report ID, or always the most recent report?
