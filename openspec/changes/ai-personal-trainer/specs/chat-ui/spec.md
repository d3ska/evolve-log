## ADDED Requirements

### Requirement: Floating chat button on all pages
The system SHALL render a floating action button (FAB) in the bottom-right corner of every authenticated page. Clicking the FAB SHALL open the `ChatOverlay` panel. The FAB SHALL remain visible while the overlay is open and function as a close toggle.

#### Scenario: FAB visible on all authenticated pages
- **WHEN** an authenticated user views any page (dashboard, workouts, progress, blood, insights)
- **THEN** the floating chat button is visible in the bottom-right corner

#### Scenario: FAB toggles overlay
- **WHEN** the user clicks the FAB while the overlay is closed
- **THEN** the ChatOverlay opens; clicking FAB again closes it

### Requirement: ChatOverlay panel with message history
The system SHALL implement a `ChatOverlay` component that renders as a fixed-position panel. It SHALL display the current conversation's message history with role-based styling (user messages right-aligned, assistant messages left-aligned). The panel SHALL include a text input and send button.

#### Scenario: Messages displayed with role styling
- **WHEN** the ChatOverlay has messages from both user and assistant
- **THEN** user messages appear right-aligned and assistant messages appear left-aligned with distinct visual styling

#### Scenario: Input cleared after send
- **WHEN** the user submits a message
- **THEN** the text input is cleared immediately and the user message appears in the conversation

### Requirement: Token-by-token streaming rendering
The system SHALL connect to the `POST /api/ai/chat` SSE endpoint using the native browser `EventSource` API. Assistant response text SHALL appear incrementally as `token` events arrive, not waiting for the full response before rendering.

#### Scenario: Text appears token by token
- **WHEN** the AI is generating a response
- **THEN** each received `token` event appends the text chunk to the assistant message bubble in real time

#### Scenario: EventSource closed on done event
- **WHEN** the `done` SSE event arrives
- **THEN** the EventSource connection is closed and the input is re-enabled

### Requirement: Tool status banner during tool execution
The system SHALL display a `ToolStatusBanner` component when a `tool_use` SSE event is received. The banner SHALL show the human-readable label from the event (e.g., "Checking your bench press history…") and disappear when the corresponding `tool_result` event arrives or on `done`.

#### Scenario: Banner shown during tool execution
- **WHEN** a `tool_use` event arrives with label `"Checking your bench press history…"`
- **THEN** the `ToolStatusBanner` appears with that label text and the input area remains disabled

#### Scenario: Banner dismissed after tool result
- **WHEN** the `tool_result` event arrives after a `tool_use` event
- **THEN** the `ToolStatusBanner` is dismissed

### Requirement: Error display in chat
The system SHALL display error messages from the `error` SSE event as a styled error bubble in the conversation. The error bubble SHALL include a user-safe message and a suggestion to check settings if the error indicates an invalid API key.

#### Scenario: Error event shown as error bubble
- **WHEN** an `error` SSE event arrives
- **THEN** a red-styled error bubble appears in the conversation with the event's message text

### Requirement: Quick action buttons per page context
The system SHALL render contextual `QuickActions` above the chat input based on the current page. Clicking a quick action SHALL pre-fill the input with the action's prompt text and optionally auto-submit.

#### Scenario: Quick actions rendered on progress page
- **WHEN** the ChatOverlay is open on the `/progress` page
- **THEN** quick action buttons appear including prompts like "How was my volume this week?" and "Am I overtraining chest?"

#### Scenario: Quick action pre-fills input
- **WHEN** the user clicks a quick action button
- **THEN** the chat input is filled with the action's prompt text

### Requirement: Conversation ID persisted in localStorage
The system SHALL generate a UUID v4 conversation ID on the first message of a new conversation and store it in `localStorage` under the key `evolveLog_conversationId`. This ID SHALL be sent with every subsequent message. A "New chat" action SHALL clear the stored ID, causing a new conversation to start.

#### Scenario: Conversation ID generated and persisted on first message
- **WHEN** the user sends the first message in a new session
- **THEN** a UUID is generated, stored in `localStorage`, and included in the API request

#### Scenario: Existing conversation resumed on page reload
- **WHEN** the user reloads the page and opens the ChatOverlay
- **THEN** the stored `conversationId` is read from `localStorage` and used in the next message

#### Scenario: New chat clears conversation ID
- **WHEN** the user clicks "New chat"
- **THEN** the `localStorage` entry is removed, the message history is cleared, and the next message starts a new conversation

### Requirement: Thinking indicator before first token
The system SHALL display an animated "thinking" indicator from the moment a message is sent until the first `token` event is received. The indicator SHALL disappear immediately when the first text token arrives.

#### Scenario: Thinking indicator shown on send
- **WHEN** the user submits a message
- **THEN** an animated thinking indicator appears in the assistant message area immediately

#### Scenario: Thinking indicator replaced by text
- **WHEN** the first `token` event arrives
- **THEN** the thinking indicator is replaced by the streamed text
