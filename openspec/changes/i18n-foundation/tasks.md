# i18n Foundation — Tasks

## Phase 1: Database (V22 Migration)

- [ ] **T1** Add `locale VARCHAR(10) NOT NULL DEFAULT 'en'` to `users`
- [ ] **T2** Create `exercise_definition_translations` table with PK `(exercise_definition_id, locale)`
- [ ] **T3** Seed EN translations from existing `exercise_definitions.name` / `description`
- [ ] **T4** Write Polish translations for all ~80 system exercises inline in the migration

  > Reference list from V14 migration. Cover at minimum:
  > Chest: Bench Press, Incline Bench Press, Dumbbell Flye, Cable Crossover, Push-up
  > Back: Pull-up, Lat Pulldown, Barbell Row, Cable Row, Deadlift, T-Bar Row
  > Shoulders: Overhead Press, Lateral Raise, Front Raise, Face Pull, Arnold Press
  > Biceps: Barbell Curl, Dumbbell Curl, Hammer Curl, Preacher Curl, Cable Curl
  > Triceps: Tricep Pushdown, Skull Crusher, Dips, Overhead Tricep Extension
  > Legs: Squat, Leg Press, Romanian Deadlift, Leg Curl, Leg Extension, Lunges, Bulgarian Split Squat, Hip Thrust
  > Core: Plank, Crunch, Hanging Leg Raise, Russian Twist, Ab Wheel
  > Calves: Standing Calf Raise, Seated Calf Raise
  > Cardio: Running, Cycling, Rowing Machine, Jump Rope

## Phase 2: Backend — Infrastructure

- [ ] **T5** Create `LocaleResolver` Spring `HandlerInterceptor` — reads `user.locale`, falls back to `Accept-Language`, then `'en'`; stores in request attribute
- [ ] **T6** Create `ExerciseDefinitionTranslation` JPA entity (composite PK)
- [ ] **T7** Create `ExerciseDefinitionTranslationRepository`
- [ ] **T8** Create `MuscleI18n` utility class with EN/PL label map

## Phase 3: Backend — Service & API

- [ ] **T9** Update `ExerciseDefinitionService.findAll(userId, locale)` — use LEFT JOIN COALESCE query
- [ ] **T10** Update `ExerciseDefinitionService.findById(id, locale)` — same pattern
- [ ] **T11** Update `ExerciseDefinitionDTO` — add `primaryMuscleLabel` field
- [ ] **T12** Add `PUT /api/users/me/locale` endpoint with validation (only `'en'` and `'pl'` accepted)
- [ ] **T13** Update `UserDTO` to expose `locale` field

## Phase 4: AI Tool Updates

- [ ] **T14** Update all AI SQL tools that query `exercise_definitions` to LEFT JOIN translations
  with the user's locale passed as a parameter

## Phase 5: Tests

- [ ] **T15** Integration test: fetch exercises as PL user → Polish names returned
- [ ] **T16** Integration test: fetch exercises as EN user → English names returned
- [ ] **T17** Integration test: exercise with no PL translation → falls back to EN name
- [ ] **T18** Unit test: `MuscleI18n` returns correct labels for all supported locales
- [ ] **T19** Integration test: `PUT /api/users/me/locale` updates preference; subsequent call returns translated data

## Phase 6: Frontend

- [ ] **T20** Add `locale` field to `User` type in `src/types/api.ts`
- [ ] **T21** Add locale selector in Settings page (EN / PL toggle, calls `PUT /api/users/me/locale`)
- [ ] **T22** Pass `primaryMuscleLabel` through relevant UI components instead of the raw enum key
