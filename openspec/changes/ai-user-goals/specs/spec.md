# Spec: Persistent User Goals

## Database

1. `V23__ai_goals.sql` adds `goals TEXT` nullable column to `ai_settings`

## API

### PUT /api/ai/settings/goals

- Auth required (current user only)
- Request: `{ "goals": "string or null" }`
- `goals` max 2000 characters; `@Size(max = 2000)` on request DTO
- `goals = null` clears the field
- Response: `204 No Content` on success

### GET /api/ai/settings

- Response now includes `"goals": "..."` field (null when not set)
- No other response fields change

## Prompt Injection

1. `PromptContextBuilder` reads `AiSettingsRepository.findById(userId)`
2. When `goals != null && !goals.isBlank()`:
   - Prepend `## Your Goals\n{goals}\n\n` to the context string
3. When `goals` absent: context string unchanged (no empty section emitted)
4. Goals injected in every `buildContext()` call — chat and insight generation

## Acceptance Criteria

1. V23 migration runs cleanly on existing database (nullable column, no default)
2. `PUT /api/ai/settings/goals` with valid text → persisted, 204 returned
3. `PUT /api/ai/settings/goals` with null → goals column set to NULL, 204 returned
4. `PUT /api/ai/settings/goals` with >2000 chars → 400 validation error
5. `GET /api/ai/settings` returns goals in response
6. When goals set, system prompt contains `## Your Goals` section with the text
7. When goals null, `## Your Goals` section absent from prompt
8. Goals section appears before `## Available Data` in the context
