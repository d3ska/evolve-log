# Spec: blood-test-dedup

### Requirement: Re-uploading the same blood test CSV is idempotent

#### Scenario: Upload identical CSV twice
- **WHEN** the user uploads a blood test CSV for `(date, lab_name)` that already exists
- **THEN** the second upload produces no new `blood_test_reports` row
- **AND** existing `blood_test_results` rows are updated in-place if values differ
- **AND** the API returns 200 (not 409)

#### Scenario: Upload CSV with corrected reference ranges
- **WHEN** the user re-uploads a CSV for an existing `(date, lab_name)` with updated ref ranges
- **THEN** the `ref_low`/`ref_high` fields of affected result rows are updated
- **AND** no duplicate rows are created

#### Scenario: Upload CSV with null lab_name twice
- **WHEN** the user uploads two CSVs both with `lab_name = null` on the same date
- **THEN** `lab_name` is stored as `''` (empty string), and the second upload upserts rather than duplicates

#### Scenario: Upload CSV for a new date
- **WHEN** the user uploads a CSV for a date not yet in the DB
- **THEN** a new `blood_test_reports` row and associated `blood_test_results` rows are created normally

### Requirement: Fitatu CSV re-import refreshes existing rows

#### Scenario: Re-import same CSV
- **WHEN** the user imports a Fitatu CSV that was previously imported
- **THEN** existing `fitatu_food_logs` rows are updated (`quantity_g`, `nutrients`, `imported_at`)
- **AND** no duplicate rows are created

#### Scenario: Re-import CSV with new food entries
- **WHEN** the re-import includes food entries not previously seen for that date/meal
- **THEN** the new entries are inserted alongside the existing (updated) rows
