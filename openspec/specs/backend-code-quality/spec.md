# Spec: Backend Code Quality

## Purpose

Defines structural and validation standards for service classes and domain entities,
ensuring each service owns exactly one aggregate and that entities remain immutable after construction.

## Requirements

### Requirement: Each service class handles exactly one aggregate root
The system SHALL organise service classes so that each service is responsible for one
top-level domain aggregate. Services handling multiple aggregates SHALL be split.

#### Scenario: Supplement service split
- **WHEN** the supplement domain is reviewed
- **THEN** three services exist: `SupplementCatalogService`, `SupplementPlanService`, `SupplementLogService`
- **THEN** no single service accesses repositories from more than one aggregate

#### Scenario: Other oversized services
- **WHEN** any service class exceeds ~150 lines or handles multiple aggregates
- **THEN** it is split along aggregate boundaries before this change is archived

### Requirement: Entity classes do not expose setters
The system SHALL ensure that no JPA `@Entity` class has public setters (Lombok `@Setter`
or manually written). Entity state SHALL only be set via constructor or builder.

#### Scenario: Attempt to mutate entity after construction
- **WHEN** a developer reviews any entity class
- **THEN** no `@Setter`, `@Data`, or public `setX()` methods are present
- **THEN** state changes use builder pattern or JPA `merge` semantics

### Requirement: Request DTOs validate all required fields
The system SHALL annotate all required request DTO fields with appropriate Bean Validation
constraints (`@NotBlank`, `@NotNull`, `@Size`, etc.) across all domains.

#### Scenario: Missing required field
- **WHEN** a POST or PUT request is sent with a missing required field
- **THEN** the response is HTTP 400 with a descriptive validation error message
