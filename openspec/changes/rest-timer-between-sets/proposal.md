## Why

During an active workout, users have no way to track how long they rest between sets. Rest periods directly affect training quality and are valuable for analysis (e.g., shorter rests indicate higher intensity). This was explicitly deferred from the active-workout-module change as future scope.

## What Changes

- Add `completed_at TIMESTAMPTZ` column to `workout_sets` (V22 migration)
- Server stamps `completed_at = now()` when a set is marked `completed = true`; clears it when marked `false`
- Expose `completedAt` in `WorkoutSetDto`
- Active workout UI shows a rest divider between consecutive sets: static "Rested: M:SS" for past sets, live count-up for the interval after the last completed set
- Session history detail view shows static rest times between sets

## Capabilities

### New Capabilities

- `set-rest-timer`: Track and display rest time between sets — persisting `completed_at` on each set, deriving rest duration from adjacent timestamps, and rendering rest dividers in both the active workout and session history views

### Modified Capabilities

- `backend-data-layer`: New column on `workout_sets`; PATCH endpoint stamps timestamp on completion toggle

## Impact

- **Database**: V22 migration adds `completed_at TIMESTAMPTZ` (nullable) to `workout_sets`
- **Backend**: `WorkoutSetService.updateSet()` and `WorkoutSet` entity updated; `WorkoutSetDto` extended
- **Frontend — Active Workout**: `SetRow` / `ExerciseCard` components updated to render `RestDivider` between rows; new `useRestTimer` hook (or reuse `useElapsedTimer`)
- **Frontend — History**: Session detail set list updated to show static rest durations
- **No breaking changes** to existing API consumers — `completedAt` is additive to the DTO
