## MODIFIED Requirements

### Requirement: Settings page includes AI configuration section
The existing settings page SHALL be extended with a new "AI Configuration" section. This section SHALL contain exactly one user-editable field: the API key input. The section SHALL NOT expose model selection, system prompt editing, or any other AI internals. Saving SHALL call `PUT /api/ai/settings`.

#### Scenario: AI section visible in settings
- **WHEN** an authenticated user navigates to the Settings page
- **THEN** an "AI Configuration" section is visible containing an API key field and a save button

#### Scenario: Existing API key shown as masked hint
- **WHEN** the user opens Settings and an API key is already configured
- **THEN** the API key field displays a masked value (e.g., `sk-ant-...****`) and is not pre-filled with the plaintext key

#### Scenario: New API key saved successfully
- **WHEN** the user enters a non-blank API key and clicks Save
- **THEN** `PUT /api/ai/settings` is called, and the field updates to show the new masked hint on success

#### Scenario: No model selection exposed
- **WHEN** the user views the AI Configuration section
- **THEN** no dropdown, radio button, or other control for selecting AI model is present

#### Scenario: No system prompt editing exposed
- **WHEN** the user views the AI Configuration section
- **THEN** no textarea, field, or link for viewing or editing system prompts is present
