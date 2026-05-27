## ADDED Requirements

### Requirement: System records set completion timestamp server-side
The system SHALL stamp `completed_at = now()` on a `workout_set` row when `completed` transitions
to `true` via `PATCH /api/workouts/sets/{setId}`. When `completed` transitions to `false`, the
system SHALL clear `completed_at` to `null`. The client SHALL NOT supply `completedAt` — it is
always derived server-side.

#### Scenario: Marking a set complete stamps completed_at
- **WHEN** `PATCH /api/workouts/sets/{setId}` is called with `{ "completed": true }`
- **THEN** the response includes a non-null `completedAt` timestamp close to `now()`
- **AND** subsequent `GET` of the session returns the same `completedAt` value

#### Scenario: Re-completing a set resets completed_at
- **WHEN** a set with `completed = true` is patched with `{ "completed": false }` then `{ "completed": true }` again
- **THEN** `completedAt` is reset to the time of the second completion, not the first

#### Scenario: Uncompleting a set clears completed_at
- **WHEN** `PATCH /api/workouts/sets/{setId}` is called with `{ "completed": false }`
- **THEN** the response has `completedAt: null`

### Requirement: WorkoutSetDto exposes completedAt
The system SHALL include `completedAt` (ISO-8601 UTC string, nullable) in every `WorkoutSetDto`
response. It SHALL be `null` for sets where `completed = false` or for sets created before this
feature was shipped (historical data).

#### Scenario: Incomplete set has null completedAt
- **WHEN** a set with `completed = false` is returned in any API response
- **THEN** `completedAt` is `null`

#### Scenario: Completed set has non-null completedAt
- **WHEN** a set with `completed = true` is returned in any API response
- **THEN** `completedAt` is a valid ISO-8601 UTC timestamp string

### Requirement: Active workout UI shows live rest timer after last completed set
During an active workout session, the system SHALL display a live count-up timer between the
last completed set and the next incomplete set within each exercise. The timer SHALL count up
from `lastCompletedSet.completedAt` using the same `useElapsedTimer` hook as the workout timer.

#### Scenario: Rest divider appears after completing a set
- **WHEN** the user marks a set as complete in the active workout view
- **THEN** a rest divider appears below that set showing "Resting: 0:00" counting up in real time

#### Scenario: Live divider becomes static when next set is completed
- **WHEN** the user marks the next set as complete
- **THEN** the divider between those two sets becomes static (e.g., "Rested: 1:42") and a new live divider appears after the newly completed set

#### Scenario: No divider shown for incomplete sets with no prior completed set
- **WHEN** no set in an exercise has been completed yet
- **THEN** no rest dividers are rendered in that exercise's set list

### Requirement: Active workout UI shows static rest times for historical completed sets
For all completed sets except the last one in an exercise, the system SHALL render a static rest
divider showing the duration between that set's `completedAt` and the next set's `completedAt`,
formatted as "Rested: M:SS".

#### Scenario: Static divider between two completed sets
- **WHEN** set N and set N+1 are both completed with known `completedAt` values
- **THEN** a divider between them shows "Rested: X:XX" (static, not counting)

### Requirement: Session history detail shows rest times between sets
In the session detail / history view, the system SHALL render a static rest divider between
consecutive sets of the same exercise whenever both sets have a non-null `completedAt`.
Sets without `completedAt` (MANUAL sessions or pre-feature data) SHALL show no divider.

#### Scenario: History shows rest times for sets completed during active workout
- **WHEN** a user views the detail of a finished session that was tracked via the active workout flow
- **THEN** rest durations are shown between each pair of consecutive completed sets

#### Scenario: History shows no rest times for manually entered sessions
- **WHEN** a user views the detail of a MANUAL session (retroactively entered)
- **THEN** no rest dividers are shown between sets
