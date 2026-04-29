# Spec: active-workout-persistence

## Requirements

### Requirement: Session status lifecycle

The system SHALL track each workout session's lifecycle via a `status` column.

#### Scenario: New session via start endpoint
- **WHEN** `POST /api/workouts/sessions/start` is called
- **THEN** the created session has `status = 'ACTIVE'`

#### Scenario: Manual session creation
- **WHEN** `POST /api/workouts/sessions` is called (retrospective entry)
- **THEN** the created session has `status = 'MANUAL'`

#### Scenario: Finish transitions ACTIVE to FINISHED
- **WHEN** `POST /api/workouts/sessions/{id}/finish` is called on an `ACTIVE` session
- **THEN** `status` transitions to `'FINISHED'` and `finishedAt` is set

#### Scenario: Finish is idempotent on FINISHED session
- **WHEN** `POST /api/workouts/sessions/{id}/finish` is called on a `FINISHED` session
- **THEN** system returns 200 with the existing session data, no fields modified

#### Scenario: Only one ACTIVE session allowed per user
- **WHEN** a user already has a session with `status = 'ACTIVE'`
- **AND** they call `POST /api/workouts/sessions/start` again
- **THEN** system returns 409 Conflict with error: "A workout is already in progress"

---

### Requirement: Retrieve active session

The system SHALL expose the in-progress session for the authenticated user.

#### Scenario: User has an active session
- **WHEN** `GET /api/workouts/sessions/active` is called and the user has a session with `status = 'ACTIVE'`
- **THEN** system returns 200 with the full session DTO (including all exercises and sets)

#### Scenario: User has no active session
- **WHEN** `GET /api/workouts/sessions/active` is called and no `ACTIVE` session exists
- **THEN** system returns 200 with `{ "data": null, "error": null }`

---

### Requirement: Add exercise to session

The system SHALL allow appending exercises to any session regardless of status.

#### Scenario: Add exercise to any session
- **WHEN** `POST /api/workouts/exercises` is called with a valid `sessionId` and `name`
- **AND** the session belongs to the authenticated user
- **THEN** system creates an Exercise row linked to the session and returns 201 with the Exercise DTO

#### Scenario: Session not owned by user
- **WHEN** `POST /api/workouts/exercises` is called with a sessionId belonging to another user
- **THEN** system returns 404

---

### Requirement: Remove exercise from session

The system SHALL allow removing an exercise and all its sets from any session.

#### Scenario: Remove exercise
- **WHEN** `DELETE /api/workouts/exercises/{exerciseId}` is called
- **AND** the exercise belongs to a session owned by the user
- **THEN** system deletes the exercise and all cascade-deleted sets, returns 204

---

### Requirement: Add set

The system SHALL allow adding a single set to an exercise in any session.

#### Scenario: Add set
- **WHEN** `POST /api/workouts/sets` is called with `exerciseId`, `setNumber`, and optional `reps`/`weightKg`
- **AND** the exercise's session belongs to the user
- **THEN** system creates the set and returns 201 with WorkoutSet DTO

#### Scenario: Duplicate set number
- **WHEN** `POST /api/workouts/sets` is called with a `setNumber` already present for that exercise
- **THEN** system returns 409 with error: "Set number already exists for this exercise"

---

### Requirement: Atomic set update

The system SHALL allow partial in-place update of a single set.

#### Scenario: Update reps and weight
- **WHEN** `PATCH /api/workouts/sets/{setId}` is called with `{ "reps": 10, "weightKg": 80.0 }`
- **AND** the set belongs to the authenticated user's session
- **THEN** system updates only those fields and returns 200 with the updated WorkoutSet DTO

#### Scenario: Mark set as completed
- **WHEN** `PATCH /api/workouts/sets/{setId}` is called with `{ "completed": true }`
- **THEN** system sets `completed = true` and returns 200

#### Scenario: Update set belonging to another user
- **WHEN** the set's exercise's session belongs to a different user
- **THEN** system returns 404

---

### Requirement: Delete set

The system SHALL allow removing a single set from any session.

#### Scenario: Delete set
- **WHEN** `DELETE /api/workouts/sets/{setId}` is called and the set belongs to the user's session
- **THEN** system deletes the set and returns 204

---

### Requirement: WorkoutSet DTO includes completed flag

#### Scenario: Set DTO shape
- **WHEN** any endpoint returns a WorkoutSet
- **THEN** the DTO includes `id`, `exerciseId`, `setNumber`, `reps`, `weightKg`, `completed`

---

### Requirement: WorkoutSession DTO includes status

#### Scenario: Session DTO shape
- **WHEN** any endpoint returns a WorkoutSession
- **THEN** the DTO includes the `status` field (`"ACTIVE"`, `"FINISHED"`, or `"MANUAL"`)
