## ADDED Requirements

### Requirement: Deviation summary endpoint
The system SHALL expose a read-only endpoint that compares the plan snapshot against actual session exercises and returns a structured deviation summary.

**Endpoint:** `GET /api/workouts/sessions/{id}/deviations`

**Response shape:**
```json
{
  "data": {
    "sessionId": "<uuid>",
    "supported": true,
    "entries": [
      {
        "status": "COMPLETED | SKIPPED | ADDED",
        "plannedExerciseId": "<uuid | null>",
        "name": "<string>",
        "plannedSets": "<integer | null>",
        "plannedRepsMin": "<integer | null>",
        "plannedRepsMax": "<integer | null>",
        "actualSets": "<integer | null>",
        "sessionExerciseId": "<uuid | null>"
      }
    ]
  },
  "error": null
}
```

#### Scenario: COMPLETED entry for executed plan exercise
- **WHEN** a session exercise has a non-null `planned_exercise_id` matching a snapshot entry
- **THEN** that entry appears in `entries` with `status = "COMPLETED"`, both planned and actual fields populated

#### Scenario: SKIPPED entry for missing plan exercise
- **WHEN** a snapshot entry has no corresponding session exercise (no exercise with matching `planned_exercise_id`)
- **THEN** that entry appears with `status = "SKIPPED"`, `plannedExerciseId` set, `sessionExerciseId = null`, `actualSets = null`

#### Scenario: ADDED entry for unplanned exercise
- **WHEN** a session exercise has `planned_exercise_id = NULL` and the session has a non-null `plan_snapshot`
- **THEN** that exercise appears with `status = "ADDED"`, `plannedExerciseId = null`, planned fields `null`, `sessionExerciseId` set

#### Scenario: Snapshot null — unsupported response
- **WHEN** the session has `plan_snapshot = NULL` (MANUAL session or pre-V21 plan-based session)
- **THEN** response is `{ "data": { "sessionId": "<id>", "supported": false, "entries": [] }, "error": null }` with HTTP 200

#### Scenario: Session not owned by authenticated user
- **WHEN** user requests deviations for a session belonging to another user
- **THEN** system returns 404

#### Scenario: Active session deviations
- **WHEN** the session is still ACTIVE (not yet finished)
- **THEN** the endpoint returns the current deviation state (in-progress) with the same logic as finished sessions

### Requirement: Deviation classification rules
The system SHALL classify each deviation entry using the following deterministic rules based on `planned_exercise_id` linkage and snapshot presence.

| Class | Condition |
|---|---|
| `COMPLETED` | Session exercise has `planned_exercise_id` matching a snapshot entry |
| `SKIPPED` | Snapshot entry has no session exercise with matching `planned_exercise_id` |
| `ADDED` | Session exercise has `planned_exercise_id = NULL` in a plan-based session |

#### Scenario: No false positives when plan_exercise deleted
- **WHEN** `planned_exercise_id` on a session exercise is `NULL` due to cascade (plan deleted), but the snapshot still contains the entry
- **THEN** the snapshot entry is classified as `SKIPPED` and the session exercise (now unlinked) is classified as `ADDED`

#### Scenario: All exercises completed, none skipped or added
- **WHEN** every snapshot entry has a corresponding session exercise and no unplanned exercises exist
- **THEN** all entries are `COMPLETED` and the list length equals the snapshot length

### Requirement: Deviation data exposed in session DTO
The session DTO SHALL include a lightweight deviation indicator so the list view can show a badge without a separate API call.

**Field added to session DTO:** `deviationCount: integer | null`
- `null` if `plan_snapshot` is null (unsupported / not a plan session)
- `0` if all exercises completed as planned
- Positive integer = number of SKIPPED + ADDED entries

#### Scenario: Deviation count computed on session fetch
- **WHEN** a plan-based session with a snapshot is fetched via `GET /api/workouts/{id}` or `GET /api/workouts`
- **THEN** `data.deviationCount` is the count of SKIPPED + ADDED entries

#### Scenario: Non-plan session deviation count is null
- **WHEN** a session has no `plan_snapshot`
- **THEN** `data.deviationCount` is `null`

### Requirement: Active workout UI tags exercise origin
The frontend active workout page SHALL visually distinguish plan-sourced exercises from mid-session additions.

#### Scenario: Plan exercise tagged
- **WHEN** a session exercise has a `plannedExerciseId` in the session DTO
- **THEN** the `ExerciseCard` displays a subtle "From plan" indicator

#### Scenario: Added exercise tagged
- **WHEN** a session exercise has `plannedExerciseId = null` in a plan-based session
- **THEN** the `ExerciseCard` displays a subtle "Added" indicator

### Requirement: Finished session deviation summary UI
The frontend SHALL display a deviation summary on the finished-session / workouts detail screen for plan-based sessions.

#### Scenario: Deviation summary shown after finishing
- **WHEN** the user finishes a plan-based session with at least one deviation
- **THEN** the finished session screen shows a summary section listing skipped and added exercises by name

#### Scenario: No deviation section for non-plan sessions
- **WHEN** `deviationCount` is `null`
- **THEN** no deviation section is rendered
