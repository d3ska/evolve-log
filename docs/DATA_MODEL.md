# EvolveLog — Data Model Reference

> Auto-generated from Flyway migrations V1–V18 and repository/service layer analysis.
> Last updated: 2026-04-28

---

## Table of Contents

1. [Core Auth & Users](#1-core-auth--users)
2. [Body Measurements](#2-body-measurements)
3. [Workout Sessions & Exercises](#3-workout-sessions--exercises)
4. [Exercise Definitions & Catalog](#4-exercise-definitions--catalog)
5. [Training Plans](#5-training-plans)
6. [Progress Photos & Media](#6-progress-photos--media)
7. [Withings Integration](#7-withings-integration)
8. [Health Metrics (EAV)](#8-health-metrics-eav)
9. [Fitatu Food Logs](#9-fitatu-food-logs)
10. [Blood Tests](#10-blood-tests)
11. [Supplements](#11-supplements)
12. [AI Features](#12-ai-features)
13. [Analytics Aggregates](#13-analytics-aggregates)
14. [Index Summary](#14-index-summary)
15. [Access Patterns by Feature](#15-access-patterns-by-feature)

---

## 1. Core Auth & Users

### `users`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `email` | `VARCHAR` UNIQUE NOT NULL | |
| `password_hash` | `VARCHAR` | Null for OAuth-only accounts |
| `unit_preference` | `VARCHAR` | `'METRIC'` or `'IMPERIAL'` (V5) |
| `created_at` | `TIMESTAMP` | |

**Access patterns:**
- Lookup by `email` on login — covered by UNIQUE index
- Lookup by `id` (FK from all other tables) — PK index

**Notes:** Google OAuth added in V7; users without a password use Google login only. Unit preference used client-side to convert displayed values.

---

## 2. Body Measurements

### `measurements`

Manual body composition entries logged by the user.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `date` | `TIMESTAMP` NOT NULL | |
| `weight_kg` | `DECIMAL` | Nullable (V4 — made optional) |
| `body_fat_pct` | `DECIMAL` | |
| `chest_cm` | `DECIMAL` | |
| `waist_cm` | `DECIMAL` | |
| `biceps_cm` | `DECIMAL` | |
| `thigh_cm` | `DECIMAL` | |
| `calves_cm` | `DECIMAL` | |
| `notes` | `TEXT` | |

**Indexes:** `(user_id)` — for time-series queries per user

**Access patterns:**
- List all for user ordered by date DESC
- Date range filter for trend charts

---

## 3. Workout Sessions & Exercises

### `workout_sessions`

A single training session (one gym visit / workout day).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `training_plan_id` | `UUID` FK → `training_plans` | Nullable — free-form sessions allowed |
| `date` | `TIMESTAMP` NOT NULL | Session date/time |
| `started_at` | `TIMESTAMP` | Added V16 |
| `finished_at` | `TIMESTAMP` | Added V16 |
| `notes` | `TEXT` | |

**Indexes:** `(user_id)`, `(training_plan_id)`

---

### `exercises`

Individual exercises within a session. Each row = one exercise block (e.g., "Bench Press").

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `workout_session_id` | `UUID` FK → `workout_sessions` | |
| `exercise_definition_id` | `UUID` FK → `exercise_definitions` | Nullable — linked via auto-link or manual |
| `name` | `VARCHAR` NOT NULL | Free-text name (normalized by auto-link) |
| `primary_muscle` | `VARCHAR` | Denormalized from definition (V15) |
| `sets` | `INT` | Nullable — exercise-level summary |
| `reps` | `INT` | Nullable — exercise-level summary |
| `weight_kg` | `DECIMAL` | Nullable — exercise-level summary |
| `rpe` | `DECIMAL` | Rate of Perceived Exertion 1–10 (V15) |
| `notes` | `TEXT` | |
| `order_index` | `INT` | Display order within session |

**Indexes:** `(workout_session_id)`, `(exercise_definition_id)`

**Notes:**
- Exercise-level `sets`/`reps`/`weight_kg` used by older log entries (pre-V17).
- Newer plan-based workouts store individual sets in `workout_sets`; exercise-level fields may be null.
- `primary_muscle` is denormalized for performance in volume queries.

---

### `workout_sets`

Individual sets within an exercise (added V17).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `exercise_id` | `UUID` FK → `exercises` | |
| `set_number` | `INT` NOT NULL | 1-based ordering |
| `reps` | `INT` | Nullable (bodyweight / failure sets) |
| `weight_kg` | `DECIMAL` | Nullable |
| `rpe` | `DECIMAL` | Per-set RPE override |
| `notes` | `TEXT` | |

**Indexes:** `(exercise_id)`

**Access patterns:**
- Fetched eagerly with exercises for volume calculation
- Progressive overload: fallback to max-weight set when exercise-level data absent

---

## 4. Exercise Definitions & Catalog

### `exercise_definitions`

Catalog of named exercises with muscle group metadata. Seeded with ~80 system entries in V14.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | Null for system exercises |
| `name` | `VARCHAR` NOT NULL | Canonical exercise name |
| `primary_muscle` | `VARCHAR` | E.g. `'CHEST'`, `'QUADS'`, `'BACK'` |
| `secondary_muscles` | `VARCHAR[]` or `TEXT` | |
| `equipment` | `VARCHAR` | E.g. `'BARBELL'`, `'DUMBBELL'` |
| `is_system` | `BOOLEAN` NOT NULL | True = Anthropic-seeded; False = user-created |
| `created_at` | `TIMESTAMP` | |

**Indexes:** `(user_id)`, unique index on `(name)` for system exercises

**Access patterns:**
- `list(q, muscle)` — filtered search for exercise picker (`GET /exercises/definitions?q=bench&muscle=CHEST`)
- Lookup by `id` for progressive overload — authorization check: `is_system OR user_id = :userId`
- Auto-link: `LOWER(exercise.name) = LOWER(definition.name)` + `is_system = true` bulk update

---

## 5. Training Plans

### `training_plans`

Reusable workout plan templates.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `name` | `VARCHAR` NOT NULL | |
| `description` | `TEXT` | |
| `created_at` | `TIMESTAMP` | |

---

### `planned_exercises`

Exercises prescribed within a training plan template.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `training_plan_id` | `UUID` FK → `training_plans` | |
| `exercise_definition_id` | `UUID` FK → `exercise_definitions` | Added V16 |
| `name` | `VARCHAR` | |
| `target_sets` | `INT` | |
| `target_reps` | `INT` | |
| `target_weight_kg` | `DECIMAL` | |
| `order_index` | `INT` | |
| `notes` | `TEXT` | |

**Indexes:** `(training_plan_id)`, `(exercise_definition_id)`

---

## 6. Progress Photos & Media

### `progress_photos`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `date` | `TIMESTAMP` | |
| `url` | `VARCHAR` | Storage URL |
| `notes` | `TEXT` | |

---

### `media_attachments`

General-purpose file attachment table for any entity type (V3).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `entity_type` | `VARCHAR` | E.g. `'WORKOUT_SESSION'`, `'MEASUREMENT'` |
| `entity_id` | `UUID` | Polymorphic FK (no DB constraint) |
| `url` | `VARCHAR` | |
| `file_name` | `VARCHAR` | |
| `content_type` | `VARCHAR` | MIME type |
| `created_at` | `TIMESTAMP` | |

**Indexes:** `(user_id)`, `(entity_type, entity_id)`

---

## 7. Withings Integration

### `withings_tokens`

OAuth 2.0 tokens for Withings API access (V6).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` UNIQUE | One token set per user |
| `access_token` | `TEXT` NOT NULL | |
| `refresh_token` | `TEXT` NOT NULL | |
| `expires_at` | `TIMESTAMP` | Used to detect expiry and trigger refresh |
| `scope` | `VARCHAR` | Withings scopes granted |
| `updated_at` | `TIMESTAMP` | |

**Access patterns:**
- Lookup by `user_id` before any Withings API call
- Updated in-place on token refresh

**Notes:** The earlier `withings_measurements` table (V6) was dropped in V9 and replaced by the generic `health_metrics` EAV table.

---

## 8. Health Metrics (EAV)

### `health_metrics`

Generic Entity-Attribute-Value table for all device-sourced health data (V9). Replaces the typed `withings_measurements` table.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `source` | `VARCHAR` NOT NULL | E.g. `'WITHINGS'`, `'MANUAL'` |
| `date` | `DATE` NOT NULL | Measurement date |
| `metric_key` | `VARCHAR` NOT NULL | E.g. `'weight'`, `'fat_ratio'`, `'muscle_mass'`, `'bone_mass'`, `'hydration'`, `'pulse_wave_velocity'` |
| `value` | `DECIMAL` NOT NULL | |
| `unit` | `VARCHAR` | E.g. `'kg'`, `'%'`, `'bpm'` |
| `created_at` | `TIMESTAMP` | |

**Indexes:** `(user_id, date)`, `(user_id, source, metric_key)`

**Known metric keys (Withings):**

| Key | Unit | Description |
|-----|------|-------------|
| `weight` | `kg` | Body weight |
| `fat_ratio` | `%` | Body fat percentage |
| `fat_mass_weight` | `kg` | Fat mass |
| `fat_free_mass` | `kg` | Lean mass |
| `muscle_mass` | `kg` | Muscle mass |
| `bone_mass` | `kg` | Bone mass |
| `hydration` | `kg` or `%` | Body water |
| `pulse_wave_velocity` | `m/s` | Arterial stiffness (V8) |
| `heart_rate` | `bpm` | Resting heart rate |

**Access patterns:**
- Latest value per metric key for dashboard cards
- Time-series query `(user_id, source, metric_key, date BETWEEN :from AND :to)` for trend charts
- Aggregated weekly/monthly for AI context

---

## 9. Fitatu Food Logs

### `fitatu_food_logs`

Raw food diary imported from Fitatu CSV export (V10). One row per food item per meal per day.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `date` | `DATE` NOT NULL | |
| `meal` | `VARCHAR(100)` | E.g. `'Breakfast'`, `'Lunch'` |
| `food_name` | `VARCHAR(500)` | |
| `quantity_g` | `NUMERIC(10,2)` | Serving weight in grams |
| `nutrients` | `JSONB` NOT NULL | Flexible nutrient data (see below) |
| `imported_at` | `TIMESTAMP WITH TIME ZONE` | |

**`nutrients` JSONB structure:**
```json
{
  "calories": 350.5,
  "protein_g": 28.0,
  "carbs_g": 42.0,
  "fat_g": 8.5,
  "fiber_g": 3.2,
  "sugar_g": 12.0,
  "saturated_fat_g": 2.1,
  "sodium_mg": 480.0
}
```
All fields optional; queried with `->>`  / `::decimal` casts.

**Indexes:** `(user_id, date DESC)`, `UNIQUE (user_id, date, meal, food_name)` — dedup constraint prevents re-import duplicates

**Access patterns:**
- Daily totals: `SUM((nutrients->>'calories')::decimal)` GROUP BY `date`
- Date range macro-summary for AI insights
- Meal breakdown for a specific day

---

## 10. Blood Tests

### `blood_test_reports`

A single lab visit / blood draw event (V11).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `test_date` | `DATE` NOT NULL | |
| `lab_name` | `VARCHAR` | |
| `notes` | `TEXT` | |
| `created_at` | `TIMESTAMP` | |

**Indexes:** `(user_id)`

---

### `blood_test_results`

Individual biomarker results within a report.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `report_id` | `UUID` FK → `blood_test_reports` | |
| `marker_name` | `VARCHAR` NOT NULL | E.g. `'Testosterone'`, `'HbA1c'`, `'CRP'` |
| `value` | `DECIMAL` NOT NULL | |
| `unit` | `VARCHAR` | E.g. `'nmol/L'`, `'%'`, `'mg/L'` |
| `reference_min` | `DECIMAL` | Lab reference range lower bound |
| `reference_max` | `DECIMAL` | Lab reference range upper bound |
| `flag` | `VARCHAR` | `'HIGH'`, `'LOW'`, `'NORMAL'` — derived or imported |

**Indexes:** `(report_id)`, `(parameter_key)` — for trend queries by biomarker name

**Access patterns:**
- All reports + results for a user (list page)
- Latest value per `parameter_key` for trend comparison across reports
- Out-of-range detection: `value < ref_low OR value > ref_high`

---

## 11. Supplements

### `supplements`

Supplement product catalog (user-owned).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `name` | `VARCHAR` NOT NULL | |
| `form` | `VARCHAR` | `'CAPSULE'`, `'POWDER'`, `'LIQUID'`, etc. |
| `default_dose_mg` | `DECIMAL` | |
| `notes` | `TEXT` | |

---

### `supplement_plans`

A named protocol / stack (e.g., "Morning stack").

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `name` | `VARCHAR` NOT NULL | |
| `active` | `BOOLEAN` DEFAULT true | |
| `start_date` | `DATE` | |
| `end_date` | `DATE` | Null = ongoing |

**Indexes:** `(user_id)`

---

### `supplement_plan_entries`

Which supplements are in a plan and at what dose.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `plan_id` | `UUID` FK → `supplement_plans` | |
| `supplement_id` | `UUID` FK → `supplements` | |
| `dose_mg` | `DECIMAL` | Overrides supplement default |
| `frequency` | `VARCHAR` | E.g. `'DAILY'`, `'WITH_MEALS'` |
| `time_of_day` | `VARCHAR` | E.g. `'MORNING'`, `'PRE_WORKOUT'` |

**Indexes:** `(plan_id)`, `(supplement_id)` — added in V13 (missing FK indexes fix)

---

### `supplement_logs`

Daily adherence / intake log.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `supplement_id` | `UUID` FK → `supplements` | |
| `plan_entry_id` | `UUID` FK → `supplement_plan_entries` | Nullable — ad-hoc logs allowed |
| `taken_at` | `TIMESTAMP` NOT NULL | |
| `dose_mg` | `DECIMAL` | Actual dose taken |
| `notes` | `TEXT` | |

**Indexes:** `(user_id, taken_at)`, `(supplement_id)`, `(plan_entry_id)` — V13

---

## 12. AI Features

### `ai_settings`

Per-user AI configuration (V18).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` UNIQUE | |
| `encrypted_api_key` | `TEXT` | User's own OpenAI/Claude key, encrypted at rest |
| `model_preference` | `VARCHAR` | E.g. `'gpt-4o'`, `'claude-3-7-sonnet'` |
| `updated_at` | `TIMESTAMP` | |

---

### `ai_insights`

AI-generated periodic insights (V18).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `insight_type` | `VARCHAR` NOT NULL | E.g. `'WEEKLY_SUMMARY'`, `'NUTRITION_REVIEW'`, `'OVERTRAINING_RISK'` |
| `period_start` | `DATE` | |
| `period_end` | `DATE` | |
| `content` | `TEXT` NOT NULL | Markdown / plain text AI output |
| `generated_at` | `TIMESTAMP` | |

**Indexes:** `(user_id, insight_type, period_start)`

---

### `ai_chat_history`

Persistent chat messages per user conversation (V18).

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `conversation_id` | `UUID` NOT NULL | Groups messages into one conversation |
| `role` | `VARCHAR` NOT NULL | `'USER'` or `'ASSISTANT'` |
| `content` | `TEXT` NOT NULL | |
| `created_at` | `TIMESTAMP` | |

**Indexes:** `(user_id, conversation_id, created_at)`

---

## 13. Analytics Aggregates

### `monthly_exercise_aggregates`

Pre-computed monthly rollups per exercise definition (V18). Used to give AI context without scanning raw `exercises` rows.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | |
| `user_id` | `UUID` FK → `users` | |
| `exercise_definition_id` | `UUID` FK → `exercise_definitions` | |
| `month` | `DATE` | First day of month |
| `total_volume_kg` | `DECIMAL` | Sum of sets × reps × weight |
| `total_sets` | `INT` | |
| `total_reps` | `INT` | |
| `max_weight_kg` | `DECIMAL` | Best set weight |
| `max_e1rm` | `DECIMAL` | Best estimated 1RM (Epley formula) |
| `session_count` | `INT` | Distinct sessions this month |
| `updated_at` | `TIMESTAMP` | |

**Indexes:** `(user_id, month)`, `(user_id, exercise_definition_id, month)` UNIQUE

---

## 14. Index Summary

| Table | Index Columns | Type | Purpose |
|-------|--------------|------|---------|
| `users` | `email` | UNIQUE | Login lookup |
| `workout_sessions` | `user_id` | B-tree | Session list per user |
| `workout_sessions` | `training_plan_id` | B-tree | Plan → sessions join |
| `exercises` | `workout_session_id` | B-tree | Session → exercises join |
| `exercises` | `exercise_definition_id` | B-tree | Definition → exercise link |
| `workout_sets` | `exercise_id` | B-tree | Exercise → sets join |
| `exercise_definitions` | `user_id` | B-tree | User custom definitions |
| `health_metrics` | `(user_id, date)` | B-tree | Time-series queries |
| `health_metrics` | `(user_id, source, metric_key)` | B-tree | Metric-specific queries |
| `fitatu_food_logs` | `(user_id, log_date)` | B-tree | Daily nutrition lookup |
| `fitatu_food_logs` | `nutrients` | GIN | JSONB arbitrary queries |
| `blood_test_reports` | `user_id` | B-tree | Report list per user |
| `blood_test_results` | `report_id` | B-tree | Report → results join |
| `withings_tokens` | `user_id` | UNIQUE | One token set per user |
| `supplement_plans` | `user_id` | B-tree | Plan list per user |
| `supplement_plan_entries` | `plan_id` | B-tree | Plan → entries join (V13) |
| `supplement_plan_entries` | `supplement_id` | B-tree | FK index (V13) |
| `supplement_logs` | `(user_id, taken_at)` | B-tree | Adherence timeline |
| `supplement_logs` | `supplement_id` | B-tree | FK index (V13) |
| `supplement_logs` | `plan_entry_id` | B-tree | FK index (V13) |
| `ai_insights` | `(user_id, insight_type, period_start)` | B-tree | Insight dedup + lookup |
| `ai_chat_history` | `(user_id, conversation_id, created_at)` | B-tree | Chat thread retrieval |
| `monthly_exercise_aggregates` | `(user_id, month)` | B-tree | Monthly AI context |
| `monthly_exercise_aggregates` | `(user_id, exercise_definition_id, month)` | UNIQUE | Upsert dedup |

---

## 15. Access Patterns by Feature

### Progressive Overload Chart
```
GET /analytics/progressive-overload/{exerciseDefinitionId}?from=2025-01-01&to=2025-04-01
GET /analytics/progressive-overload/{exerciseDefinitionId}?sessions=12
```
- `ExerciseRepository.findByUserIdAndDefinitionIdBetween(userId, definitionId, from, to)` — JPQL with `CAST(ws.date AS LocalDate) BETWEEN :from AND :to`, ASC order
- `ExerciseRepository.findByUserIdAndDefinitionId(userId, definitionId, Pageable)` — DESC, limited by page size
- For each exercise: computes e1RM via Epley formula `weight × (1 + reps/30)`, tracks running max for PR detection, computes volume delta vs previous session
- Falls back to `workout_sets` when exercise-level reps/weight are null

### Weekly Muscle Volume Chart
```
GET /analytics/volume/weekly?from=2025-01-01&to=2025-04-01&muscle=CHEST
```
- Native SQL query with `DATE_TRUNC('week', ws.date)` GROUP BY `(week_start, primary_muscle)`
- Volume = `SUM(sets × reps × weight_kg)` — first tries exercise-level fields, falls back to `workout_sets` subquery
- Filtered by `primary_muscle` when `muscle` param provided

### Session Volume Summary
```
GET /analytics/sessions/{sessionId}/volume
```
- Fetches all exercises for session (owner-checked), aggregates per-muscle volume

### Exercise Definition Search
```
GET /exercises/definitions?q=bench&muscle=CHEST
```
- Filters by `LOWER(name) LIKE LOWER('%q%')` and/or `primary_muscle = muscle`
- Returns system + user-owned definitions

### Auto-Link Exercise → Definition
- Triggered after workout save: `bulkAutoLinkByUserId(userId)` native SQL
- Matches `LOWER(exercise.name) = LOWER(definition.name)` for `is_system = true` definitions
- Updates `exercise_definition_id` and `primary_muscle` on `exercises` in bulk

### Withings Sync
- Check `withings_tokens` by `user_id` → refresh if `expires_at` past
- Call Withings API → insert rows into `health_metrics` with `source = 'WITHINGS'`
- Metrics stored: weight, fat_ratio, fat_mass_weight, fat_free_mass, muscle_mass, bone_mass, hydration, pulse_wave_velocity, heart_rate

### Fitatu Import
- Parse CSV → bulk insert into `fitatu_food_logs` with JSONB `nutrients`
- Deduplicated on `(user_id, log_date, meal_name, product_name)` (upsert or skip)

### AI Insights Generation
- Reads `monthly_exercise_aggregates` for training context
- Reads `fitatu_food_logs` daily sums for nutrition context
- Reads `health_metrics` for body composition trends
- Reads `blood_test_results` for flagged biomarkers
- Writes result to `ai_insights (user_id, insight_type, period_start, period_end)`

---

## Schema Migration History (Flyway)

| Version | Description |
|---------|-------------|
| V1 | Core tables: users, measurements, workout_sessions, exercises, progress_photos |
| V2 | Training plans: training_plans, planned_exercises |
| V3 | Media: media_attachments |
| V4 | Flexible measurements: optional weight, renamed columns |
| V5 | User unit preference (`unit_preference` on users) |
| V6 | Withings OAuth: withings_tokens, withings_measurements |
| V7 | Google OAuth support on users |
| V8 | Extended Withings metrics (pulse_wave_velocity) |
| V9 | Drop withings_measurements → create health_metrics EAV |
| V10 | Nutrition: fitatu_food_logs with JSONB nutrients |
| V11 | Blood tests: blood_test_reports, blood_test_results |
| V12 | Supplements: supplements, supplement_plans, supplement_plan_entries, supplement_logs |
| V13 | Fix missing FK indexes on supplement tables |
| V14 | Exercise catalog: exercise_definitions + ~80 system exercises seeded |
| V15 | Link exercises to definitions: exercise_definition_id, rpe, primary_muscle on exercises |
| V16 | Session timing: started_at/finished_at on workout_sessions; definition FK on planned_exercises |
| V17 | Individual sets: workout_sets table |
| V18 | AI features: ai_settings, ai_insights, ai_chat_history, monthly_exercise_aggregates |
