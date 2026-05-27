## Context

The active-workout-module (fully shipped, V20 migration) introduced `workout_sets` with a `completed` boolean flag and a live workout timer. Rest timing was explicitly deferred. The `WorkoutSet` entity has no timestamps; only `workout_sessions` has `started_at` / `finished_at`.

The existing `useElapsedTimer(startedAt)` hook in the frontend counts up from a given `Instant` — it can be reused for per-set rest timing with zero changes.

## Goals / Non-Goals

**Goals:**
- Persist the exact moment each set is completed (`completed_at`) server-side
- Derive rest duration between adjacent sets from those timestamps (no separate column)
- Show a live count-up rest divider after the last completed set in the active workout UI
- Show static rest durations between sets in the session history detail view

**Non-Goals:**
- Target/countdown rest timer or notifications
- Per-exercise configurable rest targets
- Aggregate rest analytics (average rest per exercise/session) — future work
- Modifying rest time manually

## Decisions

### D1: Store `completed_at`, derive rest — not a `rest_seconds` column

**Decision:** Add `completed_at TIMESTAMPTZ NULL` to `workout_sets`. Rest duration = `set_{N+1}.completedAt − set_N.completedAt`.

**Alternatives considered:**
- `rest_seconds INTEGER` on each set: requires client to compute and send the value; lossy (no absolute time); wrong if client clock drifts.
- No persistence (frontend-only timer): rest data lost on page refresh and unavailable in history.

**Rationale:** A server-stamped timestamp is authoritative, replayable, and enables future analytics (e.g., average rest per muscle group) without a schema change.

### D2: Stamp `completed_at` server-side on PATCH, not client-supplied

**Decision:** `PATCH /api/workouts/sets/{setId}` ignores any client-supplied `completedAt`. The server sets `completed_at = now()` when `completed` transitions to `true`, and clears it to `null` when it transitions to `false`. Re-completing a set (false → true again) resets the timestamp.

**Rationale:** Prevents clock skew, tampering, and client complexity. The frontend never needs to send a timestamp.

### D3: Rest divider as a between-row component, not inline on the set row

**Decision:** A `RestDivider` component is rendered between consecutive `SetRow` elements inside `ExerciseCard`. It receives `fromCompletedAt` and `toCompletedAt` props. When `toCompletedAt` is null (last completed set), it renders a live count-up using the existing `useElapsedTimer` hook.

**Alternatives considered:**
- Append rest time to the completed `SetRow`: cramped on mobile; mixes concerns.
- Prominent rest banner per exercise card: too much visual noise when multiple exercises have completed sets.

**Rationale:** Dividers are a natural visual separator already implied by the list layout. They add zero width overhead and collapse to nothing when no timestamps are present (MANUAL sessions).

### D4: History view shows rest times only when `completedAt` is present on both adjacent sets

**Decision:** The session detail set list renders `RestDivider` (static variant) between sets only when both `set_N.completedAt` and `set_{N+1}.completedAt` are non-null.

**Rationale:** MANUAL sessions (created retroactively without the active workout flow) will never have `completed_at` values. Graceful degradation — history for those sessions looks unchanged.

## Risks / Trade-offs

- **Clock accuracy on server**: `now()` in the PATCH handler is the server receipt time, not the exact moment the user tapped ✓. Latency (typically < 500ms) is negligible for rest timing purposes.
- **Out-of-order PATCH delivery**: If two rapid PATCHes arrive out of order, `completed_at` could be slightly wrong. Acceptable — rest timers do not need millisecond precision.
- **Re-completing a set**: If a user unticks then re-ticks a set, `completed_at` resets to the re-tick time. This is intentional — the rest clock restarts.

## Migration Plan

1. Deploy V22 migration: `ALTER TABLE workout_sets ADD COLUMN completed_at TIMESTAMPTZ;`
   - Nullable, no default → zero impact on existing rows.
   - No backfill needed — historical sets simply show no rest times.
2. Deploy backend: `WorkoutSetService` stamps timestamp; DTO exposes field.
3. Deploy frontend: `RestDivider` renders only when `completedAt` is present — no visual change for old sets.

Rollback: drop the column (no other schema dependencies). Frontend degrades gracefully if field is absent.

## Open Questions

- None — scope is fully defined.
