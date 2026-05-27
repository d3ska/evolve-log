## ADDED Requirements

### Requirement: workout_sets.completed_at column exists and is nullable
The system SHALL have a `completed_at TIMESTAMPTZ` column on the `workout_sets` table,
added via Flyway migration V22. The column SHALL be nullable with no default value,
so existing rows are unaffected and historical sets simply have `null`.

#### Scenario: Migration applies cleanly to existing data
- **WHEN** the V22 migration runs against a database with existing `workout_sets` rows
- **THEN** all existing rows have `completed_at = null` and no data is lost
