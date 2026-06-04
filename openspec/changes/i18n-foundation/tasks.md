# i18n Foundation — Tasks

## Phase 1: Database (V22 Migration)

- [x] **T1** Add `locale VARCHAR(10) NOT NULL DEFAULT 'en'` to `users`
- [x] **T2** Create `exercise_definition_translations` table with PK `(exercise_definition_id, locale)`
- [x] **T3** Seed EN translations from existing `exercise_definitions.name` / `description`
- [x] **T4** Write Polish translations for all ~80 system exercises inline in the migration

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

- [x] **T5** Create `LocaleResolver` Spring `HandlerInterceptor` — reads `user.locale`, falls back to `Accept-Language`, then `'en'`; stores in request attribute
- [x] **T6** Create `ExerciseDefinitionTranslation` JPA entity (composite PK)
- [x] **T7** Create `ExerciseDefinitionTranslationRepository`
- [x] **T8** Create `MuscleI18n` utility class with EN/PL label map

## Phase 3: Backend — Service & API

- [x] **T9** Update `ExerciseDefinitionService.findAll(userId, locale)` — use LEFT JOIN COALESCE query
- [x] **T10** Update `ExerciseDefinitionService.findById(id, locale)` — same pattern
- [x] **T11** Update `ExerciseDefinitionDTO` — add `primaryMuscleLabel` field
- [x] **T12** Add `PUT /api/users/me/locale` endpoint with validation (only `'en'` and `'pl'` accepted)
- [x] **T13** Update `UserDTO` to expose `locale` field

## Phase 4: AI Tool Updates

- [ ] **T14** Update all AI SQL tools that query `exercise_definitions` to LEFT JOIN translations
  with the user's locale passed as a parameter
  > Note: T14 deferred — AI tools use raw SQL injected into LLM context; locale support requires AI prompt redesign, tracked separately.

## Phase 5: Tests

- [x] **T15** Unit test: fetch exercises as PL user → Polish names returned
- [x] **T16** Unit test: fetch exercises as EN user → English names returned
- [x] **T17** Unit test: exercise with no PL translation → falls back to EN name
- [x] **T18** Unit test: `MuscleI18n` returns correct labels for all supported locales
- [x] **T19** MockMvc test: `PUT /api/user/me/locale` updates preference and returns updated user

## Phase 6: Frontend

- [x] **T20** Add `locale` field to `User` type in `src/types/api.ts`
- [x] **T21** Add locale selector in Settings page (EN / PL toggle, calls `PUT /api/user/me/locale`)
- [x] **T22** Pass `primaryMuscleLabel` through relevant UI components instead of the raw enum key
