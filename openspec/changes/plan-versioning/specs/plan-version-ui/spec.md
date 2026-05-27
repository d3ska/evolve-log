## ADDED Requirements

### Requirement: "Plan updated" badge on historical session cards
The WorkoutsPage session history SHALL display a "Plan updated" badge on any session card where `planVersion` is non-null and less than the plan's `currentVersion`.

#### Scenario: Session on outdated plan version shows badge
- **WHEN** a completed session has `planVersion = 2` and the associated plan has `currentVersion = 4`
- **THEN** the session card displays a "Plan updated" badge

#### Scenario: Session on current plan version shows no badge
- **WHEN** a completed session has `planVersion = 4` and the plan's `currentVersion = 4`
- **THEN** no version badge is shown on the session card

#### Scenario: Session with null planVersion shows no badge
- **WHEN** a session has `planVersion = null` (pre-V23 or MANUAL session)
- **THEN** no version badge is shown on the session card

#### Scenario: MANUAL session shows no badge
- **WHEN** a session has no associated training plan (`trainingPlanId = null`)
- **THEN** no version badge is shown regardless of plan state

### Requirement: Version History section on training plan detail view
The training plan detail view SHALL include a Version History section listing all recorded versions, each showing the version number, creation timestamp, and exercise count.

#### Scenario: Plan with version history displays list
- **WHEN** user views a training plan that has multiple version rows
- **THEN** the Version History section shows one entry per version, ordered oldest-first, with version number, date, and exercise count

#### Scenario: Plan with no version history shows empty state
- **WHEN** user views a training plan that has no `training_plan_versions` rows (pre-V23 plan)
- **THEN** the Version History section shows an empty state message ("No history yet")

### Requirement: Version diff display
Clicking a version entry in the Version History section SHALL display the exercise list for that version and highlight what changed compared to the immediately preceding version (exercises added, removed, or updated).

#### Scenario: First version shows no diff
- **WHEN** user clicks version 1 in the Version History
- **THEN** the full exercise list for version 1 is shown with no diff highlighting (no previous version to compare)

#### Scenario: Subsequent version shows added exercises
- **WHEN** user clicks version N (N > 1) and version N has an exercise not present in version N-1
- **THEN** the added exercise is highlighted or marked as "Added"

#### Scenario: Subsequent version shows removed exercises
- **WHEN** user clicks version N and version N-1 had an exercise absent from version N
- **THEN** the removed exercise is shown and marked as "Removed"

#### Scenario: Diff computed client-side from adjacent snapshots
- **WHEN** user clicks any version entry
- **THEN** the frontend fetches version N and version N-1 snapshots from the API and computes the diff locally without a dedicated diff endpoint
