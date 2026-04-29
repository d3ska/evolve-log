# Spec: manual-workout-analytics

### Requirement: Manually logged exercises are resolved to a definition at save time

#### Scenario: Exercise name matches a system definition
- **GIVEN** a user logs a workout manually with exercise name "Bench Press" and no `exerciseDefinitionId`
- **WHEN** `WorkoutService.create()` saves the session
- **THEN** the saved `Exercise` row has `exercise_definition_id` set to the matching system definition
- **AND** `primary_muscle` is populated from that definition

#### Scenario: Exercise name does not match any system definition
- **GIVEN** a user logs a workout with exercise name "My Custom Move" and no `exerciseDefinitionId`
- **WHEN** `WorkoutService.create()` saves the session
- **THEN** the saved `Exercise` row has `exercise_definition_id = NULL`
- **AND** `primary_muscle = NULL`
- **AND** no error is thrown

#### Scenario: Explicit exerciseDefinitionId is always used as-is
- **GIVEN** a create request with both `name` and `exerciseDefinitionId` provided
- **WHEN** `WorkoutService.addExercise()` saves the exercise
- **THEN** the provided `exerciseDefinitionId` is used without a name lookup

---

### Requirement: Progressive overload chart shows data for manually logged exercises

#### Scenario: Manual exercise with exercise-level reps/weight, no workout_sets
- **GIVEN** an exercise linked to a definition with `reps = 8`, `weightKg = 80`, `sets = 3` on the Exercise row
- **AND** the exercise has zero WorkoutSet rows
- **WHEN** `ProgressiveOverloadService.getProgressiveOverload()` is called for that definition
- **THEN** the history entry has `reps = 8`, `weightKg = 80`, `sets = 3`
- **AND** `e1Rm` is computed (non-null)
- **AND** `volumeLoad` is computed (non-null)

#### Scenario: Plan-based exercise with workout_sets takes priority over exercise-level fields
- **GIVEN** an exercise with `reps = 5` (exercise-level) and a WorkoutSet with `reps = 10, weightKg = 100`
- **WHEN** `ProgressiveOverloadService` builds the history entry
- **THEN** `reps = 10` and `weightKg = 100` are used (workout_sets data wins)

#### Scenario: Exercise with no reps/weight anywhere
- **GIVEN** an exercise with `reps = NULL` at both exercise-level and in all workout_sets
- **WHEN** `ProgressiveOverloadService` builds the history entry
- **THEN** `reps = NULL`, `e1Rm = NULL`, `volumeLoad = NULL`
- **AND** no error is thrown

---

### Requirement: Weekly volume chart includes manually logged exercises

#### Scenario: Manual exercise with exercise-level sets/reps/weight, no workout_sets
- **GIVEN** an exercise with `primary_muscle = 'chest'`, `sets = 3`, `reps = 8`, `weight_kg = 80`
- **AND** the exercise has zero workout_sets rows
- **WHEN** `TrainingVolumeService.getWeeklyVolumeByMuscle()` is called for the relevant week
- **THEN** the 'chest' entry has `volume_load = 3 × 8 × 80 = 1920`
- **AND** the exercise is counted in `session_count`

#### Scenario: Plan-based exercise with workout_sets takes priority
- **GIVEN** an exercise with `sets = 3`, `reps = 5`, `weight_kg = 60` (exercise-level)
- **AND** the exercise has 3 WorkoutSet rows each with `reps = 10, weight_kg = 100`
- **WHEN** `TrainingVolumeService.getWeeklyVolumeByMuscle()` is called
- **THEN** `volume_load = 10 × 100 × 3 = 3000` (from workout_sets, not exercise-level fields)

#### Scenario: Exercise with no reps/weight anywhere is excluded from volume
- **GIVEN** an exercise with `primary_muscle = 'back'` and all reps/weight null
- **WHEN** `TrainingVolumeService.getWeeklyVolumeByMuscle()` is called
- **THEN** the exercise does not appear in results (cannot compute volume)
