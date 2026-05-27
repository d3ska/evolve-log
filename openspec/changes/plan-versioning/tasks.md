## 1. Database Migration (V23)

- [x] 1.1 Create `V23__plan_versioning.sql` — add `current_version INT NOT NULL DEFAULT 1` to `training_plans`
- [x] 1.2 Create `training_plan_versions` table with `(id UUID PK, training_plan_id UUID FK, version INT, exercises JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now())`
- [x] 1.3 Add `UNIQUE (training_plan_id, version)` constraint and FK index on `training_plan_id`
- [x] 1.4 Add `plan_version INT` (nullable) column to `workout_sessions`

## 2. Domain & Repository

- [x] 2.1 Create `TrainingPlanVersion` JPA entity mapping `training_plan_versions`
- [x] 2.2 Create `TrainingPlanVersionRepository` with `findByTrainingPlanIdOrderByVersionAsc` and `findByTrainingPlanIdAndVersion`
- [x] 2.3 Add `currentVersion` field to `TrainingPlan` entity
- [x] 2.4 Add `planVersion` field to `WorkoutSession` entity

## 3. Backend — Version Bump Logic

- [x] 3.1 Add `bumpVersion(TrainingPlan, List<PlannedExercise>)` private method to `TrainingPlanService` that increments `currentVersion` and inserts a `TrainingPlanVersion` row
- [x] 3.2 Call `bumpVersion` inside `addPlannedExercise` (after insert, before return)
- [x] 3.3 Call `bumpVersion` inside `removePlannedExercise` (after delete, before return)
- [x] 3.4 Call `bumpVersion` inside `updatePlannedExercise` (after update, before return)
- [x] 3.5 Call version 1 row insert inside `createTrainingPlan` (initial snapshot with empty exercise list)

## 4. Backend — Version Read Service & Endpoints

- [x] 4.1 Create `TrainingPlanVersionService` with `listVersions(planId, userId)` and `getVersion(planId, version, userId)` methods (both enforce ownership, throw `ResourceNotFoundException` on missing)
- [x] 4.2 Create `TrainingPlanVersionDto` response DTO with `version`, `createdAt`, `exerciseCount`, and `exercises` (nullable — null for list endpoint, populated for single-version endpoint)
- [x] 4.3 Add `GET /api/training-plans/{id}/versions` endpoint to `TrainingPlanController` returning list of version summaries
- [x] 4.4 Add `GET /api/training-plans/{id}/versions/{version}` endpoint returning full snapshot

## 5. Backend — startFromPlan Update

- [x] 5.1 In `WorkoutSessionFlowService.startFromPlan`, set `planVersion = plan.getCurrentVersion()` on the new session row alongside the existing `plan_snapshot` write

## 6. Backend Tests

- [x] 6.1 Unit test `TrainingPlanService` — verify version row is created on plan creation (version 1, empty list)
- [x] 6.2 Unit test `TrainingPlanService` — verify version bumps on add / remove / update of `PlannedExercise`
- [x] 6.3 Unit test `TrainingPlanService` — verify metadata-only update does NOT bump version
- [x] 6.4 Unit test `TrainingPlanVersionService` — ownership enforcement (returns 404 for other user's plan)
- [x] 6.5 Integration test `TrainingPlanController` — `GET /versions` returns empty list for pre-V23 plan (no version rows)
- [x] 6.6 Integration test `TrainingPlanController` — `GET /versions/{v}` returns 404 for non-existent version
- [x] 6.7 Integration test `WorkoutSessionFlowService` — `startFromPlan` stamps `planVersion` on session; MANUAL session has null `planVersion`

## 7. Frontend — Types & API

- [x] 7.1 Add `planVersion: number | null` to `WorkoutSession` type in `src/types/api.ts`
- [x] 7.2 Add `TrainingPlanVersion` type (`version`, `createdAt`, `exerciseCount`, `exercises?`) in `src/types/api.ts`
- [x] 7.3 Add `versionsApi.list(planId)` and `versionsApi.get(planId, version)` to `src/api/plans.ts`
- [x] 7.4 Add `currentVersion: number` to `TrainingPlan` type in `src/types/api.ts`

## 8. Frontend — "Plan updated" Badge

- [x] 8.1 In `WorkoutsPage`, derive badge visibility: `session.planVersion !== null && session.planVersion < session.trainingPlan.currentVersion`
- [x] 8.2 Render `<Badge>` with label "Plan updated" on matching session cards using the existing `Badge` UI primitive
- [x] 8.3 Confirm badge does not appear for MANUAL sessions (`trainingPlanId = null`) or pre-V23 sessions (`planVersion = null`)

## 9. Frontend — Version History Section

- [x] 9.1 Fetch version list via `versionsApi.list(planId)` on training plan detail view mount
- [x] 9.2 Render Version History section: list entries showing version number, formatted `createdAt` date, and exercise count
- [x] 9.3 Render "No history yet" empty state when list is empty
- [x] 9.4 On version entry click, fetch the clicked version and the preceding version (if any) via `versionsApi.get`
- [x] 9.5 Compute client-side diff: compare `exercises` arrays by `exerciseDefinitionId` / name to produce added / removed / updated lists
- [x] 9.6 Render diff inline under the clicked version entry (added in green, removed in red, first version shows full list with no diff markup)
