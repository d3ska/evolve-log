# Active Workout Module — Tasks

## Phase 1: Database (V20 Migration)

- [x] **T1** Write `V20__active_workout_module.sql`:
  - Add `status VARCHAR(20) NOT NULL DEFAULT 'MANUAL'` to `workout_sessions`
  - Backfill: `FINISHED` where `finished_at IS NOT NULL`, `ACTIVE` where `started_at IS NOT NULL AND finished_at IS NULL`
  - Add `completed BOOLEAN NOT NULL DEFAULT FALSE` to `workout_sets`
  - Add partial unique index: `UNIQUE (user_id) WHERE status = 'ACTIVE'`

## Phase 2: Backend — Domain & Service

- [x] **T2** Add `status` field to `WorkoutSession` entity (no setter, use constructor/merge)
- [x] **T3** Add `completed` field to `WorkoutSet` entity
- [x] **T4** Update `WorkoutSessionService.startFromPlan()` to set `status = ACTIVE`
- [x] **T5** Update `WorkoutSessionService.finishSession()` to set `status = FINISHED`
- [x] **T6** Add `WorkoutSessionService.getActiveSession(userId)` — returns `Optional<WorkoutSession>`
- [x] **T7** Add `WorkoutSessionService.addExercise(sessionId, userId, name, sets)` — validates ownership only
- [x] **T8** Add `WorkoutSessionService.removeExercise(exerciseId, userId)` — validates ownership only
- [x] **T9** Add `WorkoutSetService.addSet(exerciseId, userId, setNumber, reps, weightKg)` — validates ownership only
- [x] **T10** Add `WorkoutSetService.updateSet(setId, userId, patch)` — partial update, validates ownership only
- [x] **T11** Add `WorkoutSetService.deleteSet(setId, userId)` — validates ownership only

## Phase 3: Backend — API

- [x] **T12** Add `GET /api/workouts/sessions/active` endpoint
- [x] **T13** Add `POST /api/workouts/exercises` endpoint with request DTO `{ sessionId, name, sets }`
- [x] **T14** Add `DELETE /api/workouts/exercises/{exerciseId}` endpoint
- [x] **T15** Add `POST /api/workouts/sets` endpoint with request DTO `{ exerciseId, setNumber, reps, weightKg }`
- [x] **T16** Add `PATCH /api/workouts/sets/{setId}` endpoint with request DTO `{ reps?, weightKg?, completed? }`
- [x] **T17** Add `DELETE /api/workouts/sets/{setId}` endpoint
- [x] **T18** Update `WorkoutSessionDTO` and `WorkoutSetDTO` to include `status` and `completed` respectively

## Phase 4: Backend — Tests

- [x] **T19** Integration tests for session status transitions (start → ACTIVE, finish → FINISHED, one-active constraint)
- [x] **T20** Integration tests for `GET /active` — returns session / null
- [x] **T21** Integration tests for exercise add/delete — mutations allowed on any session status
- [x] **T22** Integration tests for set CRUD + ownership guards
- [x] **T23** Unit tests for `WorkoutSetService.updateSet` partial patch logic

## Phase 5: Frontend

- [x] **T24** Add `workoutsApi.getActiveSession()`, `addExercise()`, `removeExercise()`, `addSet()`, `updateSet()`, `deleteSet()` to `src/api/workouts.ts`
- [x] **T25** Add `status` and `completed` to relevant types in `src/types/api.ts`
- [x] **T26** Create `src/hooks/useElapsedTimer.ts` — returns `HH:mm:ss` string, ticks every second
- [x] **T27** Create `src/hooks/useActiveWorkout.ts` — state management for active session (optimistic updates, pending count, error handling)
- [x] **T28** Create `src/pages/ActiveWorkoutPage.tsx` — route `/workouts/active`
- [x] **T29** Create `src/components/workout/ExerciseCard.tsx` — expanded/collapsed, set rows, add/delete set
- [x] **T30** Create `src/components/workout/SetRow.tsx` — reps + weight inputs, completed toggle, green state
- [x] **T31** Create `src/components/workout/WorkoutTimer.tsx` — uses `useElapsedTimer`
- [x] **T32** Add route `/workouts/active` in `src/App.tsx`
- [x] **T33** Wire recovery banner in `AppBootstrap` (calls `getActiveSession` on mount, stores in a global store or passes to layout)
- [x] **T34** Add recovery banner UI to `AppLayout.tsx`

## Out of Scope (future changes)

- Offline write queue (IndexedDB-backed retry for set saves when offline)
- Rest timer between sets
- RPE / notes per set

## Decisions Locked

- **Edit after finish:** All set/exercise mutations are allowed on any session status. `status` is for UX/timer only.
- **No discard endpoint:** To abandon a workout, the user finishes it and deletes the session. `DELETE /api/workouts/sessions/{id}` already handles this.
