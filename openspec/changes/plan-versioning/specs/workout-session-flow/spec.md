## MODIFIED Requirements

### Requirement: Start workout session from training plan
When the user starts a session from a plan, the system SHALL create a `WorkoutSession` with `trainingPlanId` set, `startedAt = now()`, `date = now()`, one `Exercise` row per `PlannedExercise` with `sets` copied from the plan and `reps`/`weightKg` set to null, and SHALL also write `planVersion = plan.currentVersion` on the session row in the same transaction.

#### Scenario: Start from a valid plan
- **WHEN** user sends `POST /api/workouts/sessions/start` with a valid `trainingPlanId`
- **THEN** system creates a `WorkoutSession` with `trainingPlanId` set, `startedAt = now()`, `date = now()`, one `Exercise` row per `PlannedExercise`, and `planVersion` equal to the plan's `currentVersion` at the time of the request

#### Scenario: Exercise definition propagated on start
- **WHEN** a `PlannedExercise` has `exerciseDefinitionId` set and the user starts a session from that plan
- **THEN** the created `Exercise` row inherits `exerciseDefinitionId` and `primaryMuscle` from the definition

#### Scenario: Start from plan owned by another user
- **WHEN** user sends `POST /api/workouts/sessions/start` with a `trainingPlanId` belonging to a different user
- **THEN** system returns 404

#### Scenario: Start from non-existent plan
- **WHEN** user sends `POST /api/workouts/sessions/start` with a `trainingPlanId` that does not exist
- **THEN** system returns 404

#### Scenario: planVersion is null for MANUAL sessions
- **WHEN** user creates a session without a `trainingPlanId` (MANUAL session)
- **THEN** `planVersion` is null on the created session row
