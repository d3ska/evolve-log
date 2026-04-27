## ADDED Requirements

### Requirement: AI settings storage per user
The system SHALL store AI provider configuration in an `ai_settings` table with `user_id` as the primary key (one row per user). The table SHALL contain `provider` (VARCHAR 50, default `'anthropic'`), `api_key_encrypted` (TEXT), `created_at`, and `updated_at`.

#### Scenario: One settings row per user
- **WHEN** a user saves their API key for the first time
- **THEN** exactly one row is inserted into `ai_settings` for that user

#### Scenario: Settings updated on re-save
- **WHEN** a user saves a new API key when one already exists
- **THEN** the existing row is updated (not a new row inserted) and `updated_at` is refreshed

### Requirement: AES-256-GCM API key encryption
The system SHALL encrypt the API key at rest using AES-256-GCM via a custom `EncryptedStringConverter implements AttributeConverter<String, String>`. The encryption key SHALL be loaded from the environment variable `AI_ENCRYPTION_KEY` (base64-encoded, 32 bytes). A 96-bit random IV SHALL be generated per encryption operation and prepended to the ciphertext.

#### Scenario: API key stored encrypted
- **WHEN** a user saves their API key
- **THEN** the value stored in `ai_settings.api_key_encrypted` is AES-256-GCM ciphertext, not the plaintext key

#### Scenario: API key decrypted on read
- **WHEN** the application loads a user's `AiSettings` entity
- **THEN** the `EncryptedStringConverter` transparently decrypts the key so the service receives the plaintext value

#### Scenario: Application fails fast without encryption key
- **WHEN** the application starts and `AI_ENCRYPTION_KEY` is not set or is shorter than 32 bytes
- **THEN** the application fails to start with a clear error message identifying the missing configuration

### Requirement: Settings GET endpoint — key never exposed
The system SHALL expose `GET /api/ai/settings` returning `{ "provider": "anthropic", "hasApiKey": true, "apiKeyHint": "sk-ant-...****" }`. The actual API key SHALL never be returned in any API response. `apiKeyHint` SHALL show only the last 4 characters of the plaintext key masked with asterisks.

#### Scenario: GET response omits plaintext key
- **WHEN** an authenticated user calls `GET /api/ai/settings`
- **THEN** the response contains `hasApiKey: true` and `apiKeyHint` with masked display, but no `apiKey` field with the full value

#### Scenario: GET response when no key configured
- **WHEN** an authenticated user calls `GET /api/ai/settings` and has not saved a key
- **THEN** the response contains `hasApiKey: false` and no `apiKeyHint` field

### Requirement: Settings PUT endpoint
The system SHALL expose `PUT /api/ai/settings` accepting `{ "provider": "anthropic", "apiKey": "sk-ant-..." }`. The endpoint SHALL validate that `apiKey` is non-blank and matches the expected prefix for the specified provider before persisting. The endpoint SHALL be authenticated.

#### Scenario: Valid key accepted and stored
- **WHEN** an authenticated user calls `PUT /api/ai/settings` with a non-blank API key
- **THEN** the key is encrypted and stored; the response returns HTTP 200 with the masked settings view

#### Scenario: Blank key rejected
- **WHEN** an authenticated user calls `PUT /api/ai/settings` with an empty or whitespace-only `apiKey`
- **THEN** the response returns HTTP 400 with a validation error message

### Requirement: System prompts never exposed via API
The system SHALL store all AI system prompts as classpath resources (`src/main/resources/prompts/{task_type}.txt`). No API endpoint SHALL return prompt content. No settings UI SHALL allow users to view or edit prompts.

#### Scenario: Prompts loaded at startup
- **WHEN** the application starts
- **THEN** `PromptLoader` reads all prompt files from the classpath and stores them in memory; missing files cause a startup error

#### Scenario: No prompt endpoint exists
- **WHEN** any client attempts to access a URL containing `/prompt` or `/system-prompt`
- **THEN** the server returns HTTP 404

### Requirement: Model selection invisible to user
The system SHALL select AI models automatically based on task type and message content. No user-facing API endpoint or UI element SHALL expose model names, allow model selection, or reveal which model was used for a chat response. The `modelUsed` field SHALL be stored internally in `ai_insights` for operational observability but SHALL NOT appear in chat response payloads.

#### Scenario: Chat response contains no model field
- **WHEN** the chat SSE stream completes
- **THEN** none of the SSE events include a `modelId` or `model` field in their data payload
