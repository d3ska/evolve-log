# Tasks: ai-user-goals

## Implementation

- [x] Create `V23__ai_goals.sql` migration
- [x] Update `AiSettings.java` — add `goals TEXT` field + `updateGoals()` method
- [x] Update `AiSettingsService` — add `updateGoals(userId, goals)`, include goals in settings view
- [x] Create `UpdateGoalsRequest` DTO with `@Size(max=2000)` constraint
- [x] Update `AiSettingsController` — add `PUT /api/ai/settings/goals` endpoint
- [x] Update `PromptContextBuilder` — inject `AiSettingsRepository`, prepend goals section when set

## Tests

- [ ] `AiSettingsServiceTest` — updateGoals persists; null clears field
- [ ] `AiSettingsControllerTest` (MockMvc) — 204 on valid, 400 on >2000 chars
- [ ] `PromptContextBuilderTest` — goals section present/absent correctly
