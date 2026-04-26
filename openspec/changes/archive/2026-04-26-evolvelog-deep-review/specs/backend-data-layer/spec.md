## ADDED Requirements

### Requirement: All collection-loading queries avoid N+1
The system SHALL load any `@OneToMany` or nested `@ManyToOne` collection that appears in an API
response using a single JOIN FETCH query or `@EntityGraph`, across all domains.

#### Scenario: Fetch training plan with planned exercises
- **WHEN** `GET /api/training-plans/{id}` is called
- **THEN** exactly 1 SQL query fetches the plan and all its planned exercises

#### Scenario: Fetch supplement plan with entries
- **WHEN** `GET /api/supplements/plans/{planId}` is called
- **THEN** exactly 1 SQL query fetches the plan, entries, and associated supplements

#### Scenario: Fetch blood test report with results
- **WHEN** `GET /api/blood-tests/{id}` is called
- **THEN** exactly 1 SQL query fetches the report and all its result rows

### Requirement: Foreign key columns have database indexes across all tables
The system SHALL have indexes on all foreign key columns that appear in WHERE clauses of
application queries. Missing indexes SHALL be added in a single Flyway migration (V13).

#### Scenario: User-scoped query uses index
- **WHEN** any endpoint queries a table by `user_id`
- **THEN** the database query plan uses an index scan, not a sequential scan

#### Scenario: Child-table lookup uses index
- **WHEN** entries are looked up by their parent ID (e.g., `plan_id`, `session_id`, `report_id`)
- **THEN** the database query plan uses an index scan

### Requirement: Date-range queries are UTC-bounded
The system SHALL interpret all date filter parameters as UTC calendar day boundaries
(`date 00:00:00Z` inclusive to `date+1 00:00:00Z` exclusive) across all domains that
support date filtering (supplement logs, workout sessions, measurements, health metrics).

#### Scenario: Date filter returns only records within the UTC day
- **WHEN** any endpoint with a `date` query param is called with `date=YYYY-MM-DD`
- **THEN** only records with `timestamp >= date 00:00:00Z AND timestamp < date+1 00:00:00Z` are returned
