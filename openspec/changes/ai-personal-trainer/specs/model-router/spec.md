## ADDED Requirements

### Requirement: Deterministic task-based model selection
The system SHALL select AI models for scheduled background tasks using a deterministic switch expression on `AiTaskType` with no external calls. The mapping SHALL be: `DAILY_SUMMARY` and `POST_WORKOUT` → `claude-haiku-4-5-20251001`; `WEEKLY_REPORT` → `claude-sonnet-4-6`; `BLOOD_ANALYSIS` → `claude-opus-4-6`.

#### Scenario: Daily summary uses Haiku
- **WHEN** `ModelRouter.selectModel(AiTaskType.DAILY_SUMMARY)` is called
- **THEN** it returns `"claude-haiku-4-5-20251001"` with no external API calls

#### Scenario: Weekly report uses Sonnet
- **WHEN** `ModelRouter.selectModel(AiTaskType.WEEKLY_REPORT)` is called
- **THEN** it returns `"claude-sonnet-4-6"` with no external API calls

#### Scenario: Blood analysis uses Opus
- **WHEN** `ModelRouter.selectModel(AiTaskType.BLOOD_ANALYSIS)` is called
- **THEN** it returns `"claude-opus-4-6"` with no external API calls

### Requirement: Heuristic-first chat model routing
The system SHALL classify incoming chat messages using lightweight heuristics before considering an LLM classifier call. Messages under 80 characters with no analytical keywords SHALL be classified `SIMPLE` and routed to Haiku immediately without an external API call.

#### Scenario: Short factual message routed without classifier
- **WHEN** a chat message contains fewer than 80 characters and no analytical keywords (e.g., "trend", "progress", "compare", "volume", "blood", "HRV")
- **THEN** the router classifies it `SIMPLE`, selects Haiku, and makes zero additional API calls

#### Scenario: Analytical message routed to Sonnet
- **WHEN** a chat message contains analytical keywords such as "trend", "progress", "comparison", "last 4 weeks", or "volume"
- **THEN** the router classifies it `ANALYTICAL` and selects `"claude-sonnet-4-6"`

#### Scenario: Medical query routed to Opus
- **WHEN** a chat message contains medical keywords such as "blood", "HRV", "fatigue", "health audit", or "marker"
- **THEN** the router classifies it `MEDICAL` and selects `"claude-opus-4-6"`

### Requirement: LLM classifier fallback for ambiguous chat messages
The system SHALL invoke a Haiku classification call when heuristics cannot determine intent with high confidence. The classifier SHALL receive the user message and return one of `SIMPLE`, `ANALYTICAL`, or `MEDICAL`.

#### Scenario: Ambiguous message triggers classifier
- **WHEN** a chat message is longer than 80 characters and matches no clear keyword pattern
- **THEN** the router makes a single Haiku API call to classify intent, then selects the model corresponding to the classification result

#### Scenario: Classifier result determines final model
- **WHEN** the Haiku classifier returns `ANALYTICAL`
- **THEN** the router selects `"claude-sonnet-4-6"` for the main response

### Requirement: Model IDs are configuration constants, not magic strings
The system SHALL define all model ID strings as named constants in a configuration class or enum. No controller, service, or scheduler SHALL hard-code a model ID string inline.

#### Scenario: Model constant referenced by name
- **WHEN** `ModelRouter` returns a model ID
- **THEN** the value originates from a named constant, not a literal string defined inline in the routing logic
