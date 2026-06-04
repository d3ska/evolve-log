## Why

EvolveLog's AI trainer currently supports only Anthropic's Claude family. Adding Google Gemini as an alternative provider gives users a second option for their AI key — useful for those who already have Gemini API access, want cost diversity, or prefer Google's models. The infrastructure already has an `AiProvider` interface and per-user `provider` / `encrypted_api_key` columns in `ai_settings`, so multi-provider support is architecturally ready.

## What Changes

- Fix `AiChatService` to be fully provider-agnostic: remove the `ClaudeRequestContext` leak, resolve the active provider through `ModelRouter` instead of injecting a single `AiProvider`
- Add `GeminiAdapter` implementing `AiProvider` — handles Gemini REST API, SSE streaming, and tool-calling format
- Add `GeminiRequestContext` thread-local (mirrors `ClaudeRequestContext`) for per-request API key injection; each adapter owns its own context
- Extend `AiModelConstants` with Gemini model IDs (`gemini-2.0-flash`, `gemini-2.5-pro`)
- Refactor `ModelRouter` to hold a `Map<String, AiProvider>` registry keyed by `providerId()`, routing by the user's chosen provider
- Expose `provider` field in `AiSettingsResponse` and `AiSettingsRequest` DTOs
- Add Gemini API key validation on save (test-call `gemini-2.0-flash`)
- Add `provider` column to `ai_chat_history` for analytics — records which provider answered each message; nullable, no impact on chat logic
- Update frontend `AiSettingsPage` to let users select provider and enter the matching key

## Capabilities

### New Capabilities

- `gemini-provider`: Gemini API adapter — request/response mapping, SSE streaming, function-calling format, model constants, request context
- `multi-provider-router`: Provider registry in `ModelRouter` — route to the active provider per user, provider-aware model selection, full abstraction in `AiChatService`

### Modified Capabilities

- `ai-personal-trainer`: `AiChatService` loses its Claude-specific coupling; provider resolution moves into `ModelRouter`; `ai_chat_history` gains `provider` column

## Impact

- **New files**: `GeminiAdapter.java`, `GeminiRequestContext.java`
- **Modified files**: `ModelRouter.java`, `AiChatService.java`, `AiSettingsService.java`, `AiSettingsRequest.java`, `AiSettingsResponse.java`, `AiModelConstants.java`, `AiChatMessage.java`
- **DB migration V26**: `ALTER TABLE ai_chat_history ADD COLUMN provider VARCHAR(50)` (nullable)
- **External dependency**: `https://generativelanguage.googleapis.com/v1beta/` (Google AI Studio API) — no new library, uses existing `java.net.http.HttpClient`
- **Frontend**: `AiSettingsPage` gains a provider selector and key field
- **Chat history stays provider-agnostic**: stored messages are `role` + `content` only — switching providers mid-conversation is seamless; `provider` column is analytics-only
