## 1. Database Migration

- [x] 1.1 Create `V21__plan_session_deviations.sql`: add `exercises.planned_exercise_id UUID REFERENCES planned_exercises(id) ON DELETE SET NULL`
- [x] 1.2 Add `workout_sessions.plan_snapshot JSONB` column (nullable) to the same migration
- [x] 1.3 Add index `idx_exercises_planned_exercise_id` on `exercises(planned_exercise_id)`

## 2. Domain & JPA

- [x] 2.1 Add `planSnapshot` field (`String`, `@JdbcTypeCode(SqlTypes.JSON)`) to `WorkoutSession` entity
- [x] 2.2 Add `plannedExerciseId` UUID field to `Exercise` entity (plain column, no JPA association)
- [x] 2.3 Add `setPlanSnapshot(String)` setter-style method on `WorkoutSession` (or use builder — keep entity consistent with existing immutability pattern)

## 3. Plan Snapshot Write Path

- [x] 3.1 Create `PlanSnapshotEntry` record (or inner class) with fields: `plannedExerciseId`, `name`, `sets`, `repsMin`, `repsMax`, `restSeconds`, `position`
- [x] 3.2 Inject `ObjectMapper` into `WorkoutSessionFlowService`
- [x] 3.3 In `startFromPlan`: after building exercises, serialize the `PlannedExercise` list to JSON and set `plan_snapshot` on the session
- [x] 3.4 In `startFromPlan`: set `planned_exercise_id` on each `Exercise` built from a `PlannedExercise`

## 4. Deviation Logic

- [x] 4.1 Create `WorkoutDeviationService` with method `getDeviations(UUID sessionId, UUID userId) → SessionDeviationDto`
- [x] 4.2 Implement classification: COMPLETED (exercise has matching `planned_exercise_id`), SKIPPED (snapshot entry has no matching exercise), ADDED (exercise has null `planned_exercise_id` in a plan-based session)
- [x] 4.3 Handle `plan_snapshot IS NULL` case — return `SessionDeviationDto` with `supported = false` and empty entries
- [x] 4.4 Handle cascade-null case: snapshot entry present but `planned_exercise_id` null on exercise → classify exercise as ADDED, snapshot entry as SKIPPED

## 5. Deviation Endpoint

- [x] 5.1 Create `SessionDeviationDto` response record: `sessionId`, `supported`, `entries: List<DeviationEntryDto>`
- [x] 5.2 Create `DeviationEntryDto` record: `status` (enum/string), `plannedExerciseId`, `name`, `plannedSets`, `plannedRepsMin`, `plannedRepsMax`, `actualSets`, `sessionExerciseId`
- [x] 5.3 Add `GET /api/workouts/sessions/{id}/deviations` endpoint to `WorkoutSessionFlowController`
- [x] 5.4 Wire `WorkoutDeviationService` into the controller

## 6. Deviation Count on Session DTO

- [x] 6.1 Add `deviationCount` (`Integer`, nullable) field to `WorkoutSessionDto`
- [x] 6.2 Compute `deviationCount` (SKIPPED + ADDED count) in `WorkoutSessionDto.from()` or in the service layer — use `plan_snapshot` presence to determine if computable
- [x] 6.3 Verify `deviationCount = null` for sessions without `plan_snapshot`

## 7. Plan Snapshot in Session DTO

- [x] 7.1 Add `planSnapshot` field (list of `PlanSnapshotEntryDto`) to `WorkoutSessionDto`
- [x] 7.2 Create `PlanSnapshotEntryDto` response record with all snapshot fields
- [x] 7.3 Deserialize `plan_snapshot` JSON in `WorkoutSessionDto.from()` and populate the field (null-safe)

## 8. Plan Sync Endpoint

- [x] 8.1 Add `syncFromSession(UUID planId, UUID sessionId, UUID userId)` to `TrainingPlanService`
- [x] 8.2 Reject sync if session status is `ACTIVE` (throw `ApiException` 409)
- [x] 8.3 Derive `repsMin`/`repsMax` from modal reps across `workout_sets` per exercise; fallback to snapshot; fallback to 0
- [x] 8.4 Delete all existing `PlannedExercise` rows for the plan and insert new ones from session exercises
- [x] 8.5 Add `POST /api/training-plans/{planId}/sync-from-session/{sessionId}` to `TrainingPlanController`

## 9. Tests

- [x] 9.1 Unit test `WorkoutDeviationService`: all-completed, one-skipped, one-added, cascade-null, unsupported (no snapshot)
- [x] 9.2 Unit test `WorkoutSessionFlowService.startFromPlan`: verify snapshot JSON content and `planned_exercise_id` values
- [x] 9.3 Unit test `TrainingPlanService.syncFromSession`: reps derivation (modal, snapshot fallback, zero fallback), active session rejection
- [x] 9.4 Integration/controller test: `GET /workouts/sessions/{id}/deviations` — 200 with entries, unsupported case, 404 for wrong user
- [x] 9.5 Integration/controller test: `POST /training-plans/{planId}/sync-from-session/{sessionId}` — success, active session 409, wrong user 404

## 10. Frontend — Types & API

- [x] 10.1 Add `planSnapshot: PlanSnapshotEntry[] | null` and `plannedExerciseId: string | null` and `deviationCount: number | null` to `WorkoutSession` / `Exercise` types in `src/types/api.ts`
- [x] 10.2 Add `SessionDeviation` and `DeviationEntry` types to `src/types/api.ts`
- [x] 10.3 Add `getDeviations(sessionId: string)` to `workoutsApi` in `src/api/workouts.ts`
- [x] 10.4 Add `syncFromSession(planId: string, sessionId: string)` to `plansApi` in `src/api/plans.ts`

## 11. Frontend — Active Workout UI

- [x] 11.1 In `ExerciseCard` (or `ActiveWorkoutPage`), show a subtle "From plan" badge when `exercise.plannedExerciseId` is non-null
- [x] 11.2 Show a subtle "Added" badge when `exercise.plannedExerciseId` is null and `session.trainingPlanId` is set

## 12. Frontend — Finished Session / Workouts Detail UI

- [x] 12.1 After `finish()` succeeds, call `getDeviations(sessionId)` and store result in local state
- [x] 12.2 Render deviation summary section: list SKIPPED exercises by name with a "skipped" label, ADDED exercises with an "added" label
- [x] 12.3 Hide the deviation section when `supported = false` or `deviationCount === 0`
- [x] 12.4 Show "Apply to plan" button on the finished session screen when `session.trainingPlanId` is set
- [x] 12.5 On "Apply to plan" click: show confirmation dialog ("This will replace the plan's exercises with what you actually did.")
- [x] 12.6 On confirmation: call `syncFromSession`, show success toast, hide the button

## 13. CLAUDE.md & OpenSpec Housekeeping

- [x] 13.1 Update `CLAUDE.md` migration table with V21 entry
- [x] 13.2 Update `CLAUDE.md` domain areas table (Workout row) to mention `plan_snapshot` and `planned_exercise_id`
- [x] 13.3 Add `plan-session-deviations` change to active changes list in `CLAUDE.md`
