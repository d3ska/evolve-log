# Proposal: AI Tools Quality Fixes

## Problem 1: get_recent_workouts — Missing Per-Set Detail

Since V17, workouts are tracked at set level in `workout_sets` (individual reps/weight
per set). The `exercises` table still has aggregate `reps` and `weight_kg` columns, but
these are nullable and populated only for pre-V17 data or via `updateAggregates()`.

The current tool reads only the exercise-level fields:
```java
sb.append(": ").append(e.getSets()).append("×");
if (e.getReps() != null) sb.append(e.getReps());
if (e.getWeightKg() != null) sb.append(" @ ").append(e.getWeightKg()).append(" kg");
```

For a modern workout logged via the active workout module (set-by-set completion),
`reps` and `weightKg` are null on the `Exercise` entity. The AI sees:
```
- Bench Press: 5×
```
...with no rep or weight data. Progressive overload analysis from recent sessions
is therefore broken for any user using the set-level tracking feature.

**Fix:** `Exercise.workoutSets` is already eagerly loaded (`FetchType.EAGER` +
`@Fetch(FetchMode.SUBSELECT)`). When aggregate fields are null, render individual
sets from `workoutSets`. Also surface `exercise.rpe` when present.

## Problem 2: get_measurements — No Date Range

The tool only accepts `limit` (last N entries). For trend analysis questions like
"how has my body composition changed in the last 3 months?", the AI must request
up to 50 entries and hope recent ones cover the period. This is wasteful and inflexible.

`MeasurementRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, start, end)`
already exists and is unused by the tool.

**Fix:** Add `from_date`/`to_date` parameters. When both are provided, use the
date-range query. When absent, fall back to the existing limit-based query.
