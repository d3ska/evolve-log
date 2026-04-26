## ADDED Requirements

### Requirement: System exercise catalog is seeded and queryable
The system SHALL maintain a catalog of exercise definitions with name, primary muscle group,
secondary muscle groups, and equipment type. ~80 common exercises SHALL be pre-seeded as
system entries (`is_system = true`) via Flyway migration V14.

#### Scenario: List system exercises
- **WHEN** `GET /api/exercises/definitions` is called (with no query params)
- **THEN** the response is HTTP 200 with a list of all system exercise definitions
- **THEN** each definition includes `id`, `name`, `primaryMuscle`, `secondaryMuscles`, `equipment`, `isSystem`

#### Scenario: Search exercises by name
- **WHEN** `GET /api/exercises/definitions?q=bench` is called
- **THEN** the response includes only definitions whose name contains "bench" (case-insensitive)

#### Scenario: Filter exercises by muscle group
- **WHEN** `GET /api/exercises/definitions?muscle=chest` is called
- **THEN** the response includes only definitions whose `primaryMuscle` equals "chest"

### Requirement: Users can create custom exercise definitions
The system SHALL allow authenticated users to create exercise definitions linked to their
account (`is_system = false`, `user_id = authenticated user`). Custom definitions SHALL
appear alongside system definitions in search results for that user.

#### Scenario: Create custom definition
- **WHEN** `POST /api/exercises/definitions` is called with `{ name, primaryMuscle, equipment }`
- **THEN** the response is HTTP 201 with the created definition
- **THEN** the definition has `isSystem = false` and `userId` set to the caller

#### Scenario: Duplicate system name is rejected
- **WHEN** `POST /api/exercises/definitions` is called with a name that matches an existing
  system definition (case-insensitive)
- **THEN** the response is HTTP 409 Conflict

### Requirement: Exercises can be linked to a definition
The system SHALL allow an exercise row to reference an exercise definition via
`exercise_definition_id` (nullable FK). When a definition is linked, the system SHALL
denormalize `primary_muscle` from the definition onto the exercise row.

#### Scenario: Create exercise with definition link
- **WHEN** a workout exercise is created or updated with a valid `exerciseDefinitionId`
- **THEN** `exercise_definition_id` is set on the exercise row
- **THEN** `primary_muscle` on the exercise row is populated from the definition's `primaryMuscle`

#### Scenario: Create exercise without definition link
- **WHEN** a workout exercise is created without an `exerciseDefinitionId`
- **THEN** the exercise is stored with `exercise_definition_id = NULL` and `primary_muscle = NULL`
- **THEN** the exercise is included in session totals but excluded from muscle-group analytics

### Requirement: Distinct muscle group list is available
The system SHALL expose the list of distinct primary muscle group values present in the
catalog, for use in filter dropdowns.

#### Scenario: Get muscle groups
- **WHEN** `GET /api/exercises/definitions/muscle-groups` is called
- **THEN** the response is HTTP 200 with a sorted list of distinct primary muscle group strings
