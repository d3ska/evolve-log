# Spec: Frontend Quality

## Purpose

Defines error handling and type safety standards for the React frontend, ensuring
all user interactions surface errors visibly and the API layer uses explicit TypeScript types.

## Requirements

### Requirement: All async form/modal submissions handle errors explicitly
The system SHALL display a user-visible error message when any API call from a modal or form
fails, across all pages (supplements, workouts, measurements, blood tests, nutrition, photos).

#### Scenario: API failure on any modal submit
- **WHEN** any modal submit handler's API call throws or rejects
- **THEN** the modal remains open and displays an inline error message
- **THEN** the modal does NOT silently close or leave the user with no feedback

#### Scenario: Network error
- **WHEN** the backend is unreachable during any form submission
- **THEN** the user sees a meaningful error message (not a blank screen or frozen button)

### Requirement: No unhandled promise rejections from user interactions
The system SHALL ensure that all `async` event handlers in the frontend are wrapped in
`try/catch` or have `.catch()` attached, preventing silent failures and unhandled rejection warnings.

#### Scenario: Delete action fails
- **WHEN** a delete button is clicked and the API call fails
- **THEN** the item is not removed from the UI and an error is surfaced to the user

### Requirement: TypeScript `any` is not used in API layer or component props
The system SHALL not use the `any` type in `src/api/**` or in component prop interfaces.
Proper types SHALL be derived from API response shapes.

#### Scenario: API function return types
- **WHEN** any function in `src/api/` is reviewed
- **THEN** all parameters and return types are explicitly typed (no implicit or explicit `any`)
