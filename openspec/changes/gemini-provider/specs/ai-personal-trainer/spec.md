## MODIFIED Requirements

### Requirement: Provider-agnostic AI interface
The system SHALL expose an `AiProvider` interface that abstracts all LLM provider details. Business logic throughout the application SHALL depend only on this interface, never on provider-specific types. The interface SHALL declare five methods: `complete(AiRequest)`, `stream(AiRequest, AiStreamSink)`, `providerId()`, `prepareContext(String apiKey)`, `clearContext()`, and `modelIdForTier(ModelTier tier)`.

#### Scenario: Blocking completion returns response
- **WHEN** a caller invokes `AiProvider.complete(AiRequest)` with a valid request
- **THEN** the system returns a fully populated `AiResponse` containing the assistant message text and token usage metadata

#### Scenario: Streaming response delivers tokens
- **WHEN** a caller invokes `AiProvider.stream(AiRequest, AiStreamSink)` with a valid request
- **THEN** the provider calls `AiStreamSink.onToken()` for each incremental text chunk, `AiStreamSink.onToolUse()` for each tool the model requests, and `AiStreamSink.onDone()` when complete

#### Scenario: Stream error propagated via callback
- **WHEN** the provider encounters a network or API error during streaming
- **THEN** the provider calls `AiStreamSink.onError(Throwable)` and does NOT call `onDone()`

#### Scenario: prepareContext stores key for current thread
- **WHEN** `provider.prepareContext("my-key")` is called on any `AiProvider` implementation
- **THEN** subsequent calls to that adapter's internal context holder return `"my-key"` on the same thread

#### Scenario: clearContext removes key from thread
- **WHEN** `provider.clearContext()` is called
- **THEN** the adapter's internal context holder returns null for the current thread

---

### Requirement: Deterministic task-based model selection
The system SHALL select AI model tiers for scheduled background tasks using a deterministic switch expression on `AiTaskType` with no external calls. `ModelRouter.selectTier(AiTaskType)` SHALL return `ModelTier`: `DAILY_SUMMARY` and `POST_WORKOUT` → `FAST`; `WEEKLY_REPORT` → `BALANCED`; `BLOOD_ANALYSIS` → `SMART`. Callers resolve the concrete model ID by calling `provider.modelIdForTier(tier)`.

#### Scenario: Daily summary uses FAST tier
- **WHEN** `ModelRouter.selectTier(AiTaskType.DAILY_SUMMARY)` is called
- **THEN** it returns `ModelTier.FAST` with no external API calls

#### Scenario: Weekly report uses BALANCED tier
- **WHEN** `ModelRouter.selectTier(AiTaskType.WEEKLY_REPORT)` is called
- **THEN** it returns `ModelTier.BALANCED` with no external API calls

#### Scenario: Blood analysis uses SMART tier
- **WHEN** `ModelRouter.selectTier(AiTaskType.BLOOD_ANALYSIS)` is called
- **THEN** it returns `ModelTier.SMART` with no external API calls

---

### Requirement: Heuristic-first chat model routing
The system SHALL classify incoming chat messages using lightweight heuristics before considering an LLM classifier call. Messages under 80 characters with no analytical keywords SHALL be classified `FAST` and routed to the active provider's FAST model immediately without an external API call.

#### Scenario: Short factual message routed without classifier
- **WHEN** a chat message contains fewer than 80 characters and no analytical keywords
- **THEN** the router classifies it `FAST`, selects the FAST model for the user's active provider, and makes zero additional API calls

#### Scenario: Analytical message routed to BALANCED
- **WHEN** a chat message contains analytical keywords such as "trend", "progress", "comparison", "last 4 weeks", or "volume"
- **THEN** the router classifies it `ANALYTICAL` and returns `ModelTier.BALANCED`

#### Scenario: Medical query routed to SMART
- **WHEN** a chat message contains medical keywords such as "blood", "HRV", "fatigue", "health audit", or "marker"
- **THEN** the router classifies it `MEDICAL` and returns `ModelTier.SMART`

---

### Requirement: Model IDs are provider-scoped constants
The system SHALL define model ID strings as named constants in per-provider constant classes: `ClaudeModelConstants` in `ai.provider.anthropic` and `GeminiModelConstants` in `ai.provider.gemini`. The shared `AiModelConstants` class SHALL be removed. No controller, service, or scheduler SHALL hard-code a model ID string inline.

#### Scenario: Claude model constant referenced by name
- **WHEN** any class returns a Claude model ID
- **THEN** the value originates from `ClaudeModelConstants`, not a literal string

#### Scenario: AiModelConstants class absent
- **WHEN** the codebase is compiled
- **THEN** no class named `AiModelConstants` exists in any package

---

### Requirement: Settings PUT endpoint validates provider-specific key format
The system SHALL expose `PUT /api/ai/settings` accepting `{ "provider": "anthropic"|"google", "apiKey": "..." }`. The endpoint SHALL validate that `provider` is one of the supported values, that `apiKey` is non-blank, and that the key matches the expected prefix for the specified provider (`sk-ant-` for `anthropic`; no prefix restriction for `google`). The endpoint SHALL be authenticated.

#### Scenario: Anthropic key with correct prefix accepted
- **WHEN** an authenticated user calls `PUT /api/ai/settings` with `provider = "anthropic"` and `apiKey = "sk-ant-abc123"`
- **THEN** the key is stored and the response returns HTTP 200

#### Scenario: Unknown provider rejected
- **WHEN** an authenticated user calls `PUT /api/ai/settings` with `provider = "openai"`
- **THEN** the response returns HTTP 400 with a validation error

#### Scenario: Blank key rejected
- **WHEN** an authenticated user calls `PUT /api/ai/settings` with an empty or whitespace-only `apiKey`
- **THEN** the response returns HTTP 400 with a validation error message

## ADDED Requirements

### Requirement: provider column recorded in chat history
The system SHALL write the resolved provider ID (e.g., `"anthropic"` or `"google"`) to the `ai_chat_history.provider` column when persisting the assistant message after a successful chat response. Rows saved before this migration SHALL retain `NULL` in the `provider` column. The column SHALL NOT be used in any chat query or logic — it is analytics-only.

#### Scenario: Provider recorded for new messages
- **WHEN** a user with `provider = "google"` sends a chat message and the AI responds successfully
- **THEN** the persisted `ai_chat_history` row for the assistant message has `provider = "google"`

#### Scenario: Null provider on old rows does not cause errors
- **WHEN** `AiChatService` loads history rows that have `provider = NULL`
- **THEN** the rows are included normally in the conversation context with no errors
