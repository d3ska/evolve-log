# Spec: New AI Data Source Tools

---

## Tool: get_supplement_info

**Name:** `get_supplement_info`

**Description:**
Returns the athlete's supplement plans with all entries (supplement name, dose,
timing, notes). Use this when asked about supplement protocol, nutrient timing,
recovery support, or when cross-referencing diet and supplementation.

**Input:**
| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `active_only` | boolean | No | `true` | Filter to active plans only |

**Acceptance Criteria:**
1. Returns all plans for the user, filtered by `isActive` when `active_only=true`
2. Entries are rendered in `sortOrder ASC`
3. `doseAmount + doseUnit` rendered together; omitted if both null
4. `timeSlot == CUSTOM` uses `customTime` value in place of slot name
5. Inactive plans shown with `[inactive]` label when `active_only=false`
6. No plans → `"No supplement plans found."`
7. No active plans but inactive exist → helpful message suggesting `active_only: false`

---

## Tool: get_health_metrics

**Name:** `get_health_metrics`

**Description:**
Returns device-measured health metrics from Withings (body composition, heart rate,
VO2max, basal metabolic rate, visceral fat, etc.) for a given date range. Use this
for body composition trends, calorie target estimation via BMR, cardiovascular fitness
tracking, or recovery monitoring.

**Input:**
| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `from_date` | string (YYYY-MM-DD) | No | 30 days ago | Range start |
| `to_date` | string (YYYY-MM-DD) | No | today | Range end |
| `metric_keys` | string[] | No | all | Filter to specific metric keys |

**Acceptance Criteria:**
1. Delegates to `HealthMetricService.getDailyMetrics(userId, "withings", from, to)`
2. Date defaults: `to = today`, `from = today - 30 days`
3. Invalid date strings silently fall back to defaults
4. When `metric_keys` provided, only those keys are rendered per day
5. Sparse days (missing keys) render only present metrics — no null/zero padding
6. No data → `"No Withings health metrics found for {from} to {to}."`
7. Keys are rendered with human-readable labels where known:
   - `basal_metabolic_rate` → `bmr`
   - `body_fat_percent` → `body_fat`
   - `heart_pulse_bpm` → `heart_pulse`
   - `vo2_max` → `vo2_max`
   - All others → key as-is
8. `PromptContextBuilder` includes Withings hint when recent data exists
