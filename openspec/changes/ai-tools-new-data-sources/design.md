# Design: New AI Data Source Tools

---

## Tool 1: get_supplement_info

### Data Flow

```
Claude → tool_use: get_supplement_info { active_only: true }
             ↓
GetSupplementInfoTool.execute()
             ↓
SupplementPlanRepository.findByUserIdOrderByCreatedAtDesc(userId)
  [EntityGraph: entries, entries.supplement — no extra queries]
             ↓
Filter on plan.isActive when active_only=true
             ↓
Format as markdown text
```

### Entity Mapping

```
SupplementPlan
  ├── name        → plan header
  ├── description → subtitle if present
  ├── active      → [active] / [inactive] label
  └── entries (ordered by sortOrder ASC)
        ├── supplement.name  → supplement name
        ├── doseAmount + doseUnit → "5g", "1 capsule"
        ├── timeSlot         → e.g. PRE_WORKOUT, POST_WORKOUT, MORNING, EVENING, WITH_MEAL, CUSTOM
        ├── customTime       → used when timeSlot == CUSTOM
        └── notes            → appended if present
```

### Output Format (example)

```
Supplement plans (active):

### Daily Stack [active]
- Creatine Monohydrate — 5g, POST_WORKOUT
- Vitamin D3 — 2000 IU, MORNING
- Omega-3 — 1g, WITH_MEAL, notes: take with largest meal
- Magnesium Glycinate — 400mg, EVENING
```

### Tool Schema

```json
{
  "type": "object",
  "properties": {
    "active_only": {
      "type": "boolean",
      "description": "If true (default), return only active supplement plans."
    }
  },
  "required": []
}
```

---

## Tool 2: get_health_metrics

### Data Flow

```
Claude → tool_use: get_health_metrics { from_date: "...", to_date: "...", metric_keys: [...] }
             ↓
GetHealthMetricsTool.execute()
             ↓
HealthMetricService.getDailyMetrics(userId, "withings", from, to)
  → returns List<DailyHealthMetricsDto> (date + Map<metric_key, value>)
             ↓
Optionally filter to requested metric_keys
             ↓
Format as markdown table / list
```

### Known Metric Keys (Withings)

`weight_kg`, `body_fat_percent`, `fat_free_mass_kg`, `fat_mass_weight_kg`,
`muscle_mass_kg`, `bone_mass_kg`, `hydration_kg`, `heart_pulse_bpm`,
`basal_metabolic_rate`, `vo2_max`, `vascular_age`, `visceral_fat`,
`metabolic_age`, `nerve_health_score`, `pulse_wave_velocity`

### Output Format (example)

```
Health metrics (withings) — 2026-05-01 to 2026-06-01:

- 2026-06-01: weight 82.3kg, body_fat 16.2%, muscle_mass 67.1kg, bmr 1920 kcal, vo2_max 48.5
- 2026-05-25: weight 82.8kg, body_fat 16.5%, muscle_mass 66.9kg, heart_pulse 58 bpm
...
```

Only present metrics are rendered (sparse Withings measurements vary by sync).

### Tool Schema

```json
{
  "type": "object",
  "properties": {
    "from_date": {
      "type": "string",
      "description": "Start date YYYY-MM-DD (default: 30 days ago)"
    },
    "to_date": {
      "type": "string",
      "description": "End date YYYY-MM-DD (default: today)"
    },
    "metric_keys": {
      "type": "array",
      "items": { "type": "string" },
      "description": "Optional filter to specific metrics e.g. [\"weight_kg\",\"vo2_max\"]. Omit for all."
    }
  },
  "required": []
}
```
