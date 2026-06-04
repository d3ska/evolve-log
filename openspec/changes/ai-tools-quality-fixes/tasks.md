# Tasks: ai-tools-quality-fixes

## Implementation

- [x] Fix `GetRecentWorkoutsTool.execute()`
  - Check `e.getReps() == null` to decide path
  - Fallback: iterate `e.getWorkoutSets()` ordered by setNumber
  - Render completed sets with data; incomplete sets as "planned (not completed)"
  - Append `[RPE {rpe}]` to exercise header when rpe != null
- [x] Fix `GetMeasurementsTool`
  - Add `from_date` / `to_date` to inputSchema
  - Parse dates with fallback (same pattern as GetNutritionLogTool)
  - Branch on whether from_date present: date-range vs limit-based query
  - Date-range results rendered ASC; limit results remain DESC
  - Header includes count and range when date-range mode

## Tests

- [x] `GetRecentWorkoutsToolTest`
  - Legacy path: exercise has reps+weight → old format
  - Modern path: reps null, workoutSets present → per-set format
  - Mixed session (some exercises with aggregates, some without)
  - Incomplete sets render correctly
  - RPE surfaced when present, absent when null
- [x] `GetMeasurementsToolTest`
  - from_date present → date-range query used, ASC order
  - No dates → limit-based query, DESC order
  - Invalid date string → fallback applied
  - Header format correct for both modes
