## Why

When a user starts a workout session from a training plan and modifies it during the session (skipping exercises, swapping one for another, or adding unplanned ones), those deviations are invisible — the app cannot tell what was planned vs what was actually done. This makes it impossible to review session history meaningfully, identify recurring substitutions, or decide whether the plan itself should be updated.

## What Changes

- When a session is started from a plan, the current exercise list is **snapshotted as JSONB** on the session row — so "what was planned" is frozen at session-start time, independent of future plan edits.
- Each session exercise created from a plan exercise gets a `planned_exercise_id` FK back to the originating `planned_exercises` row — so exercises added mid-session (unplanned) are distinguishable from plan-sourced ones.
- A new read endpoint exposes a **deviation summary** for any plan-based session: which exercises were completed as planned, skipped, swapped (plan exercise replaced by a differently-named exercise), and added unplanned.
- The active-workout UI **tags exercises** as "from plan" or "added" and shows a deviation count badge.
- A new **"Apply to plan"** action lets the user push the actual session exercise list back as the plan's new exercise set (optional, explicit user action).

## Capabilities

### New Capabilities
- `plan-snapshot`: Freeze planned exercise list on the session at start time; schema + write path.
- `session-deviations`: Backend deviation-comparison logic + `GET /workouts/sessions/{id}/deviations` endpoint + frontend deviation summary UI.
- `plan-sync-from-session`: `POST /training-plans/{id}/sync-from-session/{sessionId}` — replace plan exercises with the actual session exercises.

### Modified Capabilities
- `workout-session-flow`: `startFromPlan` must now populate `plan_snapshot` and set `planned_exercise_id` on created exercises.

## Impact

**Backend**
- New Flyway migration V21: `exercises.planned_exercise_id UUID REFERENCES planned_exercises(id) ON DELETE SET NULL`; `workout_sessions.plan_snapshot JSONB`.
- `WorkoutSessionFlowService.startFromPlan` — populate both new fields.
- New `WorkoutDeviationService` — pure read-only comparison logic (no repository writes).
- New endpoint on `WorkoutSessionFlowController` (or existing controller): `GET /workouts/sessions/{id}/deviations`.
- New endpoint on `TrainingPlanController`: `POST /training-plans/{id}/sync-from-session/{sessionId}`.
- New response DTO: `SessionDeviationDto`.

**Frontend**
- `src/types/api.ts` — add `planSnapshot`, `plannedExerciseId` fields; add `SessionDeviationDto` type.
- `ActiveWorkoutPage` — label each exercise as plan-sourced or added.
- Session detail / finished session screen — deviation summary badge/list.
- Training plans page — "Apply session to plan" trigger after finishing a session.

**Database**
- Two nullable columns added to existing tables; no breaking schema changes.
- `plan_snapshot` is write-once at session start and never mutated.
