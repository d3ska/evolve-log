## ADDED Requirements

### Requirement: GeminiAdapter implements AiProvider
The system SHALL provide `GeminiAdapter implements AiProvider` in the `ai.provider.gemini` package. All HTTP calls to the Gemini API SHALL use `java.net.http.HttpClient`. No Google AI Java SDK SHALL be added as a Gradle dependency.

#### Scenario: GeminiAdapter identifies itself
- **WHEN** `GeminiAdapter.providerId()` is called
- **THEN** it returns the string `"google"`

#### Scenario: Adapter isolation enforced
- **WHEN** any class outside `ai.provider.gemini` is inspected
- **THEN** it contains no imports from `ai.provider.gemini` or any Google AI SDK package

---

### Requirement: Gemini API authentication via query parameter
The system SHALL authenticate Gemini requests by appending the API key as a `key` query parameter to the request URL. No `Authorization` or `x-api-key` header SHALL be sent. The API key SHALL be read from `GeminiRequestContext` (thread-local), which is set by `GeminiAdapter.prepareContext()` before each request.

#### Scenario: API key injected into URL
- **WHEN** `GeminiAdapter.stream()` is called with an API key set in `GeminiRequestContext`
- **THEN** the outgoing HTTP request URL contains `?key=<apiKey>` and no key appears in request headers

#### Scenario: Missing key throws before HTTP call
- **WHEN** `GeminiAdapter.stream()` is called and `GeminiRequestContext.getApiKey()` returns null
- **THEN** the adapter throws `IllegalStateException` before any HTTP connection is opened

---

### Requirement: Gemini message format mapping
The system SHALL map `AiRequest` fields to the Gemini `v1beta` request body format. The system prompt SHALL be sent as `systemInstruction.parts[0].text`. Messages SHALL be serialised as `contents[]` with `role` (`"user"` or `"model"`) and `parts[0].text`. The `AiMessage` role value `"assistant"` SHALL be remapped to `"model"` on serialisation.

#### Scenario: System prompt placed in systemInstruction
- **WHEN** `GeminiAdapter` serialises a request with a non-blank `systemPrompt`
- **THEN** the JSON body contains `"systemInstruction": { "parts": [{ "text": "<systemPrompt>" }] }`

#### Scenario: Assistant role remapped to model
- **WHEN** `AiRequest.messages` contains an `AiMessage` with `role = "assistant"`
- **THEN** the serialised Gemini content entry uses `"role": "model"`, not `"role": "assistant"`

---

### Requirement: Gemini tool format mapping
The system SHALL serialise `AiToolDefinition` instances into the Gemini `functionDeclarations` format. Tool parameters SHALL be placed inside `tools[0].functionDeclarations[n].parameters`. JSON Schema `type` values SHALL be uppercased (`"string"` → `"STRING"`, `"object"` → `"OBJECT"`, etc.).

#### Scenario: Tool definitions serialised correctly
- **WHEN** `GeminiAdapter` serialises a request containing one `AiToolDefinition`
- **THEN** the JSON body contains `"tools": [{ "functionDeclarations": [{ "name": "...", "description": "...", "parameters": { "type": "OBJECT", ... } }] }]`

---

### Requirement: Gemini SSE streaming
The system SHALL stream responses from `https://generativelanguage.googleapis.com/v1beta/models/{model}:streamGenerateContent?alt=sse&key={apiKey}` using `HttpResponse.BodyHandlers.ofLines()`. Each `data:` line SHALL be parsed as JSON. Text tokens (`candidates[0].content.parts[n].text`) SHALL be forwarded to `AiStreamSink.onToken()`. Function calls (`candidates[0].content.parts[n].functionCall`) SHALL be forwarded to `AiStreamSink.onToolUse()`.

#### Scenario: Text tokens forwarded in order
- **WHEN** the Gemini SSE stream emits multiple `data:` lines each containing a text part
- **THEN** `AiStreamSink.onToken()` is called once per line in the order received

#### Scenario: Function call emitted as tool use
- **WHEN** a `data:` line contains `functionCall` with `name` and `args`
- **THEN** `AiStreamSink.onToolUse()` is called with the tool name and args map

#### Scenario: Stream error propagated
- **WHEN** the Gemini HTTP connection returns a non-2xx status
- **THEN** `AiStreamSink.onError()` is called with a descriptive exception and `onDone()` is NOT called

---

### Requirement: Gemini model constants
The system SHALL define Gemini model ID strings as named constants in `GeminiModelConstants` in the `ai.provider.gemini` package. No other class SHALL define Gemini model ID literals.

#### Scenario: FAST tier constant defined
- **WHEN** `GeminiModelConstants.FLASH` is referenced
- **THEN** it equals `"gemini-2.0-flash"`

#### Scenario: BALANCED and SMART tier constant defined
- **WHEN** `GeminiModelConstants.PRO` is referenced
- **THEN** it equals `"gemini-2.5-pro"` (used for both BALANCED and SMART tiers)

---

### Requirement: GeminiRequestContext thread-local
The system SHALL provide `GeminiRequestContext` as a package-private final class in `ai.provider.gemini` with static methods `setApiKey(String)`, `getApiKey()`, and `clear()` backed by a `ThreadLocal<String>`.

#### Scenario: Key set and retrieved within same thread
- **WHEN** `GeminiRequestContext.setApiKey("key123")` is called and then `getApiKey()` is called on the same thread
- **THEN** `getApiKey()` returns `"key123"`

#### Scenario: Key cleared after request
- **WHEN** `GeminiRequestContext.clear()` is called
- **THEN** `GeminiRequestContext.getApiKey()` returns null
