## Context

When `WorkoutSessionFlowService.startFromPlan` runs, it copies `PlannedExercise` rows into `Exercise` rows on the new session. After that moment the link is severed — no FK, no snapshot. If the user then:

- Removes an exercise mid-session → no record that it was planned
- Adds an exercise mid-session → indistinguishable from a plan exercise
- Edits the plan later → the session row sees the new plan state, not what it committed to at start

The current schema has `workout_sessions.training_plan_id` (nullable FK) but no way to reconstruct "what the plan looked like at session start", nor which session exercises originated from plan exercises.

Two independent pieces of information are needed:
1. **Per-exercise origin** — did this exercise come from the plan?
2. **Whole-plan snapshot** — what was the full intended exercise list when the session started?

---

## Goals / Non-Goals

**Goals:**
- Record `planned_exercise_id` on every `Exercise` created by `startFromPlan`
- Freeze the plan exercise list as JSONB on `workout_sessions.plan_snapshot` at session start
- Expose a `GET /workouts/sessions/{id}/deviations` endpoint returning a structured diff
- Surface deviation info in the UI (active workout tags + finished session summary)
- Allow explicit "apply this session back to plan" via `POST /training-plans/{id}/sync-from-session/{sessionId}`

**Non-Goals:**
- Full plan version history (branching, rollback, changelog) — out of scope
- Automatic plan updates after each session — always explicit user action
- Tracking set-level deviations (only exercise-level granularity)
- Retroactively backfilling `planned_exercise_id` for existing sessions

---

## Decisions

### D1: JSONB snapshot vs. versioned plan table

**Chosen:** `workout_sessions.plan_snapshot JSONB`

**Alternatives considered:**
- *Versioned plan table (`training_plan_versions`)* — each plan edit creates a new version row; sessions reference a version. Heavy: adds a full extra table, complicates plan edit flows, requires migrating all plan reads to go through versions. Overkill for a personal-use app where the plan is usually edited intentionally between blocks, not mid-session.
- *Snapshot table (`session_plan_snapshots`)* — separate normalised table. Same data as JSONB but with FK overhead and no benefit since the snapshot is read-only and never queried independently.
- *JSONB on session* — write-once at start, no further mutation, cheap to read. Sufficient for diff purposes. Chosen.

**Snapshot schema (JSONB array of objects):**
```json
[
  {
    "plannedExerciseId": "<uuid>",
    "name": "Bench Press",
    "sets": 4,
    "repsMin": 6,
    "repsMax": 8,
    "restSeconds": 120,
    "position": 0
  }
]
```

### D2: `planned_exercise_id` as a plain UUID column vs. a real FK

**Chosen:** Plain `UUID` column (`exercises.planned_exercise_id`) with `ON DELETE SET NULL` FK.

The FK is declared in the migration for referential integrity at DB level, but cascade-set-null ensures that deleting a plan (and its planned exercises) doesn't break session history. The column is nullable: `null` means "not from a plan" or "plan exercise was deleted".

### D3: Deviation classification rules

Given snapshot S and actual exercises A for a plan-based session:

| Class | Condition |
|---|---|
| `COMPLETED` | Snapshot entry has a matching session exercise (`planned_exercise_id` matches) |
| `SKIPPED` | Snapshot entry has no matching session exercise (no row with that `planned_exercise_id`) |
| `ADDED` | Session exercise has `planned_exercise_id = null` (added mid-session) |
| `SWAPPED` | Not modelled as a first-class type — a SKIPPED + an ADDED pair in the same session. The UI may optionally infer a swap by name-proximity heuristics, but the backend returns raw SKIPPED + ADDED entries. |

This avoids requiring explicit "replace" semantics in the active workout flow.

### D4: `plan-sync-from-session` — replace or merge?

**Chosen:** Full replace. The endpoint deletes all existing `planned_exercises` for the plan and inserts new ones mirroring the session's actual exercise list (name, sets; reps range derived from actual reps if available, else kept from snapshot).

**Rationale:** A merge strategy (keep unchanged, update changed, insert new) requires fuzzy matching and is complex to implement and explain. Full replace matches user mental model: "use this session as the new template".

### D5: Where does the deviation endpoint live?

**Chosen:** `GET /workouts/sessions/{id}/deviations` on the existing `WorkoutSessionFlowController`. The deviation is a property of the session, not the plan. A separate `WorkoutDeviationService` handles the pure comparison logic to keep `WorkoutSessionFlowService` focused.

### D6: JSONB mapping in JPA

Use `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6 / Spring Boot 3+) with a plain `String` field on `WorkoutSession`. Serialize/deserialize manually using Jackson `ObjectMapper` in the service layer — keeps the entity clean and avoids Hibernate-level JSON type issues with PostgreSQL JSONB.

---

## Risks / Trade-offs

- **Snapshot drift**: The snapshot is accurate at session start but can't reflect mid-session plan edits made in another tab. Acceptable — the snapshot is labelled as "at session start".
- **JSONB grows with large plans**: A plan with 20 exercises produces ~2 KB of JSON per session. Not a concern for personal-use volumes.
- **`planned_exercise_id` nulled by cascade**: If the user deletes a training plan, planned exercises are cascade-deleted → `planned_exercise_id` becomes null on old sessions. The snapshot still contains the original names so deviations remain partially computable from the snapshot alone. The deviation endpoint must handle this gracefully (treat null `planned_exercise_id` as ADDED if the session has a `plan_snapshot`).
- **Sync-from-session overwrites plan**: Destructive by design. The UI must show a clear confirmation before calling the endpoint.

## Migration Plan

1. Add V21 migration — two nullable columns, one FK index. Non-breaking, backward-compatible.
2. Deploy backend with new columns. Old `startFromPlan` sessions continue working; new columns are null for pre-V21 sessions.
3. Deviation endpoint returns `{ "unsupported": true }` for sessions created before V21 (detected by `plan_snapshot IS NULL`).
4. No data backfill required.
5. Rollback: drop the two columns and the FK index (V22 rollback migration if needed).

## Open Questions

- Should the snapshot be taken for retroactively-created (MANUAL) sessions that have a `training_plan_id` set? → **No.** Snapshot is only written by `startFromPlan`. MANUAL sessions with a plan link show `unsupported: true` on the deviations endpoint.
- Should `plan-sync-from-session` also update `repsMin`/`repsMax` from actual session data? → **Yes**, if the session has workout_sets with reps data, derive `repsMin = repsMax = modal reps value per exercise`. Otherwise keep snapshot values.
