# Active Workout Module — Tasks

## Phase 1: Database (V20 Migration)

- [ ] **T1** Write `V20__active_workout_module.sql`:
  - Add `status VARCHAR(20) NOT NULL DEFAULT 'MANUAL'` to `workout_sessions`
  - Backfill: `FINISHED` where `finished_at IS NOT NULL`, `ACTIVE` where `started_at IS NOT NULL AND finished_at IS NULL`
  - Add `completed BOOLEAN NOT NULL DEFAULT FALSE` to `workout_sets`
  - Add partial unique index: `UNIQUE (user_id) WHERE status = 'ACTIVE'`

## Phase 2: Backend — Domain & Service

- [ ] **T2** Add `status` field to `WorkoutSession` entity (no setter, use constructor/merge)
- [ ] **T3** Add `completed` field to `WorkoutSet` entity
- [ ] **T4** Update `WorkoutSessionService.startFromPlan()` to set `status = ACTIVE`
- [ ] **T5** Update `WorkoutSessionService.finishSession()` to set `status = FINISHED`
- [ ] **T6** Add `WorkoutSessionService.getActiveSession(userId)` — returns `Optional<WorkoutSession>`
- [ ] **T7** Add `WorkoutSessionService.addExercise(sessionId, userId, name, sets)` — validates ownership only
- [ ] **T8** Add `WorkoutSessionService.removeExercise(exerciseId, userId)` — validates ownership only
- [ ] **T9** Add `WorkoutSetService.addSet(exerciseId, userId, setNumber, reps, weightKg)` — validates ownership only
- [ ] **T10** Add `WorkoutSetService.updateSet(setId, userId, patch)` — partial update, validates ownership only
- [ ] **T11** Add `WorkoutSetService.deleteSet(setId, userId)` — validates ownership only

## Phase 3: Backend — API

- [ ] **T12** Add `GET /api/workouts/sessions/active` endpoint
- [ ] **T13** Add `POST /api/workouts/exercises` endpoint with request DTO `{ sessionId, name, sets }`
- [ ] **T14** Add `DELETE /api/workouts/exercises/{exerciseId}` endpoint
- [ ] **T15** Add `POST /api/workouts/sets` endpoint with request DTO `{ exerciseId, setNumber, reps, weightKg }`
- [ ] **T16** Add `PATCH /api/workouts/sets/{setId}` endpoint with request DTO `{ reps?, weightKg?, completed? }`
- [ ] **T17** Add `DELETE /api/workouts/sets/{setId}` endpoint
- [ ] **T18** Update `WorkoutSessionDTO` and `WorkoutSetDTO` to include `status` and `completed` respectively

## Phase 4: Backend — Tests

- [ ] **T19** Integration tests for session status transitions (start → ACTIVE, finish → FINISHED, one-active constraint)
- [ ] **T20** Integration tests for `GET /active` — returns session / null
- [ ] **T21** Integration tests for exercise add/delete guards (FINISHED session → 400)
- [ ] **T22** Integration tests for set CRUD + optimistic conflict scenarios
- [ ] **T23** Unit tests for `WorkoutSetService.updateSet` partial patch logic

## Phase 5: Frontend

- [ ] **T24** Add `workoutsApi.getActiveSession()`, `addExercise()`, `removeExercise()`, `addSet()`, `updateSet()`, `deleteSet()` to `src/api/workouts.ts`
- [ ] **T25** Add `status` and `completed` to relevant types in `src/types/api.ts`
- [ ] **T26** Create `src/hooks/useElapsedTimer.ts` — returns `HH:mm:ss` string, ticks every second
- [ ] **T27** Create `src/hooks/useActiveWorkout.ts` — state management for active session (optimistic updates, pending count, error handling)
- [ ] **T28** Create `src/pages/ActiveWorkoutPage.tsx` — route `/workouts/active`
- [ ] **T29** Create `src/components/workout/ExerciseCard.tsx` — expanded/collapsed, set rows, add/delete set
- [ ] **T30** Create `src/components/workout/SetRow.tsx` — reps + weight inputs, completed toggle, green state
- [ ] **T31** Create `src/components/workout/WorkoutTimer.tsx` — uses `useElapsedTimer`
- [ ] **T32** Add route `/workouts/active` in `src/App.tsx`
- [ ] **T33** Wire recovery banner in `AppBootstrap` (calls `getActiveSession` on mount, stores in a global store or passes to layout)
- [ ] **T34** Add recovery banner UI to `AppLayout.tsx`

## Out of Scope (future changes)

- Offline write queue (IndexedDB-backed retry for set saves when offline)
- Rest timer between sets
- RPE / notes per set

## Decisions Locked

- **Edit after finish:** All set/exercise mutations are allowed on any session status. `status` is for UX/timer only.
- **No discard endpoint:** To abandon a workout, the user finishes it and deletes the session. `DELETE /api/workouts/sessions/{id}` already handles this.
