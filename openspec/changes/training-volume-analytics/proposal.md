## Why

The workout tracker currently stores sessions and exercises but provides no analysis of training
load, muscle group balance, or strength progression — making it impossible to answer the core
question every lifter needs: am I actually improving, and am I training each muscle enough?

## What Changes

- Introduce an **exercise definition catalog** (seeded system exercises + user-defined) so
  exercises have identity, muscle group mapping, and equipment metadata instead of being
  free-text strings.
- Add `exercise_definition_id`, `rpe`, and `primary_muscle` fields to the existing `exercises`
  table (nullable — existing rows remain valid).
- Implement **Volume Load** calculation (`sets × reps × weight`) and **Internal Load**
  (`volume × RPE`) per exercise, per session, and per muscle group.
- Implement **Progressive Overload tracking**: e1RM history (Epley formula), personal record
  detection, and week-over-week volume comparison per muscle group.
- Expose new analytics endpoints under `/api/analytics/` and a definitions endpoint under
  `/api/exercises/definitions`.

## Capabilities

### New Capabilities

- `exercise-catalog`: Exercise definition catalog — system-seeded exercises with muscle group
  and equipment metadata, plus user-defined extensions. Backs the identity layer required by
  all analytics.
- `training-volume`: Volume Load and Internal Load computation per session and per muscle
  group, aggregated weekly. Powers the training load dashboard.
- `progressive-overload`: e1RM history, personal record detection, and volume increment
  tracking per exercise over time. Powers the strength progression charts.

### Modified Capabilities

- `global-api-standards`: No requirement changes — new endpoints follow existing envelope
  conventions.

## Impact

**Backend:**
- 2 new Flyway migrations (V14 exercise_definitions, V15 exercises columns)
- 2 new domain entities: `ExerciseDefinition`
- 3 new services: `ExerciseDefinitionService`, `TrainingVolumeService`,
  `ProgressiveOverloadService` (or combined into `TrainingVolumeService`)
- 1 new pure utility class: `VolumeCalculator` (no Spring dependencies)
- 2 new controllers: `ExerciseDefinitionController`, `TrainingAnalyticsController`
- Seed data script for ~80 system exercise definitions

**Frontend:**
- Exercise search/link UI when logging an exercise (autocomplete against `/api/exercises/definitions`)
- Session volume summary card (shown after saving a session)
- Weekly volume-by-muscle bar chart (Recharts)
- Progressive overload timeline chart per exercise (line chart with PR markers)

**Dependencies:** No new libraries required — computation is pure Java math, charts use the
already-planned Recharts dependency.
