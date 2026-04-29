# Manual Workout Analytics Fix — Design

## Fix 1 — Inline definition resolution in WorkoutService

Add a private helper `resolveDefinitionId(String name, UUID explicitId)` to `WorkoutService`:

```java
private UUID resolveDefinitionId(String name, UUID explicitId) {
    if (explicitId != null) return explicitId;
    if (name == null) return null;
    return definitionRepository.findByNameIgnoreCaseAndIsSystemTrue(name)
            .map(ExerciseDefinition::getId)
            .orElse(null);
}
```

`ExerciseDefinitionRepository.findByNameIgnoreCaseAndIsSystemTrue(String name)` already exists
(added in `training-volume-analytics`).

Update `buildExercises()` — replace:
```java
.exerciseDefinitionId(req.exerciseDefinitionId())
.primaryMuscle(primaryMuscle)
```
with:
```java
UUID resolvedDefId = resolveDefinitionId(req.name(), req.exerciseDefinitionId());
String resolvedMuscle = resolvedDefId != null ? resolvePrimaryMuscle(resolvedDefId) : null;
// ...
.exerciseDefinitionId(resolvedDefId)
.primaryMuscle(resolvedMuscle)
```

Apply the same change in `addExercise()`, replacing the existing `resolvePrimaryMuscle` call
with `resolveDefinitionId` + `resolvePrimaryMuscle`.

**Why inline rather than post-save bulk**: a targeted lookup per exercise avoids issuing a
full-table bulk UPDATE after every save and keeps the relationship explicit at insert time.

---

## Fix 2 — ProgressiveOverloadService fallback

### `effectiveReps(Exercise e)`

Current:
```java
private static Integer effectiveReps(Exercise e) {
    return e.getWorkoutSets().stream()
            .filter(ws -> ws.getReps() != null && ws.getWeightKg() != null)
            .max(Comparator.comparing(WorkoutSet::getWeightKg))
            .map(WorkoutSet::getReps)
            .orElse(null);
}
```

New:
```java
private static Integer effectiveReps(Exercise e) {
    return e.getWorkoutSets().stream()
            .filter(ws -> ws.getReps() != null && ws.getWeightKg() != null)
            .max(Comparator.comparing(WorkoutSet::getWeightKg))
            .map(WorkoutSet::getReps)
            .orElse(e.getReps());   // fall back to exercise-level field
}
```

### `effectiveWeight(Exercise e)`

Current:
```java
private static BigDecimal effectiveWeight(Exercise e) {
    return e.getWorkoutSets().stream()
            .map(WorkoutSet::getWeightKg)
            .filter(Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(null);
}
```

New:
```java
private static BigDecimal effectiveWeight(Exercise e) {
    return e.getWorkoutSets().stream()
            .map(WorkoutSet::getWeightKg)
            .filter(Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(e.getWeightKg());  // fall back to exercise-level field
}
```

---

## Fix 3 — Weekly volume native query fallback

### Current query (problem areas highlighted)

```sql
SUM(
    (SELECT SUM(s.reps * s.weight_kg)
     FROM workout_sets s
     WHERE s.exercise_id = e.id
       AND s.reps IS NOT NULL AND s.weight_kg IS NOT NULL)
)  AS volume_load,
...
AND EXISTS (
    SELECT 1 FROM workout_sets s
    WHERE s.exercise_id = e.id
      AND s.reps IS NOT NULL AND s.weight_kg IS NOT NULL
)
```

### Replacement

```sql
SUM(
    COALESCE(
        (SELECT SUM(s.reps * s.weight_kg)
         FROM workout_sets s
         WHERE s.exercise_id = e.id
           AND s.reps IS NOT NULL AND s.weight_kg IS NOT NULL),
        CASE
            WHEN e.sets IS NOT NULL AND e.reps IS NOT NULL AND e.weight_kg IS NOT NULL
            THEN (e.sets * e.reps * e.weight_kg)
        END
    )
)  AS volume_load,
...
AND (
    EXISTS (
        SELECT 1 FROM workout_sets s
        WHERE s.exercise_id = e.id
          AND s.reps IS NOT NULL AND s.weight_kg IS NOT NULL
    )
    OR (e.reps IS NOT NULL AND e.weight_kg IS NOT NULL AND e.sets IS NOT NULL)
)
```

The `COALESCE` prefers per-set volume (plan-based / active workout path) and falls back to
`sets × reps × weight_kg` from the exercise row (manual log path). The new OR condition
removes the hard exclusion of exercise-level-only entries.

---

## Backfill Note

Existing manually logged exercises in production have `exercise_definition_id = NULL` and
`primary_muscle = NULL`. The weekly volume query filters `AND e.primary_muscle IS NOT NULL`,
so they remain excluded from the volume chart even after Fix 3 until they are linked.

Required one-time action for production:
```
POST /api/exercises/auto-link
```
This triggers `ExerciseAutoLinkService.autoLink(userId)` which runs:
```sql
UPDATE exercises e
SET exercise_definition_id = d.id,
    primary_muscle         = d.primary_muscle
FROM exercise_definitions d
WHERE LOWER(e.name) = LOWER(d.name)
  AND d.is_system = true
  AND e.exercise_definition_id IS NULL
  AND e.workout_session_id IN (SELECT id FROM workout_sessions WHERE user_id = :userId)
```

No migration needed — this is a data repair, not a schema change.

---

## Data Flow After Fix

| Workout type | exercise_definition_id | workout_sets | Progressive overload | Weekly volume |
|---|---|---|---|---|
| Plan-based (existing) | set by startFromPlan | has rows | ✓ works | ✓ works |
| Manual new (after fix) | resolved by name inline | empty | ✓ works via fallback | ✓ works via fallback |
| Manual historical (after auto-link) | set by auto-link | empty | ✓ works via fallback | ✓ works via fallback |
| Manual historical (no name match) | still null | empty | still invisible | still invisible |
