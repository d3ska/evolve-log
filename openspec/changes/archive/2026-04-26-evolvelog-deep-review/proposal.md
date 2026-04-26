## Why

EvolveLog has grown across 12 migration phases covering workouts, measurements, photos,
supplements, Withings integration, blood tests, and nutrition — with zero automated tests
and no structured review. Before adding more features, a full-codebase audit is needed to
address data integrity risks, API inconsistencies, and code quality debt across all domains.
Security will be handled separately as a pre-deployment hardening pass.

## What Changes

### Backend (all domains)
- Fix HTTP status codes on DELETE endpoints across all controllers (200 → 204)
- Add `@ControllerAdvice` for a consistent error envelope across all controllers
- Add missing database indexes on foreign key columns across all tables (V13 migration)
- Resolve N+1 query risks wherever collections are eager-loaded (workouts, supplements, training plans, blood tests)
- Split oversized service classes into focused, single-responsibility services
- Remove Lombok `@Setter` from all entity classes

### Frontend (all pages)
- Add `try/catch` to all modal/form submit handlers that currently swallow errors silently
- Add user-visible error states to all async operations (not just supplements)
- Audit TypeScript `any` usages and tighten types

### Tests
- Write first automated test suite targeting supplement domain as pilot (≥80% line coverage)
- Document coverage gaps in other domains as follow-up work

## Capabilities

### New Capabilities
- `backend-data-layer`: Fix N+1 queries and add missing DB indexes across all domains
- `backend-code-quality`: Improve service cohesion and entity immutability across all services
- `global-api-standards`: Consistent HTTP status codes and error envelope across all controllers
- `frontend-quality`: Explicit error handling and type safety across all pages
- `test-foundation`: First automated test suite (supplement domain pilot, expandable to all)

### Modified Capabilities
- None — no existing spec files yet; this review establishes the baseline

## Impact

- **Backend**: All controllers, all service classes, all entity classes, Flyway migrations
- **Frontend**: All pages and modal components in evolve-log-ui
- **Database**: New Flyway migration V13 for missing indexes (all tables)
- **Dependencies**: Confirm `spring-boot-starter-test` in `build.gradle`
