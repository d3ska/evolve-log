# Test Coverage Report

## Summary

| Domain | Unit Tests | Controller Tests | Integration Tests | Status |
|--------|-----------|-----------------|-------------------|--------|
| Supplements (catalog, plan, log) | ✅ 14 tests | ✅ 6 tests | — | **Covered (pilot)** |
| Workouts | ❌ | ❌ | — | Not covered |
| Training Plans | ❌ | ❌ | — | Not covered |
| Measurements | ❌ | ❌ | — | Not covered |
| Blood Tests | ❌ | ❌ | — | Not covered |
| Nutrition (Fitatu CSV) | ❌ | ❌ | — | Not covered |
| Progress Photos | ❌ | ❌ | — | Not covered |
| Media Attachments | ❌ | ❌ | — | Not covered |
| Analytics / Biweekly Reports | ❌ | ❌ | — | Not covered |
| Auth / OAuth2 | ❌ | ❌ | — | Not covered |
| User / Withings / Health Metrics | ❌ | ❌ | — | Not covered |
| GlobalExceptionHandler | ❌ | — | — | Not covered |

**Test run (2026-04-26):** 21 tests total, 20 pass, 1 pre-existing failure (`EvolveLogApplicationTests.contextLoads` — requires a running PostgreSQL instance, no DB available in CI without Testcontainers profile).

---

## Covered: Supplement Domain

### Service layer — 14 unit tests

**`SupplementCatalogServiceTest`** (3 tests)
- `shouldReturnDtoWhenCreatingSupplement`
- `shouldCallDeleteWhenDeletingExistingSupplement`
- `shouldThrow404WhenDeletingSupplementWithUnknownId`

**`SupplementPlanServiceTest`** (6 tests)
- `shouldReturnDtoWhenCreatingPlan`
- `shouldAddEntryToPlanWhenSupplementAndPlanExist`
- `shouldThrow404WhenAddingEntryToUnknownPlan`
- `shouldRemoveEntryFromPlanWhenEntryExists`
- `shouldCallDeleteWhenDeletingExistingPlan`
- `shouldThrow404WhenDeletingPlanWithUnknownId`

**`SupplementLogServiceTest`** (5 tests)
- `shouldLogIntakeAsSpontaneousWhenNoPlanEntryProvided`
- `shouldLogIntakeAsPlannedWhenPlanEntryIdProvided`
- `shouldReturnLogsForDateWhenListingByDate`
- `shouldCallDeleteWhenDeletingExistingLog`
- `shouldThrow404WhenDeletingLogWithUnknownId`

### Controller layer — 6 MockMvc tests (standaloneSetup)

**`SupplementControllerTest`** (6 tests)
- `shouldReturn201WhenCreatingSupplement`
- `shouldReturn200WithListWhenListingSupplements`
- `shouldReturn204WhenDeletingSupplement`
- `shouldReturn201WhenLoggingIntake`
- `shouldReturn200WithLogsWhenListingByDate`
- `shouldReturn204WhenDeletingLog`

---

## Not Covered: Remaining Domains

### Workouts
**Services:** workout session CRUD, exercise management
**Recommended:** Unit tests for session create/update/delete (not-found), exercise list; `WorkoutControllerTest` covering `POST /api/workouts`, `GET /api/workouts`, `PUT /api/workouts/{id}`, `DELETE /api/workouts/{id}` → 204.

### Training Plans
**Services:** plan CRUD, planned exercise add/remove/reorder
**Recommended:** Unit tests for `createPlan`, `addPlannedExercise`, `removePlannedExercise`, `deletePlan` (success + not-found); `TrainingPlanControllerTest` for all plan and planned-exercise endpoints.

### Measurements
**Services:** measurement create/list/update/delete, trend calculation
**Recommended:** Unit tests for CRUD operations and not-found paths; `MeasurementControllerTest` covering `POST /api/measurements`, `GET /api/measurements`, `DELETE /api/measurements/{id}` → 204.

### Blood Tests
**Services:** CSV parsing (`BloodTestCsvParser`), report creation, result list, history
**Recommended:** Unit tests for `BloodTestCsvParser` (valid CSV, malformed row, empty file); unit tests for `BloodTestService` (uploadReport, getReport not-found, deleteReport); `BloodTestControllerTest` for upload, list, detail, delete endpoints.
Note: `ParameterCatalog` contains pure lookup logic — high-value, low-effort unit test target.

### Nutrition (Fitatu CSV)
**Services:** `FitatuCsvParser`, food log list by date
**Recommended:** Unit tests for `FitatuCsvParser` (valid CSV, missing columns, duplicate rows); `NutritionControllerTest` for `POST /api/nutrition/import` and `GET /api/nutrition/logs`.

### Progress Photos
**Services:** `PhotoService` — upload, list, delete
**Recommended:** Unit tests mocking `StorageService`; `PhotoControllerTest` for upload (multipart), list, delete → 204.

### Media Attachments
**Services:** `MediaAttachmentService` — attach to entity, list, delete
**Recommended:** Unit tests per attachment type; `MediaAttachmentControllerTest` covering the attach/list/delete endpoints.

### Analytics / Biweekly Reports
**Services:** `AnalyticsService` (personal records, exercise progress), `BiweeklyReportService`
**Recommended:** Unit tests for PR calculation edge cases (no sessions, single session, multiple sessions); biweekly report generation with date range boundary conditions; `AnalyticsControllerTest` for all analytics endpoints.

### Auth / OAuth2
**Services:** `AuthService`, `CustomOAuth2User`, `CustomOidcUser`
**Recommended:** Unit tests for `AuthService.findOrCreateUser` (existing user, new user); `CustomOAuth2User` / `CustomOidcUser` attribute extraction; `AuthControllerTest` for `/api/auth/me` → 200 with principal resolver, unauthenticated → 401.

### User / Withings / Health Metrics
**Services:** `HealthMetricService`, `WithingsMetricProvider`, user preferences update
**Recommended:** Unit tests for `HealthMetricService.getMetricsForDate` (Withings provider returns data, no data found); `WithingsControllerTest` for token exchange and status endpoints; `UserControllerTest` for `GET /api/users/me` and `PATCH /api/users/me/preferences`.

### GlobalExceptionHandler
**Recommended:** Standalone MockMvc test that triggers each handler path: `ResponseStatusException` → correct status + `ApiResponse.error`, `MethodArgumentNotValidException` → 400 with field errors, uncaught `Exception` → 500 with generic message. No application context needed.

---

## Known Issues

- `EvolveLogApplicationTests.contextLoads` fails without a running PostgreSQL instance. This is a pre-existing issue unrelated to supplement tests. To fix: annotate with `@Testcontainers` and start a `PostgreSQLContainer`, or exclude the test from the default Gradle task and run it only in a CI environment where a database is available.
