## ADDED Requirements

### Requirement: monthly_exercise_aggregates table
The system SHALL maintain a `monthly_exercise_aggregates` table with columns: `id` (UUID PK), `user_id` (FK to users), `exercise_name` (VARCHAR 255), `exercise_definition_id` (FK, nullable), `year_month` (DATE — first day of month), `max_weight_kg` (DECIMAL 8,2), `total_volume_kg` (DECIMAL 12,2), `session_count` (INT), `total_sets` (INT), and `computed_at` (TIMESTAMPTZ). A `UNIQUE` constraint SHALL exist on `(user_id, exercise_name, year_month)`.

#### Scenario: Unique constraint prevents duplicates
- **WHEN** the nightly job attempts to insert an aggregate for a `(user_id, exercise_name, year_month)` that already exists
- **THEN** the database raises a unique constraint violation which the UPSERT handles silently

### Requirement: Nightly UPSERT computation
The system SHALL compute monthly aggregates nightly at 02:30 using `@Scheduled(cron = "0 30 2 * * *")`. The job SHALL aggregate `exercises` joined to `workout_sessions` for completed months only (previous months and earlier) using SQL: `MAX(weight_kg)`, `SUM(sets * reps * weight_kg)` as total volume, `COUNT(DISTINCT workout_session_id)`, and `SUM(sets)`. The result SHALL be written using a native SQL UPSERT (`INSERT ... ON CONFLICT DO UPDATE`).

#### Scenario: Previous month aggregated on first run
- **WHEN** the nightly job runs for the first time
- **THEN** rows are created in `monthly_exercise_aggregates` for each exercise the user has performed in any completed calendar month

#### Scenario: UPSERT is idempotent
- **WHEN** the nightly job runs twice for the same completed month
- **THEN** existing rows are updated (not duplicated) and `computed_at` is refreshed to the latest run time

#### Scenario: Current month excluded
- **WHEN** the nightly job runs mid-month
- **THEN** no aggregate is written for the current (incomplete) calendar month

### Requirement: Index for fast AI tool lookups
The system SHALL define an index `idx_monthly_agg_lookup ON monthly_exercise_aggregates(user_id, exercise_name, year_month DESC)` to support efficient queries from the `get_monthly_aggregates` AI tool.

#### Scenario: Tool query uses index
- **WHEN** `get_monthly_aggregates` queries for a specific exercise over 12 months
- **THEN** the query executes with an index scan rather than a full table scan

### Requirement: AI tool reads pre-computed aggregates only
The `get_monthly_aggregates` AI tool SHALL read exclusively from `monthly_exercise_aggregates` and SHALL NOT perform any on-the-fly aggregation across raw `exercises` rows. If no aggregate exists for a requested period, the tool SHALL return an empty list for that period rather than falling back to raw data.

#### Scenario: Tool returns empty list for missing months
- **WHEN** the model requests monthly aggregates for a period where no aggregate rows exist
- **THEN** the tool returns an empty list without querying the `exercises` table

### Requirement: Failure handling and logging
The monthly aggregate job SHALL wrap all processing in a try-catch block. Failures SHALL be logged at `ERROR` level with the affected user ID and stack trace. The job SHALL continue processing remaining users after a single user failure.

#### Scenario: Per-user failure is isolated
- **WHEN** the aggregate computation fails for one user due to a SQL error
- **THEN** the error is logged at `ERROR` level and the job proceeds to process any other users without aborting
