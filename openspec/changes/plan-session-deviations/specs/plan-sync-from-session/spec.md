## ADDED Requirements

### Requirement: Sync plan exercises from a completed session
The system SHALL allow the user to explicitly replace a training plan's exercise list with the actual exercises performed in a given session.

**Endpoint:** `POST /api/training-plans/{planId}/sync-from-session/{sessionId}`

**Request body:** none (action is fully determined by the session)

**Response:** updated `TrainingPlanDto` (HTTP 200)

#### Scenario: Plan exercises replaced with session exercises
- **WHEN** user calls the sync endpoint with a valid `planId` and `sessionId` they own
- **THEN** all existing `PlannedExercise` rows for the plan are deleted and new ones are inserted mirroring the session's `Exercise` rows (name, sets, position)
- **AND** `repsMin` / `repsMax` are derived from `workout_sets` reps data if available (mode value per exercise), otherwise copied from the `plan_snapshot` entry for that exercise, otherwise set to 0

#### Scenario: Reps derived from workout sets
- **WHEN** a session exercise has completed `workout_sets` with reps recorded
- **THEN** the new `PlannedExercise.repsMin` and `repsMax` are both set to the modal (most frequent) reps value across that exercise's sets

#### Scenario: Reps fallback to snapshot
- **WHEN** a session exercise has no reps recorded in `workout_sets` AND the exercise has a matching snapshot entry
- **THEN** `repsMin` and `repsMax` are copied from the snapshot entry

#### Scenario: Reps fallback to zero
- **WHEN** a session exercise has no reps and no snapshot entry
- **THEN** `repsMin = 0`, `repsMax = 0`

#### Scenario: Session not owned by authenticated user
- **WHEN** user provides a `sessionId` belonging to another user
- **THEN** system returns 404

#### Scenario: Plan not owned by authenticated user
- **WHEN** user provides a `planId` belonging to another user
- **THEN** system returns 404

#### Scenario: Session has no plan link required
- **WHEN** the session being synced from does not have `training_plan_id` matching `planId`
- **THEN** the sync is still allowed — the user may sync any of their sessions into any of their plans

#### Scenario: Sync from active session blocked
- **WHEN** the session has status `ACTIVE`
- **THEN** system returns 409 with error "Cannot sync from an active session — finish the session first"

### Requirement: Sync confirmation required in UI
The frontend SHALL prompt the user for confirmation before calling the sync endpoint, as it is a destructive operation.

#### Scenario: User confirms sync
- **WHEN** user clicks "Apply to plan" and confirms the dialog
- **THEN** the frontend calls `POST /api/training-plans/{planId}/sync-from-session/{sessionId}` and shows a success toast

#### Scenario: User cancels sync
- **WHEN** user clicks "Apply to plan" but cancels the confirmation dialog
- **THEN** no API call is made and the plan is unchanged

#### Scenario: Sync entry point on finished session screen
- **WHEN** a finished session was started from a plan (has `trainingPlanId`)
- **THEN** the finished session screen shows an "Apply to plan" button
