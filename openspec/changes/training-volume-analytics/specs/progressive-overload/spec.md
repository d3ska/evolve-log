## ADDED Requirements

### Requirement: e1RM history is tracked per exercise definition
The system SHALL compute estimated 1-Rep Maximum (e1RM) for each occurrence of an exercise
using the Epley formula: `e1RM = weightKg × (1 + reps / 30.0)`. e1RM SHALL only be computed
when `reps <= 12`; for higher rep ranges the raw `weightKg` is returned as the performance
indicator instead.

#### Scenario: Get progressive overload history
- **WHEN** `GET /api/analytics/progressive-overload/{exerciseDefinitionId}?sessions=12` is called
- **THEN** the response is HTTP 200 with `exerciseName`, `unit` (`kg`), and a `history` array
- **THEN** `history` contains one entry per session (most recent first, limited to `sessions`
  count) that includes the exercise
- **THEN** each history entry includes `sessionDate`, `sets`, `reps`, `weightKg`, `e1Rm`
  (nullable if reps > 12), `volumeLoad`, `rpe` (nullable), and `isPR`

#### Scenario: Exercise not found or belongs to another user's custom definition
- **WHEN** `{exerciseDefinitionId}` does not exist or is a user-defined definition owned by
  another user
- **THEN** the response is HTTP 404

#### Scenario: No history for exercise
- **WHEN** the authenticated user has never logged the given exercise definition
- **THEN** the response is HTTP 200 with an empty `history` array

### Requirement: Personal records are detected and flagged
The system SHALL mark each history entry as a Personal Record (`isPR = true`) when its e1RM
(or weightKg for reps > 12) is strictly greater than all previous entries in the returned
history for that exercise.

#### Scenario: First logged session is always a PR
- **WHEN** a user's earliest session for an exercise is returned
- **THEN** `isPR = true` for that entry

#### Scenario: Session matches but does not exceed prior best
- **WHEN** a session's e1RM equals the prior best but does not exceed it
- **THEN** `isPR = false` for that entry

#### Scenario: Multiple sessions in the same day
- **WHEN** a user logged the same exercise in two sessions on the same date
- **THEN** both sessions appear as separate entries in the history
- **THEN** the higher e1RM of the two is used for PR comparison against prior dates

### Requirement: Volume increment is shown between consecutive sessions
The system SHALL include `volumeDelta` in each history entry — the difference in Volume Load
versus the immediately preceding session for the same exercise. The oldest entry in the
history window has `volumeDelta = null`.

#### Scenario: Volume increased vs. prior session
- **WHEN** current session's volumeLoad exceeds the prior session's volumeLoad for that exercise
- **THEN** `volumeDelta` is a positive number

#### Scenario: Volume decreased vs. prior session
- **WHEN** current session's volumeLoad is less than the prior session's
- **THEN** `volumeDelta` is a negative number

### Requirement: Existing exercises can be auto-linked to definitions by name
The system SHALL provide an endpoint to backfill `exercise_definition_id` on existing exercise
rows by matching their `name` (case-insensitive) to system exercise definitions. This is
user-triggered, not automatic.

#### Scenario: Auto-link matches exercises
- **WHEN** `POST /api/exercises/auto-link` is called
- **THEN** each exercise row whose `name` matches a system definition name (case-insensitive)
  and whose `exercise_definition_id` is currently NULL has its FK and `primary_muscle` updated
- **THEN** the response is HTTP 200 with `{ matched: N, skipped: M }` counts

#### Scenario: Auto-link is idempotent
- **WHEN** `POST /api/exercises/auto-link` is called twice
- **THEN** the second call changes no rows and returns `{ matched: 0, skipped: M }`
