# Data Sync Idempotency — Proposal

## Why

Re-uploading the same CSV or triggering a sync twice must produce identical DB state.
Most sync paths are already idempotent after V19 (Fitatu dedup index, health_metrics upsert).
Two gaps remain:

1. **Blood tests** — re-uploading a CSV creates a duplicate `blood_test_reports` row plus
   all its `blood_test_results` children. No unique constraint exists.
2. **Fitatu service** — the DB constraint from V19 exists but the Java service still uses a
   plain `INSERT`, so the constraint fires as an exception rather than a clean upsert.

## What Changes

- New migration (V21): unique constraint on `blood_test_reports (user_id, date, lab_name)`
  and a composite unique constraint on `blood_test_results (report_id, parameter_key)`
- `BloodTestService`: replace insert with `ON CONFLICT DO NOTHING` at the report level;
  skip result rows if report already exists
- `FitatuService`: replace plain `save()` with a native upsert using
  `ON CONFLICT (user_id, date, meal, food_name) DO UPDATE SET nutrients = EXCLUDED.nutrients, quantity_g = EXCLUDED.quantity_g`
  so re-importing updated CSV data refreshes nutrient values rather than silently discarding them

## Impact

- 1 migration (V21)
- `BloodTestService` and `FitatuImportService` updated
- No API contract changes
- No frontend changes
