# Design: gemini-provider

## Context

The AI subsystem today is structured around a single `AiProvider` bean (`ClaudeAdapter`). While `AiProvider` is an interface, `AiChatService` leaks the concrete implementation by directly calling `ClaudeRequestContext.setApiKey()` — a Claude-specific class. `ModelRouter` holds one `AiProvider` and returns Claude model ID strings from `selectModel()` / `selectModelForChat()`.

To add Gemini cleanly, both of these coupling points must be fixed before the adapter itself is added. The changes are:

1. Move API key lifecycle into the `AiProvider` contract.
2. Refactor `ModelRouter` to a provider registry that resolves the right adapter per user.
3. Introduce model tier abstraction so `ModelRouter` doesn't return provider-specific model ID strings.
4. Implement `GeminiAdapter` against the now-stable contract.

---

## Goals / Non-Goals

**Goals:**
- `AiChatService` contains zero provider-specific imports after this change.
- Switching between Claude and Gemini in Settings takes effect immediately on the next request — no restart, no history loss.
- `ai_chat_history.provider` records which provider answered each message (analytics).
- `GeminiAdapter` supports streaming, tool-calling, and the same `AiProvider` contract as Claude.

**Non-Goals:**
- Simultaneous multi-provider per user (e.g., Claude for chat + Gemini for insights) — single active provider per user only.
- Migrating existing `ai_chat_history` rows to backfill provider (they will remain NULL).
- Automatic provider failover (if Gemini is down, no fallback to Claude).
- Gemini native grounding or Google Search tool — only tools defined in `AiToolRegistry`.

---

## Decisions

### D1 — API key lifecycle moves into `AiProvider`

**Problem:** `AiChatService` currently calls `ClaudeRequestContext.setApiKey(apiKey)` directly — Claude-specific code in a supposedly provider-agnostic service.

**Decision:** Extend the `AiProvider` interface with two methods:
```java
void prepareContext(String apiKey);   // set thread-local key before call
void clearContext();                   // clean up in finally block
```
`ClaudeAdapter.prepareContext()` delegates to `ClaudeRequestContext.setApiKey()`.
`GeminiAdapter.prepareContext()` delegates to `GeminiRequestContext.setApiKey()`.
`AiChatService` calls `provider.prepareContext(apiKey)` — no concrete class referenced.

**Alternative considered:** Add `apiKey` field to `AiRequest`. Rejected — `AiRequest` is a pure request descriptor and mixing credentials into it couples the provider contract to secrets management.

---

### D2 — `ModelRouter` becomes a provider registry

**Problem:** `ModelRouter` holds a single `AiProvider` and cannot route to a different adapter per user.

**Decision:** `ModelRouter` is injected with `List<AiProvider>` (Spring auto-collects all beans implementing the interface) and builds a `Map<String, AiProvider>` keyed by `providerId()`. A new method is added:
```java
public AiProvider resolveProvider(String providerId)
```
`AiChatService` resolves the provider at the start of each chat request by reading `aiSettings.getProvider()`.

The LLM classifier (`classifyByLlm`) inside `ModelRouter` also uses `resolveProvider()` — it passes the user's current provider so the classify call goes to the same provider the user has configured.

---

### D3 — Model tier abstraction

**Problem:** `selectModel(AiTaskType)` and `selectModelForChat()` return Claude model ID strings (`"claude-haiku-4-5-20251001"` etc.), which is meaningless to `GeminiAdapter`.

**Decision:** Introduce `ModelTier { FAST, BALANCED, SMART }` enum. `ModelRouter` exposes:
```java
public ModelTier selectTier(AiTaskType taskType)
public ModelTier selectTierForChat(String message, String providerId)
```
Each `AiProvider` implementation is responsible for mapping a `ModelTier` to its concrete model ID:
```java
String modelIdForTier(ModelTier tier);  // added to AiProvider interface
```
`AiModelConstants` is split: Claude constants move to `anthropic/ClaudeModelConstants`, Gemini constants to `gemini/GeminiModelConstants`. The shared `AiModelConstants` class is removed.

Tier → model mapping:

| Tier | Claude | Gemini |
|------|--------|--------|
| FAST | `claude-haiku-4-5-20251001` | `gemini-2.0-flash` |
| BALANCED | `claude-sonnet-4-6` | `gemini-2.5-pro` |
| SMART | `claude-opus-4-6` | `gemini-2.5-pro` (Gemini has no separate "ultra" in AI Studio) |

---

### D4 — Gemini API format mapping

The Gemini REST API (`v1beta`) differs from Anthropic in four areas:

**Authentication:** API key passed as query param `?key={apiKey}` on every request. No `x-api-key` header. `GeminiRequestContext` stores the key thread-locally; `GeminiAdapter` appends it to the URL.

**Message format:**
```json
{
  "systemInstruction": { "parts": [{ "text": "..." }] },
  "contents": [
    { "role": "user",  "parts": [{ "text": "..." }] },
    { "role": "model", "parts": [{ "text": "..." }] }
  ]
}
```
Role `"assistant"` from `AiMessage` is remapped to `"model"` on the way out.

**Tool format:**
```json
{
  "tools": [{
    "functionDeclarations": [{
      "name": "get_recent_workouts",
      "description": "...",
      "parameters": { "type": "OBJECT", "properties": {...} }
    }]
  }]
}
```
`AiToolDefinition.inputSchema()` (JSON Schema) maps directly to `parameters` with minor normalisation (type values uppercased).

**SSE streaming:** Gemini sends `data:` lines with full JSON candidates:
```
data: {"candidates":[{"content":{"parts":[{"text":"Hello"}],"role":"model"}}]}
data: {"candidates":[{"content":{"parts":[{"functionCall":{"name":"get_recent_workouts","args":{}}}]}}]}
```
`GeminiAdapter` parses each `data:` line, extracts text tokens (→ `sink.onToken`) or function calls (→ `sink.onToolUse`).

**Tool results:** Returned in a `"user"` role turn with `functionResponse`:
```json
{ "role": "user", "parts": [{ "functionResponse": { "name": "...", "response": { "output": "..." } } }] }
```
This maps to `AiMessage.user("Tool result for X:\n" + result)` in the generic `AiChatService` — no adapter-level change needed because `AiChatService` re-sends tool results as plain user messages.

---

### D5 — `provider` column in `ai_chat_history`

**Decision:** Add `VARCHAR(50) NULL` column `provider` to `ai_chat_history` via V26 migration. `AiChatService` writes the resolved provider ID (`"anthropic"` / `"google"`) when persisting the assistant message. Existing rows remain NULL (no backfill). The column is purely for analytics — no query in the application reads it.

---

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Gemini tool-calling format mismatch at runtime (JSON Schema uppercase types) | Cover with a unit test that serialises an `AiToolDefinition` through `GeminiAdapter` and asserts the output shape before any live call |
| Gemini SSE format changes between API versions | Pin to `v1beta`; log raw SSE lines at DEBUG level so format breaks surface quickly |
| `classifyByLlm` calls the wrong provider if `resolveProvider` fails | `resolveProvider` throws `IllegalStateException` on unknown provider ID — caught by `ModelRouter.classifyByLlm` fallback, defaults to `SIMPLE` |
| `ModelTier.SMART` maps to same model as `BALANCED` for Gemini | Documented in `GeminiModelConstants`; acceptable until Gemini Ultra becomes available in AI Studio |
| Thread-local leak if `clearContext()` not called | Existing `try/finally` pattern in `AiChatService` is preserved; `clearContext()` is called in the `finally` block unconditionally |

---

## Migration Plan

1. V26 migration: `ALTER TABLE ai_chat_history ADD COLUMN provider VARCHAR(50)`.
2. Refactor `AiProvider` interface (add `prepareContext`, `clearContext`, `modelIdForTier`).
3. Update `ClaudeAdapter` to implement new methods; add `ClaudeModelConstants`.
4. Add `ModelTier` enum; refactor `ModelRouter` to registry + tier-based selection.
5. Update `AiChatService`: remove `ClaudeRequestContext` import, use registry pattern.
6. Implement `GeminiAdapter` + `GeminiRequestContext` + `GeminiModelConstants`.
7. Update `AiSettingsService` / DTOs to expose `provider`.
8. Write tests (unit: `GeminiAdapter` serialisation + SSE parsing; integration: provider switch round-trip).
9. Update `ai_settings` seed/docs; update frontend `AiSettingsPage`.

Rollback: deploying without V26 migration is safe (column is additive). Reverting the adapter removes Gemini from the registry; `resolveProvider("google")` throws, which surfaces as a user-facing error on next chat.

---

## Open Questions

- **Gemini model for classifier:** `gemini-2.0-flash` is cheap and fast, equivalent to Haiku. Confirm this is the intended `FAST` tier model before release.
- **Key validation on save:** Should the validation test-call use a minimal prompt to a cheap model, or just a `GET /models` list call? List call is cheaper but doesn't verify the key works for generation.
- **Frontend provider selector default:** When a user has no provider set in `ai_settings`, the UI should default to `"anthropic"` — confirm this matches the existing DB default.
