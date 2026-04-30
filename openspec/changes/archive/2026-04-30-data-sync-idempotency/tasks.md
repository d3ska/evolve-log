# Data Sync Idempotency — Tasks

## Phase 1: Migration

- [x] **T1** Write `V21__data_sync_idempotency.sql`:
  - Add `UNIQUE (user_id, date, lab_name)` to `blood_test_reports`
  - Add `UNIQUE (report_id, parameter_key)` to `blood_test_results`
  - Verify no existing duplicates before adding constraints (add dedup DELETE if needed)

## Phase 2: Blood Test Service

- [x] **T2** Update `BloodTestService` upload logic:
  - Normalize `lab_name`: store `''` instead of `null`
  - Use native SQL `INSERT ... ON CONFLICT DO NOTHING RETURNING id` for report insert
  - Fall back to SELECT if no row returned (conflict path)
  - Use `INSERT ... ON CONFLICT (report_id, parameter_key) DO UPDATE` for result rows

## Phase 3: Fitatu Service

- [x] **T3** Update `FitatuImportService` to use native upsert:
  `ON CONFLICT (user_id, date, meal, food_name) DO UPDATE SET quantity_g=..., nutrients=..., imported_at=now()`

## Phase 4: Tests

- [x] **T4** Integration test: upload blood test CSV twice → single report row, result rows updated
- [x] **T5** Integration test: re-import Fitatu CSV → rows updated, no duplicates
- [x] **T6** Integration test: upload blood test CSV with null lab_name twice → single row
