## ADDED Requirements

### Requirement: Version row created on plan creation
When a new training plan is created, the system SHALL initialize `current_version = 1` on the plan row and insert a corresponding `training_plan_versions` row capturing the initial (empty) exercise list, in the same transaction.

#### Scenario: New plan has version 1
- **WHEN** user creates a new training plan
- **THEN** `training_plans.current_version` is set to `1` and a `training_plan_versions` row with `version = 1` and `exercises = []` is persisted

### Requirement: Version bump on PlannedExercise mutation
The system SHALL increment `training_plans.current_version` and insert a new `training_plan_versions` row (with the updated exercise list snapshot) inside the same transaction as any of the following operations: add a `PlannedExercise`, remove a `PlannedExercise`, or update a `PlannedExercise`.

#### Scenario: Add exercise bumps version
- **WHEN** user adds a `PlannedExercise` to a plan that is at version N
- **THEN** `training_plans.current_version` becomes N+1 and a `training_plan_versions` row for version N+1 is inserted with the full updated exercise list

#### Scenario: Remove exercise bumps version
- **WHEN** user removes a `PlannedExercise` from a plan that is at version N
- **THEN** `training_plans.current_version` becomes N+1 and a `training_plan_versions` row for version N+1 is inserted with the remaining exercise list

#### Scenario: Update exercise bumps version
- **WHEN** user updates a `PlannedExercise` (e.g. changes sets, reps, or weight) on a plan that is at version N
- **THEN** `training_plans.current_version` becomes N+1 and a `training_plan_versions` row for version N+1 is inserted with the updated exercise list

#### Scenario: Metadata-only update does not bump version
- **WHEN** user updates only plan metadata (name, description, scheduled days) without touching any `PlannedExercise`
- **THEN** `training_plans.current_version` is unchanged and no new `training_plan_versions` row is inserted

#### Scenario: Version bump is atomic with the mutation
- **WHEN** the exercise mutation succeeds
- **THEN** the version increment and version row insert are committed in the same transaction; if either fails the entire operation rolls back

### Requirement: Version snapshot format
The `training_plan_versions.exercises` JSONB column SHALL store the full list of `PlannedExercise` records for that version in the same structure used by `workout_sessions.plan_snapshot`, so the frontend can reuse the same deserialization logic.

#### Scenario: Snapshot contains all planned exercises
- **WHEN** a version row is created after adding exercise B to a plan that already had exercise A
- **THEN** the `exercises` JSONB contains both A and B with all their fields (name, sets, reps, weightKg, position, exerciseDefinitionId)

### Requirement: Version history list endpoint
The system SHALL expose `GET /api/training-plans/{id}/versions` returning a list of all version summaries for the given plan, ordered ascending by version number.

#### Scenario: List versions for own plan
- **WHEN** authenticated user requests `GET /api/training-plans/{id}/versions` for a plan they own
- **THEN** system returns 200 with a list of `TrainingPlanVersionDto` objects, each containing `version`, `createdAt`, and `exerciseCount`

#### Scenario: List versions for another user's plan
- **WHEN** authenticated user requests versions for a plan owned by a different user
- **THEN** system returns 404

#### Scenario: List versions for plan with no history rows
- **WHEN** the plan exists but was created before V23 (no version rows)
- **THEN** system returns 200 with an empty list

### Requirement: Single version snapshot endpoint
The system SHALL expose `GET /api/training-plans/{id}/versions/{version}` returning the full exercise snapshot for the requested version.

#### Scenario: Fetch existing version
- **WHEN** authenticated user requests `GET /api/training-plans/{id}/versions/2` and version 2 exists
- **THEN** system returns 200 with `TrainingPlanVersionDto` including `version`, `createdAt`, and `exercises` array

#### Scenario: Fetch non-existent version
- **WHEN** user requests a version number that does not exist for the plan
- **THEN** system returns 404

#### Scenario: Fetch version for another user's plan
- **WHEN** user requests a version of a plan they do not own
- **THEN** system returns 404
