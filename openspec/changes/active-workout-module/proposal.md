# Active Workout Module — Proposal

## Why

The current workout logging UX is a retrospective form: you finish training, then fill in sets/reps/weights.
This means the app cannot be used *during* a workout. More critically, a page refresh destroys any in-progress
data because there is no persistent "active session" concept visible to the frontend.

The `workout-session-flow` change added `started_at`/`finished_at` to the DB and the start/finish lifecycle to
the API. This change builds the *UX layer* on top of that foundation:

- A live workout timer (persisted via `started_at` — survives refresh)
- Atomic set-level saves so zero data is lost between sets
- Visual distinction between completed sets (green) and collapsed exercises
- Full CRUD on sets and exercises during an active session
- A dedicated "Resume active session" recovery path if the user navigates away

## What Changes

### Backend
- New `status` column on `workout_sessions` (`ACTIVE` / `FINISHED` / `MANUAL`) with automatic transitions
- New endpoints for granular set CRUD: `POST`, `PATCH`, `DELETE` on `/api/workouts/sets`
- New endpoints for granular exercise CRUD: `POST`, `DELETE` on `/api/workouts/exercises`
- `GET /api/workouts/sessions/active` — returns the in-progress session for the current user (null if none)
- `workout_sets.completed` boolean column for per-set completion state

### Frontend
- `ActiveWorkoutPage` (route `/workouts/active`) — full-screen workout mode
- Persistent elapsed timer calculated from `startedAt` stored on the session
- Optimistic UI: every set mutation updates state locally before the async API call settles
- "Completed" vs "Collapsed" exercise card states
- Recovery banner: if an active session exists on app load, surface a "Resume workout" prompt

## Capabilities

### New Capabilities
- `active-workout-persistence`: Granular set/exercise CRUD with `completed` flag and session status
- `active-workout-ui`: Live workout screen with timer, atomic saves, and CRUD

### Extended Capabilities
- `workout-session-flow`: `finish` transition now sets `status = FINISHED`; existing `start` sets `status = ACTIVE`

## Impact

- 1 new Flyway migration (V20): `status` on `workout_sessions`, `completed` on `workout_sets`
- New service methods in `WorkoutSessionService` / `WorkoutSetService`
- New controller endpoints
- New frontend page, hooks, and API module additions
- No changes to analytics engine — `monthly_exercise_aggregates` aggregation logic is unchanged
