# i18n Foundation — Design

## V22 Migration

```sql
-- User locale preference
ALTER TABLE users
    ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'en';

-- Exercise translation table
CREATE TABLE exercise_definition_translations (
    exercise_definition_id UUID        NOT NULL REFERENCES exercise_definitions(id) ON DELETE CASCADE,
    locale                 VARCHAR(10) NOT NULL,
    name                   VARCHAR(300) NOT NULL,
    description            TEXT,
    PRIMARY KEY (exercise_definition_id, locale)
);

CREATE INDEX idx_exercise_def_translations_locale
    ON exercise_definition_translations (locale);

-- Seed EN translations from the existing name/description columns
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name, description)
SELECT id, 'en', name, description
FROM exercise_definitions
WHERE is_system = true;

-- PL translations are seeded inline in this migration (see seed data section below)
-- User-created exercises do NOT get system translations; users manage their own names.
```

## Locale Resolution

Priority order (highest to lowest):
1. `users.locale` for the authenticated user
2. `Accept-Language` request header (fallback for unauthenticated requests)
3. Hard default: `'en'`

A `LocaleContextHolder` (Spring request-scoped bean) populated in a `HandlerInterceptor`
provides the resolved locale to service methods.

## ExerciseDefinitionService — Translation Lookup

```
1. Resolve locale for current user
2. SELECT t.name, t.description
   FROM exercise_definition_translations t
   WHERE t.exercise_definition_id = :id AND t.locale = :locale
3. If no row found (translation missing), fall back to exercise_definitions.name / description (EN)
```

For list queries (all exercises), use a single LEFT JOIN rather than N+1:
```sql
SELECT d.id, d.primary_muscle, d.is_system,
       COALESCE(t.name, d.name)               AS name,
       COALESCE(t.description, d.description) AS description
FROM exercise_definitions d
LEFT JOIN exercise_definition_translations t
       ON t.exercise_definition_id = d.id AND t.locale = :locale
WHERE d.is_system = true OR d.user_id = :userId
ORDER BY name
```

## Muscle Group Display Names

Muscle group names (`primary_muscle` enum values) are translated in a static backend map
rather than a DB table — there are ~15 values and they never change:

```java
// MuscleI18n.java
Map<String, Map<String, String>> NAMES = Map.of(
    "CHEST",     Map.of("en", "Chest",     "pl", "Klatka piersiowa"),
    "BACK",      Map.of("en", "Back",      "pl", "Plecy"),
    "SHOULDERS", Map.of("en", "Shoulders", "pl", "Barki"),
    "BICEPS",    Map.of("en", "Biceps",    "pl", "Biceps"),
    "TRICEPS",   Map.of("en", "Triceps",   "pl", "Triceps"),
    "LEGS",      Map.of("en", "Legs",      "pl", "Nogi"),
    "GLUTES",    Map.of("en", "Glutes",    "pl", "Pośladki"),
    "CORE",      Map.of("en", "Core",      "pl", "Brzuch / Core"),
    "CALVES",    Map.of("en", "Calves",    "pl", "Łydki"),
    "FOREARMS",  Map.of("en", "Forearms",  "pl", "Przedramiona"),
    "CARDIO",    Map.of("en", "Cardio",    "pl", "Cardio"),
    "FULL_BODY", Map.of("en", "Full Body", "pl", "Całe ciało")
);
```

DTOs expose `primaryMuscleLabel` as the resolved string alongside the enum key.

## API Contract

No breaking changes. `ExerciseDefinitionDTO` already has `name` and `description` fields.
After this change they are locale-resolved. The raw enum key `primaryMuscle` remains; a new
`primaryMuscleLabel` field is added.

New endpoint (optional but useful):
- `PUT /api/users/me/locale` — update locale preference (`{ "locale": "pl" }`)

## User-Created Exercises

User-created exercises (`is_system = false`) are never translated. The name the user typed
is used as-is regardless of locale. No translation rows are inserted for them.

## AI Agent Impact

All AI tool queries that join exercise definitions must add:
```sql
LEFT JOIN exercise_definition_translations t
       ON t.exercise_definition_id = d.id AND t.locale = :userLocale
```
The AI tools receive `userLocale` from the current user's profile. This ensures the agent
responds in the user's language when naming exercises.
