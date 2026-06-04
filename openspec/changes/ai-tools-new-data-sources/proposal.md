# Proposal: New AI Data Source Tools

## Problem

Two significant data domains are invisible to the AI trainer:

### 1. Supplements

The user may have an active supplement protocol (creatine loading, vitamin D, protein
timing, pre-workout, etc.). When planning diet or recovery, the AI doesn't know:
- What supplements are scheduled
- What doses and time slots are prescribed
- Whether the protocol is aligned with training goals

### 2. Withings Health Metrics

The `health_metrics` EAV table (source = 'withings') contains device-measured body
composition and cardiovascular data:
- `weight_kg`, `body_fat_percent`, `muscle_mass_kg`, `bone_mass_kg`, `hydration_kg`
- `basal_metabolic_rate` — critical for calorie target planning
- `vo2_max` — aerobic capacity trend
- `visceral_fat` — health risk marker
- `heart_pulse_bpm` — resting heart rate trend

This is richer and more continuous than manual measurements, but currently invisible
to the AI.

## Proposed Solution

Add two new `AiTool` implementations. Both use existing repositories/services:

- `GetSupplementInfoTool` → `SupplementPlanRepository.findByUserIdOrderByCreatedAtDesc`
  (already eager-loads `entries` + `entries.supplement`)
- `GetHealthMetricsTool` → `HealthMetricService.getDailyMetrics(userId, "withings", from, to)`
  (same service already used by the nutrition tool)

No new queries or migrations required.
