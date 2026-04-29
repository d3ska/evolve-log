# Data Sync Idempotency — Design

## V21 Migration

```sql
-- Blood test report dedup: same user uploading the same date + lab is a re-upload
ALTER TABLE blood_test_reports
    ADD CONSTRAINT uq_blood_test_reports_dedup
    UNIQUE (user_id, date, lab_name);

-- Per-report parameter dedup: same parameter cannot appear twice in one report
ALTER TABLE blood_test_results
    ADD CONSTRAINT uq_blood_test_results_dedup
    UNIQUE (report_id, parameter_key);
```

> Note: `lab_name` is nullable. PostgreSQL UNIQUE treats NULLs as distinct, so two rows with
> `lab_name = NULL` on the same date do NOT conflict. For users who never enter a lab name,
> add a coalesce in the service layer: store `''` (empty string) as the canonical "unknown lab"
> value rather than NULL.

## Blood Test Service — Upsert Logic

```
1. Attempt INSERT INTO blood_test_reports ... ON CONFLICT (user_id, date, lab_name) DO NOTHING RETURNING id
2. If no row returned (conflict) → SELECT id FROM blood_test_reports WHERE user_id=? AND date=? AND lab_name=?
3. For each result row: INSERT INTO blood_test_results ... ON CONFLICT (report_id, parameter_key)
   DO UPDATE SET value=EXCLUDED.value, unit=EXCLUDED.unit, ref_low=EXCLUDED.ref_low,
                 ref_high=EXCLUDED.ref_high, flag=EXCLUDED.flag
```

Re-uploading with corrected reference ranges or flags updates the existing result rows rather
than duplicating them.

## Fitatu Service — Upsert Logic

```sql
INSERT INTO fitatu_food_logs (user_id, date, meal, food_name, quantity_g, nutrients)
VALUES (...)
ON CONFLICT (user_id, date, meal, food_name)
DO UPDATE SET
    quantity_g  = EXCLUDED.quantity_g,
    nutrients   = EXCLUDED.nutrients,
    imported_at = now()
```

Re-importing a CSV with corrected quantities or nutrient values silently refreshes the row.
New rows (food not previously imported for that date/meal) are inserted normally.
