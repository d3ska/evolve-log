# Spec: active-workout-ui

## Requirements

### Requirement: Active workout timer

The UI SHALL display a persistent elapsed time counter for the current workout.

#### Scenario: Timer starts from session start time
- **WHEN** the `ActiveWorkoutPage` mounts with a session whose `startedAt` is set
- **THEN** the timer displays `HH:mm:ss` elapsed since `startedAt` and increments every second

#### Scenario: Timer survives navigation away and back
- **WHEN** the user navigates away from `/workouts/active` and returns
- **THEN** the timer resumes from the correct elapsed time (calculated from `startedAt` returned by the API)

#### Scenario: No startedAt (MANUAL session)
- **WHEN** the session has no `startedAt`
- **THEN** no timer is shown

---

### Requirement: Atomic set save with zero data loss

The UI SHALL save every set mutation immediately without blocking the user interaction.

#### Scenario: User changes reps or weight
- **WHEN** the user edits a reps or weight field and blurs the input
- **THEN** the UI immediately sends `PATCH /api/workouts/sets/{setId}` in the background
- **AND** the input remains responsive (not disabled) during the save

#### Scenario: Save succeeds
- **WHEN** the `PATCH` call returns 200
- **THEN** no visible change (the optimistic state matches the server)

#### Scenario: Save fails
- **WHEN** the `PATCH` call returns an error
- **THEN** the input reverts to the last successfully saved value
- **AND** a toast notification is shown: "Failed to save — tap to retry"

#### Scenario: In-flight save indicator
- **WHEN** one or more `PATCH` or `POST` requests are in-flight
- **THEN** a "Saving…" indicator is shown in the page header

#### Scenario: App refresh during active workout
- **WHEN** the user refreshes the browser while on `/workouts/active`
- **THEN** the page reloads, calls `GET /api/workouts/sessions/active`, and restores the full session state
- **AND** zero set data is lost

---

### Requirement: Completed vs collapsed exercise states

The UI SHALL visually distinguish completed sets and automatically collapse fully-completed exercises.

#### Scenario: Mark set as completed
- **WHEN** the user taps the completion toggle on a set row
- **THEN** the row transitions to a green visual state and the inputs become read-only
- **AND** `PATCH /api/workouts/sets/{setId}` is called with `{ "completed": true }`

#### Scenario: Unmark set as completed
- **WHEN** the user taps the toggle again on a completed set
- **THEN** the row returns to editable state
- **AND** `PATCH /api/workouts/sets/{setId}` is called with `{ "completed": false }`

#### Scenario: Auto-collapse fully completed exercise
- **WHEN** all sets of an exercise have `completed = true`
- **THEN** the exercise card automatically collapses to a one-line summary showing: exercise name + total volume (e.g. "3 × 8 @ 100 kg")

#### Scenario: Manual expand/collapse
- **WHEN** the user taps the collapse toggle on any exercise card
- **THEN** the card toggles between expanded and collapsed view regardless of completion state
- **AND** this state is UI-local (not persisted)

---

### Requirement: Full CRUD on sets during active workout

#### Scenario: Add a set
- **WHEN** the user taps "Add set" within an exercise card
- **THEN** a new set row is appended with the next `setNumber` and empty reps/weight inputs
- **AND** `POST /api/workouts/sets` is called immediately

#### Scenario: Delete a set
- **WHEN** the user swipes a set row left (mobile) or taps the delete icon
- **THEN** the row is removed optimistically from the UI
- **AND** `DELETE /api/workouts/sets/{setId}` is called

#### Scenario: Add an exercise
- **WHEN** the user taps "Add exercise"
- **THEN** the `ExerciseAutocomplete` modal opens (reuses existing `ExerciseAutocomplete` component)
- **AND** on selection, `POST /api/workouts/exercises` is called and the new card is appended

#### Scenario: Remove an exercise
- **WHEN** the user taps the remove icon on an exercise card header
- **AND** confirms (if the exercise has any completed sets)
- **THEN** `DELETE /api/workouts/exercises/{exerciseId}` is called and the card is removed

---

### Requirement: Resume active session recovery

The UI SHALL detect and surface an interrupted workout session.

#### Scenario: Active session detected on app load
- **WHEN** `AppBootstrap` calls `GET /api/workouts/sessions/active` and receives a non-null session
- **AND** the current route is NOT `/workouts/active`
- **THEN** a persistent banner appears at the top of the page:
  "Workout in progress — [duration elapsed]. Resume →"

#### Scenario: User taps Resume
- **WHEN** the user taps the Resume banner
- **THEN** the app navigates to `/workouts/active` and loads the active session

#### Scenario: No active session
- **WHEN** `GET /api/workouts/sessions/active` returns null
- **THEN** no banner is shown

---

### Requirement: Finish workout

#### Scenario: User taps Finish
- **WHEN** the user taps "Finish Workout"
- **THEN** `POST /api/workouts/sessions/{id}/finish` is called
- **AND** on success the user is navigated to the workout summary screen (session detail view)
- **AND** the recovery banner is dismissed
