# CLAUDE.md — EvolveLog Backend

## Living Document — Self-Update Contract

This file is the **single source of truth** for how this codebase is built.
It must stay in sync with the actual code.

**After every session where any of the following changes, update this file before finishing:**
- A new domain area, aggregate, or major service is added
- A new Flyway migration is created (update the migration table)
- An architecture rule is changed, relaxed, or tightened
- A new OpenSpec change is created or archived
- A new environment variable or external integration is introduced
- An approach is deliberately abandoned in favour of something different

When updating, also update `memory/MEMORY.md` in the project memory folder so the
change survives across conversations.

If a decision contradicts something already written here, **correct the existing section**
rather than appending a note — this file should always reflect current reality, not history.

---

## Project Overview

EvolveLog is a personal fitness & health tracking application.
This repository is the **backend** (Spring Boot + Gradle + PostgreSQL).
The **frontend** lives in the sibling folder `../evolve-log-ui` (React/Vite).

- **Group:** `com.deska`
- **Artifact:** `evolve-log`
- **Java:** 25 (Temurin toolchain)
- **Spring Boot:** 4.x
- **Database:** PostgreSQL 17 via Flyway migrations
- **Deploy:** Hetzner VPS via Docker Compose, images pushed to GHCR

---

## Gradle Commands

```bash
# Build (runs tests, produces JAR)
./gradlew clean build

# Run locally (requires Postgres — use docker-compose first)
./gradlew bootRun

# Run tests only
./gradlew test

# Run tests without daemon (used in CI)
./gradlew test --no-daemon

# Skip tests for a fast build
./gradlew build -x test

# Check dependency tree
./gradlew dependencies
```

---

## Local Development

### Start Postgres

```bash
docker compose up -d postgres
```

The `docker-compose.yml` in the repo root starts Postgres 17 at `localhost:5432`.
Credentials: `evolvelog / evolvelog_dev`, database: `evolvelog`.

### Start the full stack (backend + frontend)

```bash
docker compose up --build
```

Frontend is served at `http://localhost:80`, backend at `http://localhost:8080`.

### Environment variables

All secrets are injected via environment variables. See `application.properties` for the full list.
Copy `src/main/resources/application-local.properties` and fill in:

| Variable | Purpose |
|---|---|
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth login |
| `WITHINGS_CLIENT_ID` / `WITHINGS_CLIENT_SECRET` | Withings scale sync |
| `APP_AI_ENCRYPTION_KEY` | AES-256 key for AI API key storage (exactly 32 bytes, Base64-encoded) |
| `APP_FRONTEND_URL` | Frontend origin for CORS & OAuth redirects |

---

## Package Structure

```
com.deska.evolvelog
├── config/          — Spring configuration classes
├── controller/      — REST controllers (one per aggregate)
├── domain/          — JPA entities and enums
├── dto/
│   ├── request/     — Validated inbound DTOs
│   └── response/    — Outbound DTOs
├── exception/       — ApiException, ResourceNotFoundException, GlobalExceptionHandler
├── repository/      — Spring Data JPA repositories
├── security/        — Spring Security config, OAuth2 user details
├── service/         — Business logic (one service class per aggregate root)
└── storage/         — StorageService abstraction + LocalStorageService
```

---

## Architecture Rules

### DDD (Domain-Driven Design)
- Each **service class** handles exactly **one aggregate root**. Never let a single service touch repositories from multiple aggregates.
- **Entities are immutable after construction.** No public setters, no Lombok `@Setter`/`@Data` on `@Entity` classes. Use constructors or builders; use JPA merge semantics for updates.
- Domain logic lives in service classes, not controllers.

### Layering
- Controllers call services; services call repositories. Controllers never call repositories directly.
- DTOs cross the controller↔service boundary. Entities never escape the service layer into controllers.

### API Standards (enforced by `GlobalExceptionHandler`)
- **All responses** are wrapped: `{ "data": <payload>, "error": null }` on success.
- **All errors** use: `{ "data": null, "error": "<message>" }`.
- Successful **DELETE** operations return **HTTP 204** with an empty body.
- Validation failures return **HTTP 400** with a descriptive message; stack traces are never leaked.

### Request DTOs
- Every required field must be annotated with Bean Validation constraints (`@NotBlank`, `@NotNull`, `@Size`, etc.).

### Data Access
- Prevent N+1 queries: use `JOIN FETCH` or `@EntityGraph` for any `@OneToMany` / nested `@ManyToOne` that is included in an API response.
- All date-range filters interpret parameters as **UTC calendar day boundaries** (`date 00:00:00Z` inclusive, `date+1 00:00:00Z` exclusive).
- `spring.jpa.hibernate.ddl-auto=none` — Hibernate never touches the schema. Flyway owns all DDL.
- `spring.jpa.open-in-view=false` — No open session in view.

---

## OpenSpec (Spec-Driven Development)

This project uses **OpenSpec** for feature development. Before coding any new feature:

1. Create a change under `openspec/changes/<change-name>/` with `.openspec.yaml`, `proposal.md`, `design.md`, `specs/`, and `tasks.md`.
2. Specs live in `openspec/specs/` (promoted from changes after archiving).
3. Implement **only** what the spec describes.
4. Archive the change (`openspec/changes/archive/`) once shipped.

Active changes (in-progress):
- `openspec/changes/training-volume-analytics/`
- `openspec/changes/workout-session-flow/`
- `openspec/changes/ai-personal-trainer/`
- `openspec/changes/ai-tool-training-plan/` (implemented — ready to archive)
- `openspec/changes/ai-tools-new-data-sources/` (implemented — ready to archive)
- `openspec/changes/ai-tools-quality-fixes/` (implemented — ready to archive)
- `openspec/changes/ai-user-goals/` (implemented — ready to archive)
- `openspec/changes/active-workout-module/`
- `openspec/changes/i18n-foundation/`
- `openspec/changes/supplement-ux-redesign/`
- `openspec/changes/training-block/`
- `openspec/changes/plan-session-deviations/`
- `openspec/changes/rest-timer-between-sets/` (implemented — ready to archive)
Archived changes (reference):
- `openspec/changes/archive/2026-04-26-evolvelog-deep-review/`
- `openspec/changes/archive/2026-04-29-manual-workout-analytics-fix/`
- `openspec/changes/archive/2026-04-30-data-sync-idempotency/`
- `openspec/changes/archive/2026-04-30-pwa-assets/`

Promoted specs (always authoritative):
- `openspec/specs/global-api-standards/spec.md`
- `openspec/specs/backend-code-quality/spec.md`
- `openspec/specs/backend-data-layer/spec.md`
- `openspec/specs/test-foundation/spec.md`
- `openspec/specs/manual-workout-analytics/spec.md`
- `openspec/specs/data-sync-idempotency/spec.md`
- `openspec/specs/pwa-cache-busting/spec.md`

---

## Data Sync — Idempotency Rules

**Every data sync operation MUST be idempotent.** Re-running a sync must produce the same result without duplicating rows.

### Rule: Upsert with composite unique keys

| Domain | Table | Composite unique key |
|---|---|---|
| Withings / any device | `health_metrics` | `(user_id, source, date, metric_key)` |
| Fitatu CSV import | `fitatu_food_logs` | `(user_id, date, meal, food_name)` |
| Blood test upload | `blood_test_reports` + `blood_test_results` | `blood_test_reports`: `(user_id, date, lab_name)`; `blood_test_results`: `(report_id, parameter_key)` |
| AI / analytics | `monthly_exercise_aggregates` | `(user_id, exercise_definition_id, month)` |
| AI insights | `ai_insights` | `(user_id, type, period_start)` |

- Use `INSERT … ON CONFLICT (…) DO UPDATE SET …` (PostgreSQL upsert) for all device/external data.
- The `health_metrics` table already has `UNIQUE (user_id, source, date, metric_key)` — always upsert through `HealthMetricService.upsert()`.
- When adding a new data source, **define the composite unique key in the migration first**, then implement the upsert in the service.

---

## Database Migrations (Flyway)

Migrations live in `src/main/resources/db/migration/` and follow strict versioning:

| Version | Description |
|---|---|
| V1 | Initial schema (users, measurements, workout_sessions, exercises, progress_photos) |
| V2 | Training plans & planned exercises |
| V3 | Media attachments |
| V4 | Flexible measurements |
| V5 | User unit preference (METRIC / IMPERIAL) |
| V6 | Withings OAuth tokens + measurements |
| V7 | Google OAuth fields on users |
| V8 | Withings extended metrics |
| V9 | Generic `health_metrics` EAV table (replaces withings_measurements) |
| V10 | `fitatu_food_logs` (CSV import, JSONB nutrients, unique constraint on meal+food_name) |
| V11 | `blood_test_reports` + `blood_test_results` (unique constraints for upsert idempotency) |
| V12 | `supplements`, `supplement_plans`, `supplement_plan_entries`, `supplement_logs` |
| V13 | Missing FK indexes |
| V14 | `exercise_definitions` catalog (system + user, ~92 seeded exercises) |
| V15 | Exercise volume fields |
| V16 | Workout session flow (started_at, finished_at, exercise_definition_id link) |
| V17 | Workout sets |
| V18 | AI feature (ai_settings, ai_insights, ai_chat_history, monthly_exercise_aggregates) |
| V19 | Consolidate exercise data (migrate exercise-level reps/weight into workout_sets) |
| V20 | Active workout module (session status lifecycle, per-set completion flag) |
| V21 | Plan-session deviations (`exercises.planned_exercise_id`, `workout_sessions.plan_snapshot JSONB`) |
| V22 | Rest timer (`workout_sets.completed_at TIMESTAMPTZ` nullable — stamped server-side on set completion) |
| V23 | Plan versioning (`training_plans.current_version`, `training_plan_versions` table, `workout_sessions.plan_version`) |
| V24 | AI goals (`ai_settings.goals TEXT` nullable — free-text fitness goals injected into every AI system prompt) |
| V25 | i18n foundation (`users.locale`, `exercise_definition_translations` table, PL/EN seed data) |
| V26 | Gemini provider support (`ai_chat_history.provider VARCHAR(50)` nullable — analytics-only) |

**Migration rules:**
- Never modify an existing migration. Always add a new versioned file.
- Never use `spring.jpa.hibernate.ddl-auto` to alter the schema.
- Always include covering indexes for FK columns used in WHERE clauses.

---

## Domain Areas

| Area | Key entities | Notes |
|---|---|---|
| Users & Auth | `users`, Google OAuth, JWT-less session | Google OAuth via Spring Security |
| Measurements | `measurements` | Flexible body metrics (weight, BF%, limbs, custom) |
| Workout | `workout_sessions`, `exercises`, `workout_sets`, `exercise_definitions`, `planned_exercises` | Sessions → exercises → sets hierarchy; exercise_definition_id auto-linked by name match; `plan_snapshot` JSONB captures planned exercises at session start; `exercises.planned_exercise_id` links each exercise back to its planned counterpart |
| Training Plans | `training_plans`, `planned_exercises`, `training_blocks` | Templates for workouts; blocks group plans into training cycles |
| Health Metrics | `health_metrics` | Generic EAV: `(user_id, source, date, metric_key, value)` |
| Withings | `withings_tokens` | OAuth token storage; sync via `WithingsMetricProvider` |
| Fitatu | `fitatu_food_logs` | CSV-imported nutrition data, JSONB nutrients |
| Blood Tests | `blood_test_reports`, `blood_test_results` | CSV upload, per-parameter history |
| Supplements | `supplements`, `supplement_plans`, `supplement_plan_entries`, `supplement_logs` | Three services: Catalog, Plan, Log |
| Progress Photos | `progress_photos`, `media_attachments` | Local storage, max 200MB per request |
| AI Trainer | `ai_settings`, `ai_insights`, `ai_chat_history`, `monthly_exercise_aggregates` | User-supplied API key (AES-256 encrypted); `goals` TEXT field injected into every system prompt; tools: `get_recent_workouts`, `get_measurements`, `get_training_plan`, `get_supplement_info`, `get_health_metrics`, `get_nutrition_summary`, `get_blood_test_results`; providers: `ClaudeAdapter` (anthropic), `GeminiAdapter` (google); provider abstracted via `AiProvider` interface + `ModelRouter` registry; `ModelTier` enum drives model selection |

---

## Key Analytics Patterns

### Progressive Overload
- Query: `exercises` filtered by `(user_id, exercise_definition_id, date range)`, ordered ASC.
- e1RM via **Epley formula**: `weight × (1 + reps / 30)`. Running max tracked per session for PR detection.
- Volume delta computed vs. previous session.
- Falls back to `workout_sets` rows when exercise-level `reps`/`weight_kg` are null (post-V17 data).

### Weekly Muscle Volume
- Native SQL with `DATE_TRUNC('week', ws.date)` GROUP BY `(week_start, primary_muscle)`.
- Volume = `SUM(sets × reps × weight_kg)` — tries exercise-level fields first, falls back to `workout_sets` subquery.

### Auto-Link Exercises → Definitions
- Triggered after workout save: bulk native SQL matching `LOWER(exercise.name) = LOWER(definition.name)` where `is_system = true`.
- Sets `exercise_definition_id` and `primary_muscle` on `exercises` rows in-place.
- Never blocks the save; runs as a best-effort enrichment step.

### Health Metrics (Withings)
Known metric keys stored in `health_metrics` (source = `'withings'`):
`weight_kg`, `fat_free_mass_kg`, `body_fat_percent`, `fat_mass_weight_kg`, `heart_pulse_bpm`,
`muscle_mass_kg`, `hydration_kg`, `bone_mass_kg`, `pulse_wave_velocity`, `vo2_max`,
`vascular_age`, `nerve_health_score`, `visceral_fat`, `basal_metabolic_rate`, `metabolic_age`.

---

## i18n

The application supports EN and PL locales (i18n-foundation change, V25 migration).

### How it works

- `users.locale` stores the user's preferred locale (`'en'` or `'pl'`, default `'en'`).
- `LocaleInterceptor` (HandlerInterceptor) resolves locale per request from the authenticated user, stores it in a `@RequestScope` `LocaleContextHolder` bean.
- `ExerciseDefinitionTranslation` table (`exercise_definition_translations`) holds per-locale name overrides for system exercises. User-created exercises always use their raw name.
- `ExerciseDefinitionService.listDefinitions()` uses a 2-query approach: load all definitions, load all translations for the current locale into a Map, merge in-memory.
- `MuscleI18n` static utility maps raw muscle keys (e.g. `"chest"`) to locale labels (e.g. `"Klatka piersiowa"` for PL). Falls back to EN for unknown locales, returns raw key for unknown muscle keys.
- `PUT /api/user/me/locale` — update locale preference; accepts only `"en"` or `"pl"` (validated with `@Pattern`).

### Areas NOT yet translated (future work)
- Health metric keys / labels
- Blood test parameter labels (`blood_test_results.parameter_label`)
- Supplement forms, time slots, and other enum-backed display strings

---

## Git Commit Format

```
<type>: <brief description>
```

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

- No co-author lines, no "Generated by Claude" attribution
- Description is lowercase, imperative mood, no trailing period
- Example: `feat: add workout session status endpoint`

---

## CI/CD

Pipeline: `.github/workflows/deploy.yml`

1. **test** — runs `./gradlew test --no-daemon` against a real Postgres 16 container
2. **build-and-push** — builds backend and frontend Docker images, pushes to GHCR (`ghcr.io/<owner>/evolve-log-backend`, `ghcr.io/<owner>/evolve-log-frontend`)
3. **deploy** — SSH into Hetzner VPS, pulls images, restarts `docker-compose.prod.yml`

Triggers: push to `main`, or manual `workflow_dispatch`.

Required secrets: `REPO_PAT`, `SERVER_IP`, `SERVER_USER`, `SSH_PRIVATE_KEY`.

---

## Testing

- Tests use **Testcontainers** (JUnit 5 + `testcontainers-postgresql`) — a real Postgres container is spun up per test run.
- `spring-security-test` is available for mocking authentication in integration tests.
- Target: **80%+ coverage** on service classes.
- Always write tests before implementation (TDD: red → green → refactor).
