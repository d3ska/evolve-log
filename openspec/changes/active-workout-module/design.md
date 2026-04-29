# Active Workout Module — Design

## Data Model Changes (V20 Migration)

```sql
-- Session status: ACTIVE = in-progress, FINISHED = completed via /finish,
-- MANUAL = retroactively created without using the start flow
ALTER TABLE workout_sessions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

-- Backfill: sessions with finished_at set → FINISHED; sessions with started_at but
-- no finished_at → ACTIVE; all others remain MANUAL
UPDATE workout_sessions SET status = 'FINISHED' WHERE finished_at IS NOT NULL;
UPDATE workout_sessions SET status = 'ACTIVE'   WHERE started_at IS NOT NULL AND finished_at IS NULL;

-- Per-set completion flag (user ticks the set as done during active workout)
ALTER TABLE workout_sets
    ADD COLUMN completed BOOLEAN NOT NULL DEFAULT FALSE;

-- Enforce: at most one ACTIVE session per user at a time
CREATE UNIQUE INDEX uq_workout_sessions_one_active_per_user
    ON workout_sessions (user_id)
    WHERE status = 'ACTIVE';
```

## API Design

### Session Status Transitions

```
(new)    → POST /start       → ACTIVE
ACTIVE   → POST /{id}/finish → FINISHED
(form)   → POST /sessions    → MANUAL
MANUAL stays MANUAL           (no start/finish flow used)
```

All statuses remain fully editable (sets, exercises, reps, weights).
`status` controls only the timer/live UX and the one-active constraint.
To abandon an unwanted workout, finish it and delete the session — no dedicated discard endpoint needed.

### New Endpoints

#### GET /api/workouts/sessions/active
Returns the current user's ACTIVE session, or `{ "data": null }` if none.
Used on app startup to detect an interrupted workout and offer resume.

#### POST /api/workouts/exercises
```json
{ "sessionId": "uuid", "name": "Bench Press", "sets": 3 }
```
Appends an exercise to an existing session. Returns the created Exercise DTO.
Allowed only when session `status = ACTIVE`.

#### DELETE /api/workouts/exercises/{exerciseId}
Removes an exercise and all its sets from a session.
Allowed only when session `status = ACTIVE` or `MANUAL`.

#### POST /api/workouts/sets
```json
{ "exerciseId": "uuid", "setNumber": 4, "reps": 8, "weightKg": 100.0 }
```
Adds a single set. Enforces `UNIQUE (exercise_id, set_number)` (already in DB).

#### PATCH /api/workouts/sets/{setId}
```json
{ "reps": 9, "weightKg": 102.5, "completed": true }
```
Partial update — only provided fields are changed. This is the atomic save call
fired on every keystroke blur or toggle in the active workout UI.
Returns the updated WorkoutSet DTO.

#### DELETE /api/workouts/sets/{setId}
Removes a set. Re-numbering of remaining sets is NOT done server-side —
the frontend is responsible for rendering correct set numbers.

## WorkoutSet DTO

```json
{
  "id": "uuid",
  "exerciseId": "uuid",
  "setNumber": 1,
  "reps": 8,
  "weightKg": 100.0,
  "completed": false
}
```

## WorkoutSession DTO (extended)

Add `status: "ACTIVE" | "FINISHED" | "MANUAL"` to the existing session DTO.

## Frontend Architecture

### Route & Component Tree

```
/workouts/active
└── ActiveWorkoutPage
    ├── WorkoutTimer          (reads session.startedAt, ticks every second)
    ├── ExerciseCard[]
    │   ├── SetRow[]          (reps + weight inputs, completed toggle)
    │   ├── AddSetButton
    │   └── CollapseToggle
    ├── AddExerciseModal
    └── FinishWorkoutButton
```

### State Strategy

- **Session data** (exercises + sets): kept in React local state, hydrated from API on mount.
- **Optimistic updates**: every `PATCH /sets/{id}` or `POST /sets` call updates local state
  immediately, then fires the async API call. On error, local state rolls back.
- **Timer**: a `useElapsedTimer(startedAt)` hook that computes `Date.now() - startedAt`
  every second using `setInterval`. Does not need to persist — recalculated on load.
- **Pending saves indicator**: track the count of in-flight save requests.
  Show a small spinner or "Saving…" badge in the header when `pendingCount > 0`.

### Recovery Flow

On `AppBootstrap` mount (already calls `authApi.me()`), also call `GET /api/workouts/sessions/active`.
If a session is returned and the user is NOT already on `/workouts/active`:
- Show a persistent banner: "You have an active workout in progress. Resume →"
- Navigating to `/workouts/active` loads the existing session.

### Completed vs Collapsed States

- **Set completed**: `completed = true` on the set row. Row turns green, inputs become read-only.
  Un-ticking re-enables edit mode.
- **Exercise collapsed**: a UI-only state (not persisted). When all sets of an exercise are
  completed, the card automatically collapses to a one-line summary. User can re-expand manually.

## Error Handling

- If a `PATCH /sets/{id}` fails, roll back the optimistic state and show a toast:
  "Failed to save set — tap to retry."
- If connectivity is lost mid-workout, queue saves in a `pendingQueue` array in component state
  and retry on reconnect (detect via `window.addEventListener('online', ...)`).
- Do NOT use a Service Worker write-through cache for mutation requests (AI chat SSE exclusion
  already establishes this pattern — extend it to `/api/workouts/*` write paths).
