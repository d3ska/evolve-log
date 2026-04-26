## 1. Backend — API Standards (all controllers)

- [x] 1.1 Change all `DELETE` endpoint handlers across every controller to return `ResponseEntity.noContent().build()` (HTTP 204, empty body)
- [x] 1.2 Audit `GlobalExceptionHandler` (`@ControllerAdvice`) — add or create handlers for `ResponseStatusException`, `MethodArgumentNotValidException`, and fallback `Exception`
- [x] 1.3 Ensure all exception handlers return `ApiResponse.error(message)` with correct HTTP status; no stack trace in response body
- [x] 1.4 Remove any per-controller `@ExceptionHandler` methods that duplicate `GlobalExceptionHandler`

## 2. Backend — Data Layer (all domains)

- [x] 2.1 Audit all `@Entity` classes with `@OneToMany` or `@ManyToOne` relationships that appear in API responses — list N+1 candidates (supplements, workouts/exercises, training plans, blood tests)
- [x] 2.2 Add `@EntityGraph` or `JOIN FETCH` to repository methods for supplement plan + entries + supplement
- [x] 2.3 Add `@EntityGraph` or `JOIN FETCH` for workout session → exercises (if N+1 found) — already present ✓
- [x] 2.4 Add `@EntityGraph` or `JOIN FETCH` for training plan → planned exercises (if N+1 found) — already present ✓
- [x] 2.5 Add `@EntityGraph` or `JOIN FETCH` for blood test report → results (if N+1 found)
- [x] 2.6 Audit all tables for missing indexes on FK columns: `user_id`, `plan_id`, `session_id`, `report_id`, `entry_id`, `plan_entry_id`
- [x] 2.7 Create Flyway migration `V13__add_missing_indexes.sql` with `CREATE INDEX IF NOT EXISTS` for all identified missing indexes

## 3. Backend — Code Quality (all services)

- [x] 3.1 Audit all service classes — identify any exceeding ~150 lines or handling multiple aggregates
- [x] 3.2 Split `SupplementService` into `SupplementCatalogService`, `SupplementPlanService`, `SupplementLogService`
- [x] 3.3 Split any other oversized services found in 3.1 (e.g., if `WorkoutService` or `AnalyticsService` qualify)
- [x] 3.4 Update all controllers to inject the new split services
- [x] 3.5 Remove `@Setter` (and `@Data` if present) from all `@Entity` classes across all domains
- [x] 3.6 Audit all request DTOs — add missing `@NotBlank`, `@NotNull`, `@Size` constraints where fields are required

## 4. Frontend — Error Handling (all pages)

- [x] 4.1 Audit all pages for bare `await` calls inside event handlers with no `try/catch` — list all occurrences (supplements, workouts, measurements, blood tests, nutrition, photos)
- [x] 4.2 Add `error: string | null` state + `catch` block to all supplement modal submit handlers (create supplement, create plan, add entry, log intake, delete actions)
- [x] 4.3 Add error handling to all workout page modal/form submits
- [x] 4.4 Add error handling to all measurement page modal/form submits
- [x] 4.5 Add error handling to remaining pages found in 4.1
- [x] 4.6 Audit `src/api/**` for `any` types — replace with explicit types derived from response shapes

## 5. Testing (supplement domain pilot)

- [x] 5.1 Confirm `spring-boot-starter-test` is present in `build.gradle`
- [x] 5.2 Write `SupplementCatalogServiceTest` — unit tests: `createSupplement` (success), `deleteSupplement` (success + not-found)
- [x] 5.3 Write `SupplementPlanServiceTest` — unit tests: `createPlan`, `addEntry`, `removeEntry`, `deletePlan` (success + not-found)
- [x] 5.4 Write `SupplementLogServiceTest` — unit tests: `logIntake` (SPONTANEOUS + PLANNED source), `listLogsByDate`, `deleteLog` (success + not-found)
- [x] 5.5 Write `SupplementControllerTest` (standaloneSetup MockMvc — `@WebMvcTest` removed in Spring Boot 4.x): `POST /supplements` → 201, `GET /supplements` → 200, `DELETE /supplements/{id}` → 204, `POST /supplements/logs` → 201, `GET /supplements/logs?date=` → 200, `DELETE /supplements/logs/{id}` → 204
- [x] 5.6 Run `./gradlew test` — 20/21 tests pass (1 pre-existing `contextLoads` fails without DB); all supplement domain tests green; coverage gap documented in `TEST_COVERAGE.md`
