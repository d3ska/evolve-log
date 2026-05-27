## Context

Training plans are mutable templates — exercises are added, removed, or reordered as the user's programme evolves. The current schema has no memory of those edits. Once a plan changes, historical sessions that were executed against the old plan appear as if they always matched the current template.

`plan-session-deviations` (V21) addressed a different problem: it captures deviations (exercises added ad hoc or skipped during a specific session) by storing a `plan_snapshot JSONB` on each session row. That snapshot is a point-in-time freeze of the *planned exercises at session start*. It is not a versioning system — it says nothing about *which edit introduced a change* or *what the plan looked like between two sessions*.

This design introduces a lightweight versioning layer on top of the existing training plan model.

**Constraints:**
- Must not break any existing session data. The `plan_snapshot` column is preserved; this change adds orthogonal columns.
- `plan_version` must be nullable to handle sessions from before V23 (pre-versioning) and MANUAL sessions without a plan.
- The solution must stay simple enough that the version table never becomes a source of complexity. Full audit/branching/rollback is out of scope.

## Goals / Non-Goals

**Goals:**
- Record a new version whenever the exercise list of a training plan is mutated (add / remove / update a `PlannedExercise`).
- Stamp each session with the plan version it was started from.
- Expose version history through two new read-only endpoints.
- Show a "Plan updated" badge in the session history view when a session was run against an older version.
- Show a Version History section on the plan detail view.

**Non-Goals:**
- Branching or rollback of plan versions.
- Versioning plan *metadata* changes (name, description, schedule days). Only exercise-list changes trigger a new version.
- Migrating existing sessions retroactively. Pre-V23 sessions have `plan_version = null`; they show no version badge.
- Diffing beyond what is stored in the snapshot. The UI diff is a comparison of two `exercises` arrays from version rows — no semantic merge logic.

## Decisions

### D1 — Separate `training_plan_versions` table (not append-only on the plan row)

**Chosen:** A dedicated `training_plan_versions` table stores one row per version:

```
training_plan_versions (
  id               UUID PK,
  training_plan_id UUID FK → training_plans,
  version          INT NOT NULL,
  exercises        JSONB NOT NULL,   -- snapshot of PlannedExercise list
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (training_plan_id, version)
)
```

`training_plans` gains `current_version INT NOT NULL DEFAULT 1`.

**Alternative considered:** Store diff/patch rows instead of full snapshots. Rejected — diffing requires reconstructing state by replaying patches, which adds complexity. Full JSONB snapshots are cheap (exercise lists are small, <50 rows) and trivially queryable.

**Alternative considered:** Encode versions as ETag / hash with no separate table. Rejected — gives no history, no timestamp, no way to display what changed.

### D2 — Version number is a monotonic integer per plan, not global

Each plan maintains its own `current_version` counter. Version 1 is the initial state. Every exercise-list mutation increments it and inserts a new row.

Rationale: simple to reason about, easy to display ("v3 → v5"), no cross-plan coordination needed.

### D3 — Version 1 is written eagerly at plan creation, not lazily on first mutation

When a new training plan is created, `current_version = 1` and a `training_plan_versions` row for version 1 is inserted in the same transaction. This ensures every plan always has at least one version row and removes the "version 0 vs version 1" edge case.

Existing plans migrated by V23 will have `current_version = 1` but no corresponding version row (they pre-date versioning). The API must handle this gracefully: if no version row exists for version 1, the endpoint returns an empty list — no crash.

### D4 — Version bump is a service-layer concern, not a DB trigger

The version increment and version-row insert happen inside `TrainingPlanService`, wrapped in the same `@Transactional` method that mutates the `PlannedExercise`. This keeps the logic visible, testable, and under Spring's transaction management. DB triggers are avoided as they are invisible to the service layer and harder to test.

### D5 — `workout_sessions.plan_version` is populated in `startFromPlan`, not at session end

The version that matters is the one active *at the moment the session begins*. `WorkoutSessionFlowService.startFromPlan` already writes `plan_snapshot`; it will also write `plan_version = plan.currentVersion` in the same write.

### D6 — Version snapshot format mirrors the existing `plan_snapshot` JSONB shape

`training_plan_versions.exercises` uses the same serialisation as `workout_sessions.plan_snapshot` (the same DTO structure). This means the frontend can reuse the same parsing code for both. It also gives consistency: `plan_snapshot` is the version pinned to a session; `training_plan_versions.exercises` is the canonical record.

### D7 — Read endpoints on `TrainingPlanController`, served by a new `TrainingPlanVersionService`

Separating the read logic into `TrainingPlanVersionService` keeps `TrainingPlanService` focused on mutations. The read service only touches `TrainingPlanVersionRepository` and has no write methods — satisfying the single-responsibility rule and making it easy to cache later if needed.

## Risks / Trade-offs

**[Race condition on concurrent plan edits]** → Two simultaneous mutations could both read `current_version = N` and both try to insert `version = N+1`. The `UNIQUE (training_plan_id, version)` constraint will reject the second write. Mitigation: use `SELECT … FOR UPDATE` on the `training_plans` row inside the mutation transaction. For a single-user personal app, concurrent edits are extremely unlikely, but the lock is cheap and makes the invariant hard.

**[Growing version table]** → Frequent plan edits accumulate rows. For a personal fitness tracker with <10 active plans changed a few times per month, total rows are in the hundreds — not a concern. No pruning strategy needed in this change.

**[Pre-V23 plans have no version rows]** → The UI "Plan updated" badge relies on `plan_version != null && plan_version < plan.currentVersion`. Pre-V23 sessions have `plan_version = null`, so no badge appears. This is intentional and documented as a known gap.

**[Version 1 gap for pre-V23 plans]** → Existing plans get `current_version = 1` from the migration DEFAULT but no corresponding `training_plan_versions` row. The Version History section should display "No history yet" gracefully rather than failing.

## Migration Plan

**V23 SQL (three additive statements):**
1. `ALTER TABLE training_plans ADD COLUMN current_version INT NOT NULL DEFAULT 1;`
2. Create `training_plan_versions` table with `(id, training_plan_id, version, exercises JSONB, created_at)`, unique constraint on `(training_plan_id, version)`, FK index on `training_plan_id`.
3. `ALTER TABLE workout_sessions ADD COLUMN plan_version INT;` (nullable, no default).

**Rollback:** All changes are additive. Rolling back means dropping the three added artifacts after reverting the application to the pre-V23 build. No data migration to undo.

**Deploy sequence:**
1. Apply V23 migration (additive — safe to apply before code deploy with the old binary).
2. Deploy backend with new service, controller, and DTOs.
3. Deploy frontend with new API calls, badge, and Version History section.

No data backfill required. Existing plans start at `current_version = 1` with no history rows.

## Open Questions

- **Should metadata-only plan changes (name, description, scheduled days) bump the version?** Current decision: no. Only exercise-list mutations bump. If the user later wants to track name changes, a separate concept can be added without touching this design.
- **Diff display: full snapshot per version, or calculated added/removed/updated list?** A simple client-side diff (compare adjacent version snapshots by `position` and `exercise_definition_id`) is feasible and avoids a backend diff endpoint. The exact diff algorithm will be decided in the specs/tasks artifact.
