## Why

Training plans evolve over time — an exercise gets swapped, a rep range adjusted, a rest period changed — but the app currently has no memory of those edits. When browsing historical sessions you cannot tell whether the plan used at the time matches today's plan, making it impossible to understand progression in the context of changing templates.

## What Changes

- Every mutation to a training plan's exercise list (add, remove, update a `PlannedExercise`) **creates a new immutable version snapshot** stored in a new `training_plan_versions` table.
- `training_plans` gains a `current_version` integer column that increments on each exercise-list change.
- `workout_sessions` gains a `plan_version` integer column stamped at `startFromPlan` time, recording which version of the plan the session was executed against.
- A new read endpoint exposes the full version history of a plan: `GET /training-plans/{id}/versions`.
- A new read endpoint exposes a single version snapshot: `GET /training-plans/{id}/versions/{version}`.
- The session history UI shows a **"Plan updated"** badge when a session's `plan_version` is older than the plan's `current_version`.
- The training plan detail UI gains a **Version History** section listing when the plan was changed and what the exercise list looked like at each version.

## Capabilities

### New Capabilities

- `plan-version-record`: Backend mechanism — `training_plan_versions` table, `training_plans.current_version`, `workout_sessions.plan_version`; version creation logic on exercise mutations; `GET /training-plans/{id}/versions` and `GET /training-plans/{id}/versions/{version}` endpoints.
- `plan-version-ui`: Frontend — "Plan updated" badge on historical sessions in `WorkoutsPage`; Version History section on the training plan detail view; version diff display (what exercises changed between two versions).

### Modified Capabilities

- `workout-session-flow`: `startFromPlan` must now also write `plan_version = plan.currentVersion` on the new session row. (The existing `plan_snapshot` write is unchanged.)

## Impact

**Backend**
- New Flyway migration V23: `training_plan_versions` table; `training_plans.current_version INT NOT NULL DEFAULT 1`; `workout_sessions.plan_version INT` (nullable — null for sessions predating this change or MANUAL sessions).
- `TrainingPlanService` — wrap all `PlannedExercise` mutations (add / remove / update) with version bump + version row insert.
- New `TrainingPlanVersionService` — pure read logic for version history endpoints.
- New endpoints on `TrainingPlanController`.
- New response DTOs: `TrainingPlanVersionDto`, `TrainingPlanVersionListDto`.
- `WorkoutSessionFlowService.startFromPlan` — populate `plan_version`.

**Frontend**
- `src/types/api.ts` — add `planVersion` to `WorkoutSession`; add `TrainingPlanVersion` type.
- `src/api/plans.ts` — add `versionsApi` calls.
- `WorkoutsPage` — "Plan updated" badge on session cards.
- Training plan detail / plans page — Version History section.

**Database**
- Two additive columns + one new table; no breaking schema changes.
- `plan_version` is nullable to handle pre-V23 sessions gracefully.
- Sessions created before V23 show no version badge (null treated as "version unknown").
