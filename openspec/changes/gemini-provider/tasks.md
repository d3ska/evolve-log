# Tasks: gemini-provider

## 1. Database Migration

- [x] 1.1 Create `V26__ai_chat_history_provider_column.sql` — `ALTER TABLE ai_chat_history ADD COLUMN provider VARCHAR(50)`

## 2. AiProvider Interface Extension

- [x] 2.1 Add `ModelTier` enum (`FAST`, `BALANCED`, `SMART`) to `ai.provider` package
- [x] 2.2 Add `void prepareContext(String apiKey)`, `void clearContext()`, and `String modelIdForTier(ModelTier tier)` to the `AiProvider` interface
- [x] 2.3 Implement new methods in `ClaudeAdapter`: `prepareContext` delegates to `ClaudeRequestContext.setApiKey`, `clearContext` delegates to `ClaudeRequestContext.clear`, `modelIdForTier` maps to `ClaudeModelConstants`
- [x] 2.4 Create `ClaudeModelConstants` in `ai.provider.anthropic` with `FAST`, `BALANCED`, `SMART` string constants (values: `claude-haiku-4-5-20251001`, `claude-sonnet-4-6`, `claude-opus-4-6`)
- [x] 2.5 Delete `AiModelConstants` from `ai.router` — fix all call sites to use `ClaudeModelConstants` or `ModelTier`

## 3. ModelRouter Refactor

- [x] 3.1 Change `ModelRouter` constructor to accept `List<AiProvider>` and build `Map<String, AiProvider>` keyed by `providerId()`
- [x] 3.2 Add `resolveProvider(String providerId)` method — throws `IllegalStateException` on unknown ID
- [x] 3.3 Replace `selectModel(AiTaskType)` with `selectTier(AiTaskType)` returning `ModelTier`
- [x] 3.4 Replace `selectModelForChat(String)` with `selectTierForChat(String message, String providerId)` returning `ModelTier` — LLM classifier uses `resolveProvider(providerId)` internally

## 4. AiChatService Decoupling

- [x] 4.1 Remove `AiProvider aiProvider` field and `ClaudeRequestContext` import from `AiChatService`
- [x] 4.2 Resolve provider per request: `AiProvider provider = modelRouter.resolveProvider(aiSettingsService.getProvider(userId))`
- [x] 4.3 Replace `ClaudeRequestContext.setApiKey(apiKey)` with `provider.prepareContext(apiKey)` and `ClaudeRequestContext.clear()` with `provider.clearContext()` in try/finally
- [x] 4.4 Replace `aiProvider.stream(request, collecting)` with `provider.stream(request, collecting)` — resolve model ID via `provider.modelIdForTier(modelRouter.selectTierForChat(...))`
- [x] 4.5 Persist `provider.providerId()` into `AiChatMessage.provider` column when saving assistant message

## 5. GeminiRequestContext

- [x] 5.1 Create `GeminiRequestContext` in `ai.provider.gemini` — thread-local with `setApiKey`, `getApiKey`, `clear` (mirrors `ClaudeRequestContext`)

## 6. GeminiModelConstants

- [x] 6.1 Create `GeminiModelConstants` in `ai.provider.gemini` with `FLASH = "gemini-2.0-flash"` and `PRO = "gemini-2.5-pro"`

## 7. GeminiAdapter

- [x] 7.1 Create `GeminiAdapter implements AiProvider` in `ai.provider.gemini`, annotated `@Component`
- [x] 7.2 Implement `providerId()` returning `"google"`
- [x] 7.3 Implement `prepareContext` / `clearContext` delegating to `GeminiRequestContext`
- [x] 7.4 Implement `modelIdForTier`: `FAST` → `FLASH`, `BALANCED` / `SMART` → `PRO`
- [x] 7.5 Implement request serialisation: `systemInstruction`, `contents[]` with role mapping (`"assistant"` → `"model"`), `tools[0].functionDeclarations[]` with uppercased type values
- [x] 7.6 Implement `stream()`: POST to `…/models/{model}:streamGenerateContent?alt=sse&key={apiKey}`, parse `data:` lines, emit `onToken` / `onToolUse` / `onDone` / `onError`
- [x] 7.7 Implement `complete()` via non-streaming endpoint (`generateContent`) — used by LLM classifier
- [x] 7.8 Guard: throw `IllegalStateException` in `stream()` and `complete()` when `GeminiRequestContext.getApiKey()` is null

## 8. AiSettings DTO Updates

- [x] 8.1 Add `provider` field to `AiSettingsRequest` with validation: must be `"anthropic"` or `"google"`, non-blank
- [x] 8.2 Add `provider` field to `AiSettingsResponse`
- [x] 8.3 Update `AiSettingsService.saveSettings()` to persist `provider` and validate key prefix (`sk-ant-` required for `anthropic`)
- [x] 8.4 Update `AiSettingsService.getProvider(userId)` to return the stored provider string (add method if absent)
- [x] 8.5 Update `AiChatMessage` entity to include `provider` field mapped to the new column

## 9. Tests

- [x] 9.1 `GeminiAdapterTest` — unit test: request serialisation produces correct JSON shape (systemInstruction, role mapping, functionDeclarations, uppercased types)
- [x] 9.2 `GeminiAdapterTest` — SSE parsing: given raw `data:` lines with text and functionCall parts, verify `onToken` and `onToolUse` fire correctly
- [x] 9.3 `GeminiAdapterTest` — missing key: `stream()` throws `IllegalStateException` before HTTP call
- [x] 9.4 `ModelRouterTest` — `resolveProvider("anthropic")` returns `ClaudeAdapter`; `resolveProvider("unknown")` throws
- [x] 9.5 `ModelRouterTest` — `selectTier` returns correct `ModelTier` for each `AiTaskType`
- [x] 9.6 `ModelRouterTest` — `selectTierForChat` heuristic paths (FAST / BALANCED / SMART) unchanged
- [x] 9.7 `AiChatServiceTest` — no `ClaudeRequestContext` reference; verify `prepareContext` / `clearContext` called on resolved provider
- [x] 9.8 `AiSettingsServiceTest` — PUT with `provider = "anthropic"` and missing `sk-ant-` prefix returns 400; `provider = "google"` accepts any non-blank key

## 10. CLAUDE.md Update

- [x] 10.1 Update V-migration table in `CLAUDE.md` with V26 entry
- [x] 10.2 Update AI Trainer domain row to list `GeminiAdapter` alongside `ClaudeAdapter`
- [x] 10.3 Remove any reference to `AiModelConstants` from documentation
