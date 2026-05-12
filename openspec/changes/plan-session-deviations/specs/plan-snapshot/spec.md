## ADDED Requirements

### Requirement: Plan snapshot stored at session start
When a workout session is started from a training plan, the system SHALL freeze the plan's current exercise list as a JSONB document on the session row so that "what was planned" is preserved independently of future plan edits.

#### Scenario: Snapshot written on startFromPlan
- **WHEN** user calls `POST /api/workouts/sessions/start` with a valid `trainingPlanId`
- **THEN** `workout_sessions.plan_snapshot` is set to a JSON array containing one object per `PlannedExercise`, in position order, with fields: `plannedExerciseId`, `name`, `sets`, `repsMin`, `repsMax`, `restSeconds` (nullable), `position`
- **AND** the snapshot is written atomically in the same transaction as the session and exercises

#### Scenario: Snapshot reflects plan state at start time
- **WHEN** the user edits the training plan after the session has started
- **THEN** `plan_snapshot` on the existing session is unchanged

#### Scenario: No snapshot for manual sessions
- **WHEN** a `WorkoutSession` is created via the manual workout endpoint (not `startFromPlan`)
- **THEN** `plan_snapshot` is `NULL`

#### Scenario: No snapshot for sessions with no plan
- **WHEN** user calls `POST /api/workouts/sessions/start` without a `trainingPlanId`
- **THEN** request is rejected (existing behaviour); `plan_snapshot` is never written

### Requirement: planned_exercise_id set on plan-originated exercises
Each `Exercise` row created by `startFromPlan` SHALL store a reference to the `PlannedExercise` it was created from.

#### Scenario: planned_exercise_id populated on start
- **WHEN** `startFromPlan` creates session exercises from a plan
- **THEN** each `Exercise` row has `planned_exercise_id` set to the UUID of the originating `PlannedExercise`

#### Scenario: Mid-session added exercises have no planned_exercise_id
- **WHEN** the user adds an exercise to an active session via `POST /api/workouts/exercises`
- **THEN** the created `Exercise` row has `planned_exercise_id = NULL`

#### Scenario: planned_exercise_id survives plan deletion
- **WHEN** the user deletes the training plan after completing a session
- **THEN** `exercises.planned_exercise_id` is set to `NULL` by cascade (ON DELETE SET NULL), but `plan_snapshot` on the session still contains the full original exercise list

### Requirement: plan_snapshot exposed in session DTO
The session DTO SHALL include the `planSnapshot` field so the frontend can display planned vs actual comparisons without a separate API call.

#### Scenario: plan_snapshot included in session response
- **WHEN** a plan-based session is fetched via `GET /api/workouts/{id}` or returned by start/finish endpoints
- **THEN** the response `data.planSnapshot` contains the frozen exercise array (or `null` for non-plan sessions)
