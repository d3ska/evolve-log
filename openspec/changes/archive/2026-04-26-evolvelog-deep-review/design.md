## Context

EvolveLog is a Spring Boot 4 / Java 25 backend (12 Flyway migrations, 10+ controllers) with a
React + TypeScript frontend. Domains: auth, workouts, training plans, measurements, photos,
media attachments, supplements, Withings integration, blood tests, nutrition, analytics.

Review order: **Backend first, then Frontend**. Backend changes are isolated server-side;
frontend changes depend on correct API behaviour being established first.

Known cross-cutting problems:
- All DELETE endpoints return 200 instead of 204
- No single `@ControllerAdvice` — error format varies per controller
- Foreign key columns across multiple tables lack indexes
- N+1 query patterns exist wherever plan/entry/result collections are loaded (supplements, workouts, blood tests)
- Large service classes mix multiple responsibilities
- Entity classes have `@Setter` (uncontrolled mutation)
- Frontend: bare `await` calls in modal submits across all pages — errors are invisible to users
- Zero automated test coverage

## Goals / Non-Goals

**Goals:**
- Consistent HTTP status codes and error response envelope across the entire API
- Eliminate N+1 queries in all domains that load collections of related entities
- Add missing DB indexes across all tables (one V13 migration)
- Split any service class exceeding ~150 lines or handling multiple aggregates
- Remove `@Setter` from all entity classes
- Add `catch` + user-visible error states to all frontend async operations
- Establish a test suite with the supplement domain as the pilot

**Non-Goals:**
- Security hardening (deferred to pre-deployment phase)
- Changing authentication mechanism
- Adding pagination to all endpoints (document gaps; implement only where critical)
- E2E / Playwright tests (deferred to a future phase)
- Refactoring the Withings or Fitatu integration internals

## Decisions

### 1. Backend before Frontend
Backend API contract is stabilised (204 DELETEs, consistent error envelope) before frontend
error handling is written. Avoids reworking frontend error paths twice.

### 2. Single `@ControllerAdvice` for all error handling
One `GlobalExceptionHandler` handles `ResponseStatusException`, `MethodArgumentNotValidException`,
and `Exception`. All error responses use `ApiResponse { data: null, error: "..." }`.
Per-controller `@ExceptionHandler` methods are removed.

### 3. `@EntityGraph` for N+1 elimination (over JPQL JOIN FETCH)
Annotations on repository methods keep query strategy out of the service layer.
Applied wherever a `@OneToMany` or `@ManyToOne` collection is accessed in a serialised response.

### 4. V13 migration — indexes only, additive
`CREATE INDEX IF NOT EXISTS` statements covering all FK columns identified during review.
No destructive schema changes. Tables to audit: all tables with `user_id`, `plan_id`,
`session_id`, `entry_id`, `report_id` foreign keys.

### 5. Service split criterion: one aggregate root per service
Any service handling more than one top-level aggregate is split.
Supplement domain is the first candidate (`SupplementService` → catalog / plan / log).
Other domains are audited and split if they meet the same criterion.

### 6. Frontend error handling pattern: local error state per modal
Each modal/form component holds an `error: string | null` state.
`catch (e)` sets the error; the UI renders it inline. Modal does not close on error.
No global toast system introduced in this review cycle.

### 7. Test pilot: supplement domain first
The supplement domain was most recently developed and has the clearest service boundaries
post-split. It serves as the pattern for testing other domains in subsequent cycles.

## Risks / Trade-offs

- [Service split] More service classes → more constructor injection in controllers. Mitigated by keeping controllers thin.
- [EntityGraph] May over-fetch if relationships grow. Acceptable vs. lazy-loading bugs in serialisation.
- [V13 index migration] Brief write lock on large tables. Negligible for a personal app; note for future multi-tenant scaling.
- [Frontend error pattern] Local error state adds boilerplate per modal. Acceptable until a design system is introduced.
