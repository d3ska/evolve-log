## MODIFIED Requirements

### Requirement: Start workout session from training plan
The system SHALL create a new workout session pre-populated with exercises from a specified training plan when the user starts a workout. In addition, the session SHALL record a frozen snapshot of the plan's exercise list and SHALL link each created exercise back to its originating `PlannedExercise`.

#### Scenario: Start from a valid plan
- **WHEN** user sends `POST /api/workouts/sessions/start` with a valid `trainingPlanId`
- **THEN** system creates a `WorkoutSession` with `trainingPlanId` set, `startedAt = now()`, `date = now()`, and one `Exercise` row per `PlannedExercise` with `sets` copied from the plan and `reps`/`weightKg` set to null
- **AND** each created `Exercise` has `planned_exercise_id` set to the UUID of its originating `PlannedExercise`
- **AND** `workout_sessions.plan_snapshot` is set to a JSON array of all planned exercises at that moment (fields: `plannedExerciseId`, `name`, `sets`, `repsMin`, `repsMax`, `restSeconds`, `position`)
- **AND** system returns 201 with the created session DTO including `planSnapshot`

#### Scenario: Exercise definition propagated on start
- **WHEN** a `PlannedExercise` has `exerciseDefinitionId` set and the user starts a session from that plan
- **THEN** the created `Exercise` row inherits `exerciseDefinitionId` and `primaryMuscle` from the definition

#### Scenario: Start from plan owned by another user
- **WHEN** user sends `POST /api/workouts/sessions/start` with a `trainingPlanId` belonging to a different user
- **THEN** system returns 404

#### Scenario: Start from non-existent plan
- **WHEN** user sends `POST /api/workouts/sessions/start` with a `trainingPlanId` that does not exist
- **THEN** system returns 404

## ADDED Requirements

### Requirement: Finish workout session
The system SHALL record the finish time and return a combined session + volume summary when the user finishes a workout.

#### Scenario: Finish an active session
- **WHEN** user sends `POST /api/workouts/sessions/{id}/finish` on a session with `startedAt` set and no `finishedAt`
- **THEN** system sets `finishedAt = now()`, computes `durationMinutes = minutes between startedAt and finishedAt`, and returns 200 with session detail and volume summary

#### Scenario: Finish is idempotent
- **WHEN** user sends `POST /api/workouts/sessions/{id}/finish` on a session that already has `finishedAt` set
- **THEN** system returns 200 with the existing session detail and volume summary without modifying `finishedAt`

#### Scenario: Finish a session belonging to another user
- **WHEN** user sends `POST /api/workouts/sessions/{id}/finish` for a session owned by a different user
- **THEN** system returns 404

#### Scenario: Finish response includes volume summary
- **WHEN** the session has exercises with weight logged
- **THEN** the finish response `data.volume.totalVolumeLoad` reflects the sum of `sets × reps × weightKg` across all weighted exercises in the session

#### Scenario: Finish response when no weight logged
- **WHEN** the session has exercises but none have `weightKg` set
- **THEN** the finish response `data.volume.totalVolumeLoad` is zero and `data.volume.exerciseCount` reflects the number of exercises

### Requirement: Session timing fields exposed in session DTO
The system SHALL include `startedAt`, `finishedAt`, and computed `durationMinutes` in the workout session response DTO.

#### Scenario: Active session DTO
- **WHEN** a session has `startedAt` set but no `finishedAt`
- **THEN** the session DTO includes `startedAt`, `finishedAt: null`, and `durationMinutes: null`

#### Scenario: Finished session DTO
- **WHEN** a session has both `startedAt` and `finishedAt`
- **THEN** the session DTO includes both timestamps and `durationMinutes` as a positive integer

#### Scenario: Manual session DTO
- **WHEN** a session was created without using the start endpoint (no timestamps)
- **THEN** the session DTO includes `startedAt: null`, `finishedAt: null`, and `durationMinutes` as manually set (or null)
