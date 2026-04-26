## Why

Workout sessions are currently logged manually with no connection to training plans, no timing, and no summary at the end — making it impossible to use plans as a guide during training or to see a meaningful recap after finishing. This change connects plans to sessions and adds a structured start/finish flow.

## What Changes

- Add `exercise_definition_id` (nullable FK) to `planned_exercises` so plan exercises align with the exercise catalog introduced in `training-volume-analytics`
- Add `started_at` and `finished_at` timestamps to `workout_sessions` to track training duration
- New endpoint: `POST /api/workouts/sessions/start` — creates a session pre-loaded with exercises from a chosen training plan day
- New endpoint: `POST /api/workouts/sessions/{id}/finish` — locks the session, records finish time, returns session data + volume summary in one response
- `WorkoutSession` remains editable between start and finish (exercises logged as you train)

## Capabilities

### New Capabilities

- `workout-session-flow`: Structured start/finish lifecycle for workout sessions — starting from a plan, timing the session, and receiving a post-workout summary on finish

### Modified Capabilities

- `exercise-catalog`: Add `exercise_definition_id` FK to `planned_exercises` so plan exercises can be linked to definitions (enables accurate analytics per plan)

## Impact

- 1 new Flyway migration (V16): `exercise_definition_id` on `planned_exercises`, `started_at`/`finished_at` on `workout_sessions`
- `PlannedExercise` entity: add `exerciseDefinitionId` field
- `WorkoutSession` entity: add `startedAt`, `finishedAt` fields
- New `WorkoutSessionService` methods (or new service): `startFromPlan`, `finishSession`
- `TrainingPlanController` or new `WorkoutSessionController`: two new endpoints
- `TrainingVolumeService.getSessionVolumeSummary` reused in finish response
