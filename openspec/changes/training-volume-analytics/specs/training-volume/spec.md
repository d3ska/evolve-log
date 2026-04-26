## ADDED Requirements

### Requirement: Volume Load is calculated per session
The system SHALL compute Volume Load for a workout session as the sum of
`sets × reps × weightKg` across all exercises in that session. Exercises with
`weightKg = NULL` (bodyweight) SHALL be excluded from the volume sum.

#### Scenario: Get session volume summary
- **WHEN** `GET /api/analytics/sessions/{sessionId}/volume` is called by the session owner
- **THEN** the response is HTTP 200 with `totalVolumeLoad` (kg), `exerciseCount`, and a
  `byMuscleGroup` array
- **THEN** `byMuscleGroup` contains one entry per distinct `primary_muscle` present in that
  session, each with `muscle`, `volumeLoad`, and `exerciseCount`
- **THEN** exercises with no definition link are counted in `exerciseCount` but not included
  in `byMuscleGroup`

#### Scenario: Session with no weighted exercises
- **WHEN** all exercises in the session have `weightKg = NULL`
- **THEN** `totalVolumeLoad` is `0` and `byMuscleGroup` is an empty array

#### Scenario: Session belongs to another user
- **WHEN** `GET /api/analytics/sessions/{sessionId}/volume` is called by a user who does not
  own that session
- **THEN** the response is HTTP 404

### Requirement: Internal Load is calculated when RPE is present
The system SHALL compute Internal Load for an exercise as `sets × reps × weightKg × rpe`.
Session Internal Load is the sum of Internal Load across all exercises that have both
`weightKg` and `rpe` set.

#### Scenario: Session with RPE on all exercises
- **WHEN** all exercises in the session have both `weightKg` and `rpe` set
- **THEN** `totalInternalLoad` is returned in the session volume summary
- **THEN** each `byMuscleGroup` entry includes `internalLoad`

#### Scenario: Session with partial RPE data
- **WHEN** some exercises have `rpe` and others do not
- **THEN** `totalInternalLoad` is the sum of Internal Load for exercises that have RPE
- **THEN** the response includes `rpeCompleteness` as a fraction (0.0–1.0) indicating what
  proportion of exercises have RPE recorded

### Requirement: Weekly volume is aggregated per muscle group
The system SHALL aggregate Volume Load by ISO week and primary muscle group for a given
date range, enabling trend analysis.

#### Scenario: Get weekly volume by muscle group
- **WHEN** `GET /api/analytics/volume/weekly?from=2025-01-01&to=2025-04-01` is called
- **THEN** the response is HTTP 200 with a list of `{ weekStart, muscle, volumeLoad, sessionCount }`
- **THEN** results are ordered by `weekStart ASC`, then `muscle ASC`
- **THEN** weeks with no training data for a muscle are omitted (no zero-fill)

#### Scenario: Filter weekly volume by muscle group
- **WHEN** `GET /api/analytics/volume/weekly?from=2025-01-01&to=2025-04-01&muscle=chest` is called
- **THEN** only rows where `muscle = 'chest'` are returned

#### Scenario: Date range exceeds 52 weeks
- **WHEN** the `from`–`to` range exceeds 52 weeks
- **THEN** the response is HTTP 400 with an error message indicating the maximum range
