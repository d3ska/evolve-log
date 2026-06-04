# Tasks: ai-tools-new-data-sources

## Implementation

- [x] Create `GetSupplementInfoTool.java`
  - Inject `SupplementPlanRepository`
  - Input: `active_only` boolean (default true)
  - Filter on `plan.isActive()`; label inactive plans with `[inactive]`
  - Render entries in sortOrder: name, dose, timeSlot (or customTime), notes
- [x] Create `GetHealthMetricsTool.java`
  - Inject `HealthMetricService`
  - Input: `from_date`, `to_date` (defaults: today-30, today), `metric_keys` array
  - Call `getDailyMetrics(userId, "withings", from, to)`
  - Optionally filter to requested keys
  - Render per-day, skip absent metrics
- [x] Update `PromptContextBuilder.buildContext()` with:
  - Active supplement plan count hint
  - Withings data availability hint (last sync date)

## Tests

- [x] `GetSupplementInfoToolTest` — unit test
  - active plan filters inactive; inactive_only=false shows all
  - CUSTOM timeSlot uses customTime value
  - null dose fields omitted
- [x] `GetHealthMetricsToolTest` — unit test
  - date defaults applied when params missing
  - metric_keys filter applied
  - empty result returns correct message
