## ADDED Requirements

### Requirement: AiTool interface contract
The system SHALL define an `AiTool` interface with four methods: `name()` returning a snake_case String, `description()` returning a natural-language String for the model, `inputSchema()` returning a JSON Schema as `Map<String,Object>`, and `execute(Map<String,Object> args, UUID userId)` returning a domain result object.

#### Scenario: Tool name is snake_case
- **WHEN** any registered tool's `name()` is called
- **THEN** the returned string matches the pattern `[a-z][a-z0-9_]*` (lowercase snake_case)

#### Scenario: Tool executes with user scope
- **WHEN** `execute(args, userId)` is called on any tool
- **THEN** the tool queries only data belonging to the provided `userId` and never returns data for other users

### Requirement: AiToolRegistry auto-registration
The system SHALL define an `AiToolRegistry` Spring component that collects all `AiTool` beans via `List<AiTool>` constructor injection. The registry SHALL provide lookup by name and conversion of all tools to `AiToolDefinition` objects for inclusion in `AiRequest`.

#### Scenario: All tools discoverable by name
- **WHEN** `AiToolRegistry.find("get_exercise_history")` is called
- **THEN** the matching `AiTool` bean is returned without the caller knowing the concrete class

#### Scenario: New tool auto-registered
- **WHEN** a new class annotated with `@Component` implements `AiTool`
- **THEN** `AiToolRegistry` includes it automatically without any manual registration code

### Requirement: get_exercise_history tool
The system SHALL implement a tool named `get_exercise_history` that accepts `exerciseName` (String) and optional `limitWeeks` (integer, default 12) parameters. It SHALL return the list of sessions for that exercise including date, sets, reps, and weight, ordered by date ascending.

#### Scenario: Returns ordered progression data
- **WHEN** the model invokes `get_exercise_history` with `{"exerciseName": "Bench Press", "limitWeeks": 8}`
- **THEN** the tool returns sessions from the last 8 weeks for the current user, ordered oldest to newest

### Requirement: get_personal_records tool
The system SHALL implement a tool named `get_personal_records` that accepts an optional `exerciseName` filter. It SHALL return the all-time personal record (max weight) per exercise, including the date it was achieved.

#### Scenario: Returns PR per exercise
- **WHEN** the model invokes `get_personal_records` with no filter
- **THEN** the tool returns one record per exercise containing `exerciseName`, `maxWeightKg`, and `achievedDate`

### Requirement: get_volume_stats tool
The system SHALL implement a tool named `get_volume_stats` that accepts `periodWeeks` (integer, default 4). It SHALL return total volume load (sets × reps × weight) grouped by muscle group for the specified period.

#### Scenario: Volume grouped by muscle
- **WHEN** the model invokes `get_volume_stats` with `{"periodWeeks": 4}`
- **THEN** the tool returns a map of muscle group name to total volume in kg for the last 4 weeks

### Requirement: get_monthly_aggregates tool
The system SHALL implement a tool named `get_monthly_aggregates` that accepts `exerciseName` (String) and `months` (integer, default 12). It SHALL query the `monthly_exercise_aggregates` table and return pre-computed monthly stats: max weight, total volume, session count.

#### Scenario: Returns pre-computed aggregates
- **WHEN** the model invokes `get_monthly_aggregates` with `{"exerciseName": "Squat", "months": 6}`
- **THEN** the tool returns up to 6 monthly rows from `monthly_exercise_aggregates` with no on-the-fly aggregation SQL

### Requirement: get_recent_workouts tool
The system SHALL implement a tool named `get_recent_workouts` that accepts `limit` (integer, default 5). It SHALL return the most recent workout sessions including date, name, and top exercises performed.

#### Scenario: Returns recent sessions
- **WHEN** the model invokes `get_recent_workouts` with `{"limit": 3}`
- **THEN** the tool returns the 3 most recent workout sessions with their exercises for the current user

### Requirement: get_blood_results tool
The system SHALL implement a tool named `get_blood_results` that accepts an optional `reportId` (UUID). If `reportId` is omitted, it SHALL return results from the most recent blood test report.

#### Scenario: Returns most recent blood report
- **WHEN** the model invokes `get_blood_results` with no parameters
- **THEN** the tool returns all marker results from the user's most recent blood test report

### Requirement: get_measurements tool
The system SHALL implement a tool named `get_measurements` that accepts `limitWeeks` (integer, default 8). It SHALL return body measurement entries (weight, body fat percentage, etc.) ordered by date ascending.

#### Scenario: Returns measurement history
- **WHEN** the model invokes `get_measurements` with `{"limitWeeks": 4}`
- **THEN** the tool returns body measurements from the last 4 weeks ordered chronologically

### Requirement: Determinism rule — tools return pre-computed numbers
Every tool's `execute()` method SHALL compute all numerical results in Java or SQL before returning. Tools SHALL never return raw, unaggregated row lists and instruct the model to sum or average them. The AI interprets results; it does not compute them.

#### Scenario: Volume tool returns computed total
- **WHEN** `get_volume_stats` executes
- **THEN** the response contains a numeric total volume value computed by a SQL aggregate, not a list of individual sets for the model to sum

### Requirement: Tool call loop protection
The system SHALL enforce a maximum of 5 tool invocations per single chat request in `AiChatService`. After the limit is reached, the service SHALL inject a system message instructing the model to answer with data already gathered.

#### Scenario: Tool limit reached
- **WHEN** a model response triggers a 6th tool call in one request
- **THEN** the system does not execute the 6th tool and instead sends the model a message: `"Please answer with the data you have gathered."`
