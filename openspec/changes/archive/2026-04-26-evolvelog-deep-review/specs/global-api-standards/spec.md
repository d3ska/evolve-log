## ADDED Requirements

### Requirement: DELETE endpoints return 204 No Content
The system SHALL return HTTP 204 with an empty body for all successful DELETE operations
across all controllers.

#### Scenario: Any successful DELETE
- **WHEN** any `DELETE /api/**` request succeeds
- **THEN** the response status is 204 and the body is empty

### Requirement: All error responses use the standard envelope
The system SHALL return all error responses as `{ "data": null, "error": "<message>" }`
via a single `@ControllerAdvice`, for all controllers.

#### Scenario: Resource not found
- **WHEN** any endpoint returns HTTP 404
- **THEN** the body is `{ "data": null, "error": "<non-empty message>" }`

#### Scenario: Validation failure
- **WHEN** a request body fails `@Valid` constraints
- **THEN** the response is HTTP 400 with `{ "data": null, "error": "<validation message>" }`

#### Scenario: Unexpected server error
- **WHEN** an unhandled exception occurs in any controller
- **THEN** the response is HTTP 500 with `{ "data": null, "error": "Internal server error" }` (no stack trace leaked)

### Requirement: Successful responses use the standard envelope
The system SHALL wrap all successful payloads as `{ "data": <payload>, "error": null }`.

#### Scenario: Successful list response
- **WHEN** any GET list endpoint returns 200
- **THEN** the body is `{ "data": [...], "error": null }`
