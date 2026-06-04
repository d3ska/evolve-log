# Spec: AI Tools Quality Fixes

---

## get_recent_workouts — Per-Set Detail

**Acceptance Criteria:**

1. When `exercise.getReps() != null` → use existing aggregate rendering (backward compat)
2. When `exercise.getReps() == null` and `exercise.getWorkoutSets()` is non-empty →
   render per-set breakdown with `set N: {reps} reps @ {weight} kg`
3. Completed sets (`ws.isCompleted() == true`) rendered with actual reps/weight
4. Incomplete sets rendered as `set N: planned (not completed)`
5. When no aggregate AND no sets → render `{name}: {sets} sets (no rep/weight data)`
6. `exercise.getRpe()` rendered as `[RPE {value}]` appended to the exercise header line
7. RPE omitted when null
8. No new repository queries introduced (EAGER loading already covers this)

---

## get_measurements — Date Range

**Acceptance Criteria:**

1. New params: `from_date` (string, YYYY-MM-DD), `to_date` (string, YYYY-MM-DD)
2. When `from_date` is provided: use `findByUserIdAndDateBetweenOrderByDateAsc`
   - `to_date` defaults to today if omitted
3. When neither date provided: use existing `limit`-based query (default 10, max 50)
4. Date-range results are ordered ASC (oldest → newest) to show trend progression
5. Invalid date strings fall back to defaults (same pattern as GetNutritionLogTool)
6. Result count included in header: `Body measurements (2026-03-01 to 2026-06-01, 12 entries):`
