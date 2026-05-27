## 1. Database Migration

- [x] 1.1 Write `V22__set_rest_timer.sql`: add `completed_at TIMESTAMPTZ` (nullable, no default) to `workout_sets`

## 2. Backend — Domain & Service

- [x] 2.1 Add `completedAt` field (`Instant`, nullable) to `WorkoutSet` entity — no public setter
- [x] 2.2 Update `WorkoutSet.update()` to stamp `completedAt = Instant.now()` when `completed` transitions to `true`, and clear it to `null` when transitioning to `false`
- [x] 2.3 Add `completedAt` field to `WorkoutSetDto` (exposed as ISO-8601 UTC string, nullable)

## 3. Backend — Tests

- [x] 3.1 Unit test: `WorkoutSet.update()` stamps `completedAt` on `completed = true`, clears on `false`, resets on re-complete
- [x] 3.2 Integration test: `PATCH /api/workouts/sets/{setId}` with `completed: true` returns non-null `completedAt`
- [x] 3.3 Integration test: `PATCH /api/workouts/sets/{setId}` with `completed: false` returns `completedAt: null`
- [x] 3.4 Integration test: V22 migration leaves existing rows with `completed_at = null`

## 4. Frontend — Types & API

- [x] 4.1 Add `completedAt: string | null` to `WorkoutSet` type in `src/types/api.ts`

## 5. Frontend — RestDivider Component

- [x] 5.1 Create `src/components/workout/RestDivider.tsx` — accepts `fromCompletedAt: string`, `toCompletedAt: string | null`
  - When `toCompletedAt` is non-null: render static "Rested: M:SS" (grey, subtle)
  - When `toCompletedAt` is null: render live count-up "Resting: M:SS ↑" using `useElapsedTimer(fromCompletedAt)`

## 6. Frontend — Active Workout Integration

- [x] 6.1 Update `ExerciseCard.tsx` to render `RestDivider` between consecutive `SetRow` elements when `set_N.completedAt` is non-null
  - Pass `fromCompletedAt={set_N.completedAt}` and `toCompletedAt={set_{N+1}.completedAt ?? null}`
  - Only the divider after the last completed set gets `toCompletedAt=null` (live timer)

## 7. Frontend — Session History Integration

- [x] 7.1 Update the session detail / history set list to render `RestDivider` (static variant) between consecutive sets when both `set_N.completedAt` and `set_{N+1}.completedAt` are non-null

## Out of Scope

- Target/countdown rest timer
- Push or vibration notifications on rest end
- Aggregate rest analytics (average rest per exercise/session)
