## Context

`WorkoutSession` already has a `training_plan_id` FK and a `durationMinutes` integer (manually editable). `PlannedExercise` has `sets`, `repsMin`, `repsMax` but no link to the exercise definition catalog. There is no structured start/finish lifecycle — sessions are created as complete records.

The goal is to add a start→log→finish flow without breaking existing manually-created sessions.

## Goals / Non-Goals

**Goals:**
- Add `started_at` / `finished_at` timestamps to `workout_sessions` for automatic duration tracking
- Add `exercise_definition_id` (nullable) to `planned_exercises` for catalog alignment
- `POST /api/workouts/sessions/start` — creates a session from a plan with exercises pre-populated
- `POST /api/workouts/sessions/{id}/finish` — records finish time, returns session + volume summary
- Backward compatibility: existing manually-created sessions remain valid (no timestamps required)

**Non-Goals:**
- Session locking — exercises remain editable after finish
- Progression suggestions or plan-vs-actual comparison
- Per-set logging (sets remain aggregated on a single Exercise row)

## Decisions

### Session State: Derive from Timestamps, No State Column

Three implicit states:
- `MANUAL` — no `started_at`, no `finished_at` (existing behavior, still supported)
- `ACTIVE` — `started_at` set, no `finished_at` (in progress)
- `FINISHED` — both timestamps set

Adding a state enum column would require a migration and extra sync logic. Deriving state from timestamps is sufficient and simpler.

### Keep `durationMinutes` Alongside Timestamps

`WorkoutSession.durationMinutes` already exists and is used by the patch endpoint. On finish, the service computes `durationMinutes = minutes between startedAt and finishedAt` and persists both. Manual sessions that set `durationMinutes` directly continue to work. This avoids a breaking change to existing clients.

### Finish Is Idempotent: First Call Wins

If `finishedAt` is already set, `POST /finish` returns 200 with the existing summary — no error, no overwrite. This handles double-taps gracefully.

### Start From Plan Creates Exercise Rows With Null Actuals

Each `PlannedExercise` becomes an `Exercise` with:
- `sets` = planned sets (filled)
- `reps` = null (user fills in during workout)
- `weightKg` = null (user fills in during workout)
- `exerciseDefinitionId` = from `PlannedExercise.exerciseDefinitionId` (if linked)
- `primaryMuscle` = from definition (if linked)
- `name` = from `PlannedExercise.name`

This gives the user a pre-structured session to fill in without guessing what they planned.

### Finish Response: New Wrapper DTO

`POST /finish` returns a single response containing both the session detail and the volume summary:

```json
{
  "success": true,
  "data": {
    "session": { ...WorkoutSessionDto... },
    "volume": { ...SessionVolumeSummaryDto... }
  }
}
```

`SessionVolumeSummaryDto` is reused from `TrainingVolumeService` (built in `training-volume-analytics`). If no exercises have weight logged yet, `totalVolumeLoad` will be zero — acceptable.

### PlannedExercise Definition Link Is Optional

`exercise_definition_id` on `planned_exercises` is nullable. Existing plans keep working. Users can link plan exercises to definitions when editing a plan. When creating a session from a plan, only linked exercises propagate the definition FK — unlinked ones still create exercises by name (same as before).

### New Endpoint Placement

`POST /api/workouts/sessions/start` and `POST /api/workouts/sessions/{id}/finish` live in `WorkoutController` (alongside existing session CRUD), not in `TrainingPlanController`. The plan is the input to start, but the result is a session — session controller is the right owner.

## Risks / Trade-offs

- **Unfinished sessions**: A user who starts but never finishes leaves an `ACTIVE` session with null actuals. No cleanup needed — these sessions are valid and will appear in history. The client can detect `finishedAt == null` and offer to resume or finish.
- **Volume summary with no weight**: If the user finishes before filling in weights, `totalVolumeLoad = 0`. This is correct — no data means no volume. The summary still shows `exerciseCount` which is useful.
- **durationMinutes drift**: If a user edits `durationMinutes` manually on a session that also has timestamps, the two sources diverge. Acceptable — manual override is intentional.

## Migration Plan

Single migration V16:
```sql
ALTER TABLE planned_exercises
    ADD COLUMN exercise_definition_id UUID REFERENCES exercise_definitions(id) ON DELETE SET NULL;

ALTER TABLE workout_sessions
    ADD COLUMN started_at TIMESTAMP,
    ADD COLUMN finished_at TIMESTAMP;
```

No data backfill needed — all new columns are nullable. Rollback: drop the three columns.
