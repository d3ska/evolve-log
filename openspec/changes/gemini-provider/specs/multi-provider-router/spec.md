## ADDED Requirements

### Requirement: ModelTier abstraction
The system SHALL define a `ModelTier` enum with values `FAST`, `BALANCED`, and `SMART`. All routing decisions SHALL produce a `ModelTier`, not a provider-specific model ID string. Each `AiProvider` implementation SHALL implement `modelIdForTier(ModelTier tier)` returning its concrete model ID for that tier.

#### Scenario: ClaudeAdapter maps FAST tier
- **WHEN** `ClaudeAdapter.modelIdForTier(ModelTier.FAST)` is called
- **THEN** it returns `"claude-haiku-4-5-20251001"`

#### Scenario: GeminiAdapter maps FAST tier
- **WHEN** `GeminiAdapter.modelIdForTier(ModelTier.FAST)` is called
- **THEN** it returns `"gemini-2.0-flash"`

#### Scenario: GeminiAdapter maps SMART tier
- **WHEN** `GeminiAdapter.modelIdForTier(ModelTier.SMART)` is called
- **THEN** it returns `"gemini-2.5-pro"` (same as BALANCED — no higher Gemini tier available)

---

### Requirement: Provider registry in ModelRouter
The system SHALL inject `List<AiProvider>` into `ModelRouter` at construction. The router SHALL build a `Map<String, AiProvider>` keyed by `AiProvider.providerId()`. `ModelRouter.resolveProvider(String providerId)` SHALL return the matching adapter or throw `IllegalStateException` when the provider ID is unknown.

#### Scenario: Known provider resolves correctly
- **WHEN** `ModelRouter.resolveProvider("anthropic")` is called and `ClaudeAdapter` is registered
- **THEN** it returns the `ClaudeAdapter` instance

#### Scenario: Unknown provider throws
- **WHEN** `ModelRouter.resolveProvider("unknown")` is called
- **THEN** it throws `IllegalStateException` with a message identifying the unknown provider ID

#### Scenario: Both providers registered when both beans present
- **WHEN** the Spring context starts with `ClaudeAdapter` and `GeminiAdapter` both declared as `@Component`
- **THEN** `ModelRouter` registry contains entries for both `"anthropic"` and `"google"`

---

### Requirement: Tier-based task model selection
The system SHALL expose `ModelRouter.selectTier(AiTaskType taskType)` returning a `ModelTier`. The mapping SHALL be: `DAILY_SUMMARY` and `POST_WORKOUT` → `FAST`; `WEEKLY_REPORT` → `BALANCED`; `BLOOD_ANALYSIS` → `SMART`; `CHAT_CLASSIFY` → `FAST`. Callers resolve the concrete model ID via `provider.modelIdForTier(tier)`.

#### Scenario: DAILY_SUMMARY maps to FAST
- **WHEN** `ModelRouter.selectTier(AiTaskType.DAILY_SUMMARY)` is called
- **THEN** it returns `ModelTier.FAST` with no external API calls

#### Scenario: BLOOD_ANALYSIS maps to SMART
- **WHEN** `ModelRouter.selectTier(AiTaskType.BLOOD_ANALYSIS)` is called
- **THEN** it returns `ModelTier.SMART` with no external API calls

---

### Requirement: Tier-based chat routing
The system SHALL expose `ModelRouter.selectTierForChat(String message, String providerId)` returning a `ModelTier`. Heuristic classification rules are preserved unchanged. The LLM classifier fallback SHALL use `resolveProvider(providerId)` so the classify call goes to the user's active provider.

#### Scenario: Short message classified FAST without API call
- **WHEN** `selectTierForChat("What was my last squat?", "anthropic")` is called
- **THEN** the router returns `ModelTier.FAST` immediately, making zero external API calls

#### Scenario: Classifier uses active provider
- **WHEN** a user has `provider = "google"` and sends an ambiguous message
- **THEN** the LLM classifier call is routed through `GeminiAdapter`, not `ClaudeAdapter`

---

### Requirement: AiChatService is provider-agnostic
The system SHALL have zero Claude-specific or Gemini-specific imports in `AiChatService`. Provider resolution SHALL use `ModelRouter.resolveProvider(aiSettings.getProvider())`. API key lifecycle SHALL use `provider.prepareContext(apiKey)` and `provider.clearContext()` in a try/finally block.

#### Scenario: No ClaudeRequestContext in AiChatService
- **WHEN** `AiChatService.java` is inspected
- **THEN** it contains no import of `ClaudeRequestContext` or any class from `ai.provider.anthropic`

#### Scenario: Provider resolved per request
- **WHEN** a user with `provider = "google"` sends a chat message
- **THEN** `ModelRouter.resolveProvider("google")` is called and the resulting `GeminiAdapter` is used for the entire request

#### Scenario: Context cleared on error
- **WHEN** `GeminiAdapter.stream()` throws an exception during a chat request
- **THEN** `provider.clearContext()` is still called before the error propagates to the SSE controller
