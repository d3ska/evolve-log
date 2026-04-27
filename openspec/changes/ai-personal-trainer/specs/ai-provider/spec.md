## ADDED Requirements

### Requirement: Provider-agnostic AI interface
The system SHALL expose an `AiProvider` interface that abstracts all LLM provider details. Business logic throughout the application SHALL depend only on this interface, never on provider-specific types.

#### Scenario: Blocking completion returns response
- **WHEN** a caller invokes `AiProvider.complete(AiRequest)` with a valid request
- **THEN** the system returns a fully populated `AiResponse` containing the assistant message text and token usage metadata

#### Scenario: Streaming response delivers tokens
- **WHEN** a caller invokes `AiProvider.stream(AiRequest, AiStreamSink)` with a valid request
- **THEN** the provider calls `AiStreamSink.onToken()` for each incremental text chunk, `AiStreamSink.onToolUse()` for each tool the model requests, and `AiStreamSink.onDone()` when complete

#### Scenario: Stream error propagated via callback
- **WHEN** the provider encounters a network or API error during streaming
- **THEN** the provider calls `AiStreamSink.onError(Throwable)` and does NOT call `onDone()`

### Requirement: AiRequest immutable data model
The system SHALL define `AiRequest` as an immutable record carrying: `modelId` (String), `systemPrompt` (String), `messages` (List of `AiMessage`), `tools` (List of `AiToolDefinition`), and `temperature` (double). The `tools` and `messages` fields SHALL be empty lists rather than null when unused.

#### Scenario: Request is immutable
- **WHEN** code constructs an `AiRequest` with all required fields
- **THEN** the resulting record is immutable and all fields are accessible via record accessors with no mutation methods

### Requirement: AiStreamSink callback interface
The system SHALL define `AiStreamSink` as an interface with five methods: `onToken(String text)`, `onToolUse(String toolName, Map<String,Object> input)`, `onToolResult(String toolName, int rowCount)`, `onDone()`, and `onError(Throwable cause)`. The SSE controller adapter SHALL implement this interface.

#### Scenario: Tool use callback fires before execution
- **WHEN** the model requests a tool during streaming
- **THEN** `onToolUse` is called with the tool name and parsed input map before tool execution begins

#### Scenario: Token order preserved
- **WHEN** the model streams text tokens
- **THEN** `onToken` is called in the exact order tokens are received from the provider

### Requirement: ClaudeAdapter is the sole Anthropic-aware class
The system SHALL have exactly one class that references Anthropic API shapes: `ClaudeAdapter implements AiProvider` in the `ai.provider.anthropic` package. All HTTP calls to the Anthropic Messages API SHALL use `java.net.http.HttpClient`. No Anthropic Java SDK SHALL be added as a Maven/Gradle dependency.

#### Scenario: Adapter isolation enforced
- **WHEN** any class outside `ai.provider.anthropic` is inspected
- **THEN** it contains no imports from any Anthropic SDK package

#### Scenario: Streaming via raw HTTP lines
- **WHEN** `ClaudeAdapter.stream()` is called
- **THEN** the adapter opens a streaming HTTP connection using `HttpResponse.BodyHandlers.ofLines()` and emits SSE events to the `AiStreamSink` as lines arrive

### Requirement: Provider identity declaration
Each `AiProvider` implementation SHALL expose a `providerId()` method returning a lowercase string identifier (e.g., `"anthropic"`). This value SHALL match the `provider` column stored in `ai_settings`.

#### Scenario: ClaudeAdapter identifies itself
- **WHEN** `ClaudeAdapter.providerId()` is called
- **THEN** it returns the string `"anthropic"`
