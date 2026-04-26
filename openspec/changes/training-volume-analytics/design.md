## Context

The workout tracker stores sessions and exercises as flat aggregate rows (`sets`, `reps`,
`weightKg` per exercise). Exercises have no identity — just a free-text `name` column — so
grouping the same movement across sessions requires unreliable string matching. There is no
RPE field, no muscle group data, and no derived metrics. The analytics service
(`AnalyticsService`) currently handles only personal records and exercise progress using
basic SQL aggregates.

This design adds an identity layer (exercise definition catalog) and two new analytical
domains (volume calculation, progressive overload) without breaking existing workout logging.

## Goals / Non-Goals

**Goals:**
- Exercise definitions with muscle group and equipment metadata, seeded for ~80 common exercises
- Volume Load and Internal Load per session and per muscle group
- Weekly volume aggregation per muscle group for trend charts
- e1RM timeline and personal record detection per exercise
- All computation in the service layer (no DB triggers, no materialized views)
- Nullable FK to exercise definitions — existing exercise rows remain valid

**Non-Goals:**
- Set-level logging (each set as a separate row) — current aggregate model is kept
- Nutrition-volume correlation
- AI-generated training recommendations
- Real-time push notifications for PRs

## Decisions

### Decision 1: Exercise Identity via Optional FK (not mandatory catalog link)

`exercises.exercise_definition_id` is nullable. Existing rows and future free-text entries
are valid. Analytics that require identity (progressive overload, muscle group breakdown) are
silently excluded for unlinked exercises; the UI shows a "Link to exercise" prompt.

**Alternative considered:** Mandatory FK, migrate all existing rows by fuzzy-matching names.
Rejected: fuzzy match is fragile, and forcing users to clean up old data creates friction.

**Consequence:** Volume by muscle group and progressive overload only cover linked exercises.
Session total volume covers all exercises regardless of linkage.

### Decision 2: Denormalize `primary_muscle` onto `exercises`

`exercises.primary_muscle` is copied from the definition at insert/update time. This avoids
a 3-table join (sessions → exercises → definitions) on every volume aggregation query.
A DB-level check constraint is not added — the application layer is the source of truth.

**Alternative considered:** Always join to `exercise_definitions`. Rejected: adds join to the
hot read path for every session load.

### Decision 3: e1RM Computed in Java, Not SQL

Epley formula (`weight × (1 + reps/30)`) is implemented in a pure static `VolumeCalculator`
class with no Spring dependencies, making it trivially unit-testable and easy to swap
(Brzycki, O'Conner formulas) without touching SQL.

**Alternative considered:** PostgreSQL expression column or generated column. Rejected:
formula changes would require a migration, and it can't be formula-swapped per user preference.

### Decision 4: Aggregate in SQL, Enrich in Java

Volume Load aggregation (`SUM(sets × reps × weight_kg)`) and weekly grouping happen in SQL
via `@Query` JPQL or native queries. e1RM computation, PR detection, and delta calculation
happen in Java after the query returns. This keeps queries simple and logic testable.

**Alternative considered:** Full computation in Java (fetch all rows, aggregate in streams).
Rejected: would load unbounded history into memory for weekly volume queries.

### Decision 5: `TrainingVolumeService` Covers Both Volume and Progressive Overload

A single `@Transactional(readOnly = true)` service handles both analytical domains. They
share the same query pattern (join sessions + exercises filtered by user + optional definition)
and the `VolumeCalculator` utility. Splitting into two services adds indirection without
cohesion benefit at current scale.

### Decision 6: Seed Data as Flyway Migration (not application startup)

~80 system exercise definitions are inserted in `V14__exercise_definitions.sql` as static SQL
rows with `is_system = true`. This makes the seed idempotent, version-controlled, and visible
in DB history.

**Alternative considered:** `ApplicationRunner` bean on startup. Rejected: harder to audit,
runs on every restart, and must handle race conditions in multi-instance deploys.

## Risks / Trade-offs

**[Risk] Volume calculation is approximate for variable-weight sets**
Current aggregate model (`3 sets × 8 reps × 100kg`) assumes all sets use the same weight.
A user who does descending sets will see overestimated volume.
→ *Mitigation:* Document this limitation in the UI. Set-level logging can be added in a
future change without breaking the current API.

**[Risk] e1RM formula accuracy drops above 10 reps**
Epley's formula diverges from reality for high-rep sets (15+).
→ *Mitigation:* Display e1RM only when `reps <= 12`. Show raw max weight as fallback for
higher rep ranges.

**[Risk] Seed data naming conflicts with user's existing free-text exercise names**
A user who logs "Bench Press" will have an existing string that matches the seed definition,
but the FK is not automatically populated.
→ *Mitigation:* Provide a one-time "auto-link" API that matches existing exercise names
(case-insensitive) to definitions and backfills `exercise_definition_id`. Surfaced as an
optional onboarding step in the UI, not forced migration.

## Migration Plan

1. Deploy V14 migration → creates `exercise_definitions` table with seed data. No app changes required.
2. Deploy V15 migration → adds nullable columns to `exercises`. Zero downtime (nullable add).
3. Deploy new application code → new endpoints available, existing workout endpoints unchanged.
4. (Optional, user-triggered) Call `POST /api/exercises/auto-link` to backfill `exercise_definition_id`
   on existing rows by name match.

Rollback: drop V15 columns (data loss only on newly added `rpe` / `exercise_definition_id`
values, which are additive). V14 can be dropped independently.

## Open Questions

- Should secondary muscle groups be stored as a `TEXT[]` PostgreSQL array or a separate join
  table (`exercise_definition_muscles`)? Array is simpler for read; join table is queryable
  for "exercises that work triceps". Decision: use `TEXT[]` for now, migrate if querying
  secondary muscles becomes needed.
- Should `rpe` be per-exercise or per-session? Per-exercise is more precise; per-session
  (session RPE) is simpler to log. Decision: per-exercise on the `exercises` table. Session
  RPE can be computed as average or left as a separate future field on `workout_sessions`.
