## Why

EvolveLog has rich structured training data but no intelligence layer to turn it into actionable insight. Adding an AI personal trainer gives the user personalized analysis, weekly reports, and conversational access to their own data — without requiring any AI knowledge from the user.

## What Changes

- Introduce an **AI provider abstraction layer** (interface + adapters) so the app can work with Claude, Gemini, ChatGPT or any future LLM without changing business logic
- Add **programmatic model routing** — the app automatically selects the cheapest appropriate model per task (Haiku for simple summaries, Sonnet for analysis, Opus for medical/complex)
- Add **LLM intent classification** for chat — Haiku classifies user intent, routes to the right model
- Add **tool calling framework** — structured backend tools the AI can invoke to fetch specific data (workout history, PRs, volume stats, etc.) rather than receiving a full data dump
- Add **scheduled insight generation** — nightly `@Scheduled` jobs produce daily and weekly AI-generated reports stored in `ai_insights`
- Add **pre-computed monthly aggregates** — PostgreSQL table for fast multi-year trend queries, maintained nightly
- Add **streaming chat endpoint** — SSE-based endpoint proxying the AI response token by token
- Add **Insights dashboard** — dedicated frontend page showing pre-generated report cards with "Ask about this" deeplink into chat
- Add **context-aware floating chat** — chat overlay that detects current page and injects relevant context automatically
- Add **encrypted API key storage** — user stores their Claude (or other provider) API key once in settings; stored AES-256 encrypted
- System prompts and model selection are **invisible to the user** — managed in backend configuration only

## Capabilities

### New Capabilities

- `ai-provider`: Abstract `AiProvider` interface with request/response model, streaming support, and tool calling contract. Concrete `ClaudeAdapter` as first implementation. Extensible for Gemini, OpenAI adapters.
- `model-router`: `ModelRouter` service — task-type-based routing for scheduled jobs, heuristic + LLM-classifier routing for chat messages. No user configuration required.
- `ai-tools`: Tool calling framework — defines `AiTool` interface and registers backend tools the AI can invoke: `get_workout_history`, `get_volume_stats`, `get_personal_records`, `get_exercise_history`, `get_monthly_aggregates`.
- `ai-insights`: Scheduled insight generation — `@Scheduled` jobs for daily summary (Haiku) and weekly report (Sonnet). Results stored in `ai_insights` table with Markdown content and metadata.
- `monthly-aggregates`: Pre-computed `monthly_exercise_aggregates` table — max weight, total volume, session count per exercise per month, maintained nightly. Used by AI tools for fast historical queries.
- `ai-chat`: Streaming chat endpoint (`POST /api/ai/chat`, SSE) with tool calling, intent classification, DB-persisted conversation history (`ai_chat_history` table — last 10 messages per `conversation_id`), and page-context injection. SSE protocol emits typed events: `token` (streamed text), `tool_use` (tool invocation with name, e.g. "Checking your bench press PRs…"), `tool_result`, `done`, `error` — so the frontend can show live tool status and never appear frozen during tool execution.
- `ai-settings`: User AI settings — encrypted API key storage (`AES-256`), provider selection (default: Anthropic). Settings API (`GET/PUT /api/ai/settings`). No system prompt exposure.
- `insights-ui`: Frontend Insights page — report cards rendered from `ai_insights` Markdown content, "Ask about this" button linking into chat. Polling for new insights.
- `chat-ui`: Floating chat button (all pages), streaming token-by-token rendering, tool status indicators ("Fetching your deadlift history…"), quick action buttons per page context.

### Modified Capabilities

- `user-settings`: Add AI settings section (API key input only — no prompt editing, no model selection). Existing settings UI extended.

## Impact

**Backend (Spring Boot):**
- New packages: `ai.provider`, `ai.router`, `ai.tools`, `ai.service`, `ai.scheduled`
- New DB tables: `ai_settings`, `ai_insights`, `monthly_exercise_aggregates`, `ai_chat_history`
- New dependency: Anthropic Java SDK (or direct HTTP client for provider abstraction)
- Existing `ExerciseRepository`, `WorkoutRepository` used by AI tools — no schema changes

**Frontend (React/Vite):**
- New pages: `InsightsPage`
- New components: `ChatOverlay`, `InsightCard`, `ToolStatusIndicator`, `QuickActions`
- New API client methods for chat (SSE streaming) and insights

**Security:**
- API key encrypted at rest (AES-256 via `AttributeConverter`)
- System prompts never exposed via any API endpoint
- Claude API calls made server-side only — key never reaches frontend
