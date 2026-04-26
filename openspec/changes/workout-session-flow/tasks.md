## 1. Database — V16 Migration

- [x] 1.1 Create `V16__workout_session_flow.sql`: add `exercise_definition_id UUID REFERENCES exercise_definitions(id) ON DELETE SET NULL` to `planned_exercises`
- [x] 1.2 Add `started_at TIMESTAMP` and `finished_at TIMESTAMP` (both nullable) to `workout_sessions`
- [x] 1.3 Add index on `planned_exercises(exercise_definition_id)`

## 2. Backend — PlannedExercise Definition Link

- [x] 2.1 Add `exerciseDefinitionId UUID` field to `PlannedExercise` entity (`@Getter` only, no setter)
- [x] 2.2 Add `exerciseDefinitionId` to `CreatePlannedExerciseRequest` (nullable `UUID`)
- [x] 2.3 Add `exerciseDefinitionId` to `UpdatePlannedExerciseRequest` (nullable `UUID`)
- [x] 2.4 Add `exerciseDefinitionId` to `PlannedExerciseDto` and update `PlannedExerciseDto.from()`
- [x] 2.5 Update `PlannedExercise.applyPatch` to accept and apply `exerciseDefinitionId`
- [x] 2.6 Update `TrainingPlanService.addExercise` and `buildExercises`: validate that `exerciseDefinitionId` (when provided) belongs to a system definition or to the requesting user, then set it on the builder
- [x] 2.7 Update `TrainingPlanService.updateExercise` to pass `exerciseDefinitionId` through `applyPatch`

## 3. Backend — WorkoutSession Timing Fields

- [x] 3.1 Add `startedAt LocalDateTime` and `finishedAt LocalDateTime` fields to `WorkoutSession` entity
- [x] 3.2 Expose `startedAt` and `finishedAt` in `WorkoutSessionDto` (both nullable)
- [x] 3.3 Update `WorkoutSessionDto.from()` and `WorkoutSessionDto.summary()` to include the new fields

## 4. Backend — Start From Plan

- [x] 4.1 Create `StartWorkoutSessionRequest` record with `@NotNull UUID trainingPlanId`
- [x] 4.2 Create `WorkoutSessionFlowService` (or add to `WorkoutService`): implement `startFromPlan(User user, UUID trainingPlanId)`
  - Load plan, verify ownership (404 if not found or not owned)
  - Create `WorkoutSession` with `trainingPlanId`, `startedAt = now()`, `date = now()`
  - For each `PlannedExercise`: create `Exercise` with `sets` from plan, `reps = null`, `weightKg = null`, `name`, `position`, `exerciseDefinitionId` and `primaryMuscle` (if definition linked)
  - Return saved session
- [x] 4.3 Add `POST /api/workouts/sessions/start` to `WorkoutController`, returns 201 with `WorkoutSessionDto`

## 5. Backend — Finish Workout

- [x] 5.1 Create `FinishedSessionDto` record: `WorkoutSessionDto session`, `SessionVolumeSummaryDto volume`
- [x] 5.2 Implement `finishSession(UUID sessionId, UUID userId)` in `WorkoutSessionFlowService`:
  - Load session, verify ownership (404 if not found)
  - If `finishedAt` already set: return existing session + volume summary (idempotent)
  - Set `finishedAt = now()`, compute `durationMinutes = minutes between startedAt and finishedAt` (only if `startedAt` is set)
  - Save session, call `TrainingVolumeService.getSessionVolumeSummary`, return `FinishedSessionDto`
- [x] 5.3 Add `POST /api/workouts/sessions/{id}/finish` to `WorkoutController`, returns 200 with `FinishedSessionDto`

## 6. Backend — Tests

- [x] 6.1 Write `WorkoutSessionFlowServiceTest`: start from valid plan creates session + exercises with null actuals; start propagates definition FK; start with unknown plan → 404; start with another user's plan → 404
- [x] 6.2 Write `WorkoutSessionFlowServiceTest`: finish sets timestamps and duration; finish idempotent on already-finished session; finish on another user's session → 404
- [x] 6.3 Write `WorkoutControllerTest` (MockMvc): `POST /start` → 201; `POST /start` unknown plan → 404; `POST /{id}/finish` → 200 with volume; `POST /{id}/finish` already finished → 200
