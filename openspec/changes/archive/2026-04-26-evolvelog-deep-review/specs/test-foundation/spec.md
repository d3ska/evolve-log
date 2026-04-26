## ADDED Requirements

### Requirement: Supplement domain has ≥80% unit test coverage (pilot)
The system SHALL have JUnit 5 unit tests for all three supplement services
(`SupplementCatalogService`, `SupplementPlanService`, `SupplementLogService`),
using Mockito to mock repositories. This domain serves as the test pattern for all others.

#### Scenario: Happy-path create
- **WHEN** any `create*()` service method is called with valid input
- **THEN** the test asserts the repository `save()` was called and the returned DTO is correct

#### Scenario: Not-found error path
- **WHEN** any service method is called with an ID that does not belong to the user
- **THEN** the test asserts `ResponseStatusException` with 404 is thrown

### Requirement: SupplementController has @WebMvcTest coverage for all endpoints
The system SHALL have MockMvc tests for all `SupplementController` endpoints verifying
status codes, request parsing, and service delegation.

#### Scenario: POST returns 201
- **WHEN** a valid create request is sent to any POST endpoint
- **THEN** the response status is 201

#### Scenario: DELETE returns 204
- **WHEN** a valid DELETE request is sent
- **THEN** the response status is 204 (verifying the status code fix)

### Requirement: Coverage gaps in other domains are documented
The system SHALL include a `TEST_COVERAGE.md` or comment in `tasks.md` listing all domains
not yet covered by automated tests, as a roadmap for subsequent test phases.

#### Scenario: Review after supplement tests are written
- **WHEN** the supplement test suite is complete
- **THEN** a documented list exists of remaining untested controllers and services
