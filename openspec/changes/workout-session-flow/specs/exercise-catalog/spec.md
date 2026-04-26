## ADDED Requirements

### Requirement: Planned exercise can be linked to exercise definition
The system SHALL allow a `PlannedExercise` to reference an `ExerciseDefinition` via a nullable `exerciseDefinitionId` FK so that plan exercises align with the exercise catalog.

#### Scenario: Create planned exercise with definition link
- **WHEN** user sends a create or update request for a `PlannedExercise` with a valid `exerciseDefinitionId`
- **THEN** system stores the FK and the planned exercise is retrievable with `exerciseDefinitionId` in its DTO

#### Scenario: Create planned exercise without definition link
- **WHEN** user creates a `PlannedExercise` without specifying `exerciseDefinitionId`
- **THEN** system stores the exercise with `exerciseDefinitionId: null` and existing behavior is unchanged

#### Scenario: Definition belonging to another user rejected
- **WHEN** user specifies an `exerciseDefinitionId` that belongs to a different user (non-system definition)
- **THEN** system returns 404
