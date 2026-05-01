## 1. Database — Exercise Definition Catalog (V14)

- [x] 1.1 Create `V14__exercise_definitions.sql`: `exercise_definitions` table with `id`, `name`, `primary_muscle`, `secondary_muscles TEXT[]`, `equipment`, `is_system`, `user_id`, `created_at`
- [x] 1.2 Add unique index on `LOWER(name)` WHERE `is_system = true`
- [x] 1.3 Add index on `user_id` WHERE `is_system = false`
- [x] 1.4 Seed ~80 common system exercises covering: chest, back, quads, hamstrings, glutes, shoulders, biceps, triceps, core, calves

## 2. Database — Extend Exercises Table (V15)

- [x] 2.1 Create `V15__exercise_volume_fields.sql`: add nullable `exercise_definition_id UUID REFERENCES exercise_definitions(id)` to `exercises`
- [x] 2.2 Add nullable `rpe NUMERIC(3,1) CHECK (rpe BETWEEN 1.0 AND 10.0)` to `exercises`
- [x] 2.3 Add nullable `primary_muscle VARCHAR(50)` to `exercises` (denormalized)
- [x] 2.4 Add index on `exercises(exercise_definition_id)` INCLUDE `(sets, reps, weight_kg, rpe)`
- [x] 2.5 Add index on `exercises(primary_muscle)`

## 3. Backend — Exercise Catalog Domain & Service

- [x] 3.1 Create `ExerciseDefinition` entity (`@Entity`, `@Table(name="exercise_definitions")`, Lombok `@Builder @Getter`, no setters)
- [x] 3.2 Create `ExerciseDefinitionRepository` with `findByNameIgnoreCaseAndIsSystemTrue`, `findByUserIdOrIsSystemTrue`, `findDistinctPrimaryMuscles`
- [x] 3.3 Create `ExerciseDefinitionDto` record: `id`, `name`, `primaryMuscle`, `secondaryMuscles`, `equipment`, `isSystem`
- [x] 3.4 Create `CreateExerciseDefinitionRequest` record with `@NotBlank name`, `@NotBlank primaryMuscle`, nullable `equipment`
- [x] 3.5 Create `ExerciseDefinitionService`: `listDefinitions(userId, query, muscle)`, `createUserDefinition(user, req)`, `getMuscleGroups()`
- [x] 3.6 Add 409 conflict check in `createUserDefinition` when name matches system definition

## 4. Backend — Update Exercise Logging to Support Definition Link

- [x] 4.1 Add `exerciseDefinitionId` (nullable) and `rpe` (nullable) fields to `CreateExerciseRequest` and `UpdateExerciseRequest` (or existing patch DTOs)
- [x] 4.2 Update `Exercise` entity: add `exerciseDefinitionId`, `rpe`, `primaryMuscle` fields with Lombok `@Getter` only
- [x] 4.3 Update `WorkoutService` (or exercise create/update logic): when `exerciseDefinitionId` is provided, look up definition and populate `primaryMuscle` on the exercise row
- [x] 4.4 Update `Exercise.applyPatch` to handle `exerciseDefinitionId`, `rpe`, `primaryMuscle`

## 5. Backend — Exercise Catalog Controller

- [x] 5.1 Create `ExerciseDefinitionController` at `/api/exercises/definitions`
- [x] 5.2 Implement `GET /api/exercises/definitions` — query params: `q` (name search), `muscle`
- [x] 5.3 Implement `POST /api/exercises/definitions` → 201
- [x] 5.4 Implement `GET /api/exercises/definitions/muscle-groups` → sorted distinct list

## 6. Backend — Volume Calculation Utility

- [x] 6.1 Create `VolumeCalculator` pure static class (no Spring annotations)
- [x] 6.2 Implement `volumeLoad(int sets, int reps, BigDecimal weightKg)` — returns `null` if `weightKg` is null
- [x] 6.3 Implement `internalLoad(BigDecimal volumeLoad, BigDecimal rpe)` — returns `null` if either is null
- [x] 6.4 Implement `epleyE1RM(BigDecimal weightKg, int reps)` — returns `null` if `reps > 12`, returns `weightKg` if `reps == 1`
- [x] 6.5 Write unit tests for all four methods: boundary cases (reps=1, reps=12, reps=13, null weight, null rpe)

## 7. Backend — Training Volume Service & Queries

- [x] 7.1 Create `ExerciseVolumeRepository` (or extend `ExerciseRepository`) with native/JPQL query: volume by muscle group for a session
- [x] 7.2 Add query: weekly volume aggregation by muscle group for a date range (GROUP BY `DATE_TRUNC('week', ws.date)`, `e.primary_muscle`)
- [x] 7.3 Create `SessionVolumeSummaryDto` record: `sessionId`, `totalVolumeLoad`, `totalInternalLoad`, `rpeCompleteness`, `exerciseCount`, `byMuscleGroup: List<MuscleGroupVolumeDto>`
- [x] 7.4 Create `MuscleGroupVolumeDto` record: `muscle`, `volumeLoad`, `internalLoad`, `exerciseCount`
- [x] 7.5 Create `WeeklyMuscleVolumeDto` record: `weekStart`, `muscle`, `volumeLoad`, `sessionCount`
- [x] 7.6 Create `TrainingVolumeService` (`@Transactional(readOnly = true)`): `getSessionVolumeSummary(sessionId, userId)`, `getWeeklyVolumeByMuscle(userId, from, to, muscle)`
- [x] 7.7 Add 52-week range validation in `getWeeklyVolumeByMuscle` — throw 400 if exceeded

## 8. Backend — Progressive Overload Service & Queries

- [x] 8.1 Add repository query: fetch last N sessions for a `(userId, exerciseDefinitionId)` pair, ordered by `ws.date DESC`
- [x] 8.2 Create `OverloadHistoryEntryDto` record: `sessionDate`, `sessionId`, `sets`, `reps`, `weightKg`, `e1Rm`, `volumeLoad`, `rpe`, `isPR`, `volumeDelta`
- [x] 8.3 Create `ProgressiveOverloadDto` record: `exerciseDefinitionId`, `exerciseName`, `history: List<OverloadHistoryEntryDto>`
- [x] 8.4 Create `ProgressiveOverloadService` (`@Transactional(readOnly = true)`): implement `getProgressiveOverload(userId, exerciseDefinitionId, sessions)`
- [x] 8.5 Implement PR detection logic: iterate history oldest-to-newest, track running max e1RM, flag entry as PR when it exceeds prior max
- [x] 8.6 Implement `volumeDelta` calculation: difference from previous history entry's `volumeLoad`

## 9. Backend — Auto-Link Service

- [x] 9.1 Add repository query: find all `exercises` rows for a user where `exercise_definition_id IS NULL`
- [x] 9.2 Implement `ExerciseAutoLinkService.autoLink(userId)`: bulk-match exercise names (case-insensitive) to system definitions, update `exercise_definition_id` and `primary_muscle`
- [x] 9.3 Return `AutoLinkResultDto { matched, skipped }` from service

## 10. Backend — Analytics Controller

- [x] 10.1 Create `TrainingAnalyticsController` at `/api/analytics`
- [x] 10.2 Implement `GET /api/analytics/sessions/{sessionId}/volume`
- [x] 10.3 Implement `GET /api/analytics/volume/weekly` with `from`, `to`, optional `muscle` params
- [x] 10.4 Implement `GET /api/analytics/progressive-overload/{exerciseDefinitionId}` with `sessions` param (default 12, max 52)
- [x] 10.5 Implement `POST /api/exercises/auto-link` (can live in `ExerciseDefinitionController`)

## 11. Backend — Tests

- [x] 11.1 Write `VolumeCalculatorTest` — unit tests for all four formulas including boundary cases
- [x] 11.2 Write `TrainingVolumeServiceTest` — unit tests: session summary (full, no weight, partial RPE), weekly aggregation (valid range, range too large)
- [x] 11.3 Write `ProgressiveOverloadServiceTest` — unit tests: PR detection (first session, new PR, tie, multi-session same day), volumeDelta (positive, negative, null on first)
- [x] 11.4 Write `TrainingAnalyticsControllerTest` — MockMvc: `GET /sessions/{id}/volume` → 200, 404; `GET /volume/weekly` → 200, 400 on range; `GET /progressive-overload/{id}` → 200, 404

## 12. Frontend — Exercise Definition Integration

- [x] 12.1 Add `src/api/exerciseDefinitions.ts`: `listDefinitions(q?, muscle?)`, `createDefinition(req)`, `getMuscleGroups()`
- [x] 12.2 Add exercise name autocomplete to the exercise log form — debounced `GET /api/exercises/definitions?q=` as user types, selects definition to set `exerciseDefinitionId`
- [x] 12.3 Add RPE number input (1–10, step 0.5) to exercise log form (optional field)
- [x] 12.4 Add "Link exercises" banner on the workout history page if any exercises have no definition linked — triggers auto-link call

## 13. Frontend — Volume Dashboard

- [x] 13.1 Add `src/api/analytics.ts`: `getSessionVolume(sessionId)`, `getWeeklyVolume(from, to, muscle?)`, `getProgressiveOverload(exerciseDefinitionId, sessions?)`
- [x] 13.2 Create `SessionVolumeSummary` component — shows total volume load, internal load (if available), and breakdown by muscle group as a horizontal bar chart
- [x] 13.3 Display `SessionVolumeSummary` on the workout session detail page
- [x] 13.4 Create `WeeklyVolumeChart` component — Recharts `BarChart` grouped by week, one series per muscle group, with muscle filter dropdown
- [x] 13.5 Add weekly volume chart to the Analytics page

## 14. Frontend — Progressive Overload Charts

- [x] 14.1 Create `ProgressiveOverloadChart` component — Recharts `LineChart` with e1RM over time, PR markers as dots with distinct color/size
- [x] 14.2 Add exercise definition selector (searchable dropdown) to choose which exercise to view
- [x] 14.3 Show `volumeDelta` as a secondary axis or tooltip annotation on the overload chart
- [x] 14.4 Add progressive overload section to the Analytics page with exercise selector + chart
