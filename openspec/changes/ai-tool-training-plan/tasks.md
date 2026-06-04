# Tasks: ai-tool-training-plan

## Implementation

- [x] Create `GetTrainingPlanTool.java` in `com.deska.evolvelog.ai.tools`
  - Input param: `active_only` boolean (default true)
  - Use `TrainingPlanRepository.findByUserIdOrderByCreatedAtAsc`
  - Filter on `isActive` when `active_only=true`
  - Format per spec: position-ordered exercises, repsMin==repsMax collapse, nullable fields omitted
- [x] Update `PromptContextBuilder.buildContext()` to include training plan count hint
  - Inject `TrainingPlanRepository` dependency
  - Count active plans for userId and emit hint line

## Tests

- [ ] `GetTrainingPlanToolTest` — unit test with mock repository
  - active_only=true filters inactive plans
  - active_only=false returns all with [inactive] label
  - repsMin==repsMax renders without range
  - null dayOfWeek/block/restSeconds/notes omitted cleanly
  - empty result returns correct message
- [ ] `PromptContextBuilderTest` — verify plan count hint appears when plans exist
