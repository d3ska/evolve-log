# Spec: translation-tables

### Requirement: Exercise names resolve to user's locale

#### Scenario: User with locale 'pl' fetches exercise list
- **WHEN** `GET /api/exercises/definitions` is called by a user with `locale = 'pl'`
- **THEN** each exercise's `name` and `description` are the Polish translations
- **AND** exercises without a PL translation fall back to the English name silently

#### Scenario: User with locale 'en' fetches exercise list
- **WHEN** `GET /api/exercises/definitions` is called by a user with `locale = 'en'`
- **THEN** each exercise's `name` and `description` are the English values

#### Scenario: User-created exercise in list
- **WHEN** the exercise list includes a user-created exercise (`is_system = false`)
- **THEN** the name is returned as the user originally entered it, regardless of locale

#### Scenario: Locale fallback when translation missing
- **WHEN** a translation row for the requested locale does not exist
- **THEN** the base `exercise_definitions.name` (EN) is returned
- **AND** no error is returned

### Requirement: Muscle group labels are locale-resolved in DTOs

#### Scenario: ExerciseDefinitionDTO includes muscle label
- **WHEN** an exercise definition DTO is returned
- **THEN** the DTO includes `primaryMuscle` (enum key, e.g. `"CHEST"`) AND `primaryMuscleLabel` (locale-resolved string, e.g. `"Klatka piersiowa"` for PL)

### Requirement: User can update locale preference

#### Scenario: Update locale to 'pl'
- **WHEN** `PUT /api/users/me/locale` is called with `{ "locale": "pl" }`
- **THEN** `users.locale` is updated and subsequent API responses use Polish translations

#### Scenario: Invalid locale rejected
- **WHEN** `PUT /api/users/me/locale` is called with an unsupported locale (e.g. `"de"`)
- **THEN** system returns 400 with error: "Unsupported locale. Supported: en, pl"

### Requirement: All ~80 system exercises have Polish translations in seed data

#### Scenario: PL translation coverage
- **WHEN** `SELECT COUNT(*) FROM exercise_definition_translations WHERE locale = 'pl'` is run after V22
- **THEN** the count equals the number of system exercises (`SELECT COUNT(*) FROM exercise_definitions WHERE is_system = true`)
