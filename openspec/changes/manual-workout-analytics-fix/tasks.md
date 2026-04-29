# Manual Workout Analytics Fix — Tasks

## Phase 1: WorkoutService — Inline Definition Resolution

- [x] **T1** Add private `resolveDefinitionId(String name, UUID explicitId)` helper to `WorkoutService`:
  - If `explicitId != null` → return `explicitId`
  - Else if `name != null` → call `definitionRepository.findByNameIgnoreCaseAndIsSystemTrue(name)`, return its `id` or `null`
  - Else → return `null`

- [x] **T2** Update `WorkoutService.buildExercises()`: replace the direct `req.exerciseDefinitionId()` and `resolvePrimaryMuscle(req.exerciseDefinitionId())` calls with `resolveDefinitionId` + `resolvePrimaryMuscle(resolvedDefId)`

- [x] **T3** Update `WorkoutService.addExercise()`: same substitution as T2

## Phase 2: ProgressiveOverloadService — Fallback to Exercise-Level Fields

- [x] **T4** In `ProgressiveOverloadService.effectiveReps(Exercise e)`: change `.orElse(null)` to `.orElse(e.getReps())`

- [x] **T5** In `ProgressiveOverloadService.effectiveWeight(Exercise e)`: change `.orElse(null)` to `.orElse(e.getWeightKg())`

## Phase 3: Weekly Volume Query — Fallback Logic

- [x] **T6** Update `ExerciseRepository.findWeeklyVolumeByMuscle` native query:
  - Wrap the `workout_sets` SUM subquery in `COALESCE(..., CASE WHEN e.sets IS NOT NULL AND e.reps IS NOT NULL AND e.weight_kg IS NOT NULL THEN (e.sets * e.reps * e.weight_kg) END)`
  - Replace the hard `AND EXISTS (SELECT 1 FROM workout_sets ...)` filter with an OR that also accepts `(e.reps IS NOT NULL AND e.weight_kg IS NOT NULL AND e.sets IS NOT NULL)`

## Phase 4: Tests

- [x] **T7** Integration test — `WorkoutService.create()` with exercise name matching a system definition and no `exerciseDefinitionId`:
  - Saved exercise has `exerciseDefinitionId` populated
  - Saved exercise has `primaryMuscle` populated

- [x] **T8** Integration test — `WorkoutService.create()` with unrecognised exercise name:
  - Saved exercise has `exerciseDefinitionId = null`
  - No error thrown

- [x] **T9** Unit test — `ProgressiveOverloadService`: exercise with exercise-level `reps = 8`, `weightKg = 80`, zero workout_sets → history entry has non-null `e1Rm` and `volumeLoad`

- [x] **T10** Unit test — `ProgressiveOverloadService`: exercise with both exercise-level fields and workout_sets → workout_sets data wins

- [x] **T11** Integration test — `TrainingVolumeService.getWeeklyVolumeByMuscle()` with a manual exercise (exercise-level fields, no workout_sets) → correct `volume_load` returned

## Phase 5: Backfill (Operational)

- [x] **T12** Document in runbook / ops notes: after deploy, call `POST /api/exercises/auto-link`
  for each existing user to retroactively populate `exercise_definition_id` and `primary_muscle`
  on historical manual exercises. No migration required.
  > **Ops note**: After deploying this change, call `POST /api/exercises/auto-link` while
  > authenticated as each affected user (or add a one-time admin trigger) to run
  > `ExerciseRepository.bulkAutoLinkByUserId()`. This backfills `exercise_definition_id` and
  > `primary_muscle` on all historical manual exercises that have a name matching a system
  > definition. Safe to run multiple times — only updates rows where `exercise_definition_id IS NULL`.
