# Manual Workout Analytics Fix — Proposal

## Why

Workouts logged via the "Log Workout" button (manual entry, no predefined plan) are invisible
in the progressive overload chart and produce zero values in the weekly volume chart. Workouts
started from a predefined plan display correctly. This inconsistency makes the analytics
features useless for users who prefer to log freely.

Two root causes were identified by reading `WorkoutService`, `ProgressiveOverloadService`,
and `ExerciseRepository`:

### Root Cause 1 — Missing `exercise_definition_id` on manual exercises

The progressive overload query filters strictly on `exercise_definition_id`:

```java
exerciseRepository.findByUserIdAndDefinitionId(userId, definitionId, pageable)
```

Plan-based flow: `startFromPlan()` copies `exerciseDefinitionId` from the `PlannedExercise` row
→ always populated → query finds data.

Manual flow: `WorkoutService.buildExercises()` and `addExercise()` set `exerciseDefinitionId`
only when the frontend explicitly sends it. The manual log form does not send it.
Result: `exercise_definition_id = NULL` → the exercise is invisible to every analytics query.

`ExerciseAutoLinkService.autoLink()` can fix this retroactively via a bulk name-match UPDATE,
but it is only callable via an explicit `POST /api/exercises/auto-link` endpoint — it is never
invoked automatically after a save.

### Root Cause 2 — Exercise-level reps/weight ignored by analytics

Both `ProgressiveOverloadService` and the weekly volume native query source their data
**exclusively from `workout_sets` rows**:

```java
e.getWorkoutSets().stream()
    .filter(ws -> ws.getReps() != null && ws.getWeightKg() != null)
    ...
    .orElse(null);
```

Manual workouts store reps and weight directly on the `Exercise` row (`exercise.reps`,
`exercise.weightKg`) — no `workout_sets` rows are created. So even if auto-link were
called, the values would still read as null and the chart would show nothing.

The weekly volume native query compounds this with:
```sql
AND EXISTS (SELECT 1 FROM workout_sets s WHERE s.exercise_id = e.id ...)
```
This hard filter **excludes all manually logged exercises entirely** from the volume aggregation.

## What Changes

1. **Inline name-based auto-link on save**: when `exerciseDefinitionId` is absent from a
   `CreateExerciseRequest`, attempt to resolve it by matching `exercise.name` against system
   definitions (case-insensitive). Set `exerciseDefinitionId` and `primaryMuscle` when found.
   Applied in both `buildExercises()` and `addExercise()`.

2. **`ProgressiveOverloadService` fallback**: `effectiveReps()` and `effectiveWeight()` fall
   back to `exercise.reps` / `exercise.weightKg` when no `workout_sets` rows carry data.

3. **Weekly volume query fallback**: replace the hard `AND EXISTS (workout_sets ...)` filter
   with an OR that also accepts exercises with exercise-level reps/weight. Use `COALESCE` to
   prefer workout_sets volume but fall back to `sets × reps × weight_kg` from the exercise row.

4. **Backfill**: document that `POST /api/exercises/auto-link` must be called once for existing
   users to retroactively link historical manual exercises. No new migration required.

## Impact

- `WorkoutService` — `buildExercises()` and `addExercise()`: add inline definition lookup
- `ProgressiveOverloadService` — `effectiveReps()`, `effectiveWeight()`: add fallback
- `ExerciseRepository` — `findWeeklyVolumeByMuscle` native query: fallback logic
- No schema changes, no new migrations
