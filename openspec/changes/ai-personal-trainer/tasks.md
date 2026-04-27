## 1. Database Migration

- [ ] 1.1 Create `V10__ai_feature.sql` Flyway migration with `ai_settings`, `ai_insights`, `ai_chat_history`, and `monthly_exercise_aggregates` tables including all indexes and constraints
- [ ] 1.2 Verify migration runs cleanly against local PostgreSQL (`./gradlew bootRun` schema validation)

## 2. AI Provider Abstraction

- [ ] 2.1 Create `AiMessage` record (role: String, content: String) in `ai.provider`
- [ ] 2.2 Create `AiToolDefinition` record (name, description, inputSchema as `Map<String,Object>`) in `ai.provider`
- [ ] 2.3 Create `AiRequest` record (modelId, systemPrompt, messages, tools, temperature) in `ai.provider`
- [ ] 2.4 Create `AiResponse` record (content, inputTokens, outputTokens) in `ai.provider`
- [ ] 2.5 Create `AiStreamSink` interface (onToken, onToolUse, onToolResult, onDone, onError) in `ai.provider`
- [ ] 2.6 Create `AiProvider` interface (complete, stream, providerId) in `ai.provider`
- [ ] 2.7 Implement `ClaudeAdapter` in `ai.provider.anthropic` using `java.net.http.HttpClient` — blocking `complete()` method
- [ ] 2.8 Implement streaming in `ClaudeAdapter.stream()` using `HttpResponse.BodyHandlers.ofLines()` parsing SSE lines from Anthropic Messages API
- [ ] 2.9 Handle tool use blocks in `ClaudeAdapter` streaming: detect `content_block_start` with type `tool_use`, call `AiStreamSink.onToolUse()`
- [ ] 2.10 Write unit tests for `ClaudeAdapter` with a mock HTTP server (WireMock or similar)

## 3. Model Router

- [ ] 3.1 Create `AiTaskType` enum (DAILY_SUMMARY, POST_WORKOUT, WEEKLY_REPORT, BLOOD_ANALYSIS) in `ai.router`
- [ ] 3.2 Define model ID constants class (Haiku, Sonnet, Opus model ID strings) — no inline string literals
- [ ] 3.3 Implement `ModelRouter` `@Component` with `selectModel(AiTaskType)` switch expression
- [ ] 3.4 Implement heuristic chat routing in `ModelRouter.selectModelForChat(String message)`: keyword scan + length check
- [ ] 3.5 Implement Haiku classifier fallback in `ModelRouter` for ambiguous messages (> 80 chars, no clear keywords)
- [ ] 3.6 Write unit tests for `ModelRouter` covering all task types and chat classification paths

## 4. Prompt System

- [ ] 4.1 Create `src/main/resources/prompts/` directory and add prompt text files: `DAILY_SUMMARY.txt`, `WEEKLY_REPORT.txt`, `BLOOD_ANALYSIS.txt`, `CHAT.txt`
- [ ] 4.2 Implement `PromptLoader` `@Component` that reads all prompt files at startup into `Map<AiTaskType, String>` and fails fast if any file is missing
- [ ] 4.3 Implement `PromptContextBuilder` `@Component` that takes `pageContext` String and `userId` and returns a pre-fetched data block injected into the system prompt (calls existing repositories — no new queries needed)
- [ ] 4.4 Write unit tests for `PromptContextBuilder` verifying each page context maps to the correct data fetch

## 5. Encryption

- [ ] 5.1 Implement `EncryptedStringConverter implements AttributeConverter<String, String>` using AES-256-GCM with random 96-bit IV prepended to ciphertext
- [ ] 5.2 Implement `AiEncryptionConfig` `@Configuration` that reads `AI_ENCRYPTION_KEY` from environment, validates 32-byte length, and fails application startup if missing or invalid
- [ ] 5.3 Write unit tests for `EncryptedStringConverter`: encrypt-then-decrypt round trip, tampered ciphertext detection

## 6. Domain Entities and Repositories

- [ ] 6.1 Implement `AiSettings` `@Entity` with `@Convert(converter = EncryptedStringConverter.class)` on `apiKeyEncrypted` field
- [ ] 6.2 Implement `AiInsight` `@Entity` with all fields matching the V10 schema
- [ ] 6.3 Implement `AiChatMessage` `@Entity` with `userId`, `conversationId`, `role`, `content`, `createdAt`
- [ ] 6.4 Implement `MonthlyExerciseAggregate` `@Entity` with all fields matching the V10 schema
- [ ] 6.5 Implement `AiSettingsRepository extends JpaRepository<AiSettings, UUID>`
- [ ] 6.6 Implement `AiInsightRepository` with `findByUserIdAndTypeOrderByGeneratedAtDesc(UUID userId, String type, Pageable pageable)`
- [ ] 6.7 Implement `AiChatHistoryRepository` with `findTop10ByUserIdAndConversationIdOrderByCreatedAtDesc(UUID userId, UUID conversationId)`
- [ ] 6.8 Implement `MonthlyAggregateRepository` with native UPSERT `@Modifying @Query` and `findByUserIdAndExerciseName...` lookup

## 7. AI Tools

- [ ] 7.1 Create `AiTool` interface (name, description, inputSchema, execute) in `ai.tools`
- [ ] 7.2 Implement `AiToolRegistry` `@Component` collecting all `AiTool` beans via `List<AiTool>` injection, with `find(String name)` and `toDefinitions()` methods
- [ ] 7.3 Implement `GetExerciseHistoryTool @Component` — delegates to existing `ExerciseRepository`, validates `exerciseName` arg
- [ ] 7.4 Implement `GetPersonalRecordsTool @Component` — delegates to existing `ExerciseRepository`
- [ ] 7.5 Implement `GetVolumeStatsTool @Component` — delegates to existing `ExerciseRepository.findWeeklyVolumeByMuscle`
- [ ] 7.6 Implement `GetMonthlyAggregatesTool @Component` — queries `MonthlyAggregateRepository` only (no raw exercise table)
- [ ] 7.7 Implement `GetRecentWorkoutsTool @Component` — delegates to existing `WorkoutSessionRepository`
- [ ] 7.8 Implement `GetBloodResultsTool @Component` — delegates to existing `BloodTestResultRepository`
- [ ] 7.9 Implement `GetMeasurementsTool @Component` — delegates to existing `MeasurementRepository`
- [ ] 7.10 Write unit tests for each tool verifying correct repository delegation and user-scoped queries

## 8. AI Settings Service and Controller

- [ ] 8.1 Implement `AiSettingsService` with `getSettings(UUID userId)`, `saveSettings(UUID userId, String provider, String apiKey)`, and `getDecryptedApiKey(UUID userId)` methods
- [ ] 8.2 Implement `AiSettingsController` with `GET /api/ai/settings` (returns masked view) and `PUT /api/ai/settings` (validates non-blank key)
- [ ] 8.3 Write integration tests for settings endpoints: GET with no key, GET with key, PUT valid, PUT blank key

## 9. Chat Service and Controller

- [ ] 9.1 Implement `AiChatService` orchestrating: fetch last 10 history messages → build context → call `AiProvider.stream()` → persist user + assistant messages
- [ ] 9.2 Implement tool call loop in `AiChatService`: after `onToolUse()`, execute tool via `AiToolRegistry`, inject result, continue streaming; enforce max 5 tool calls per request
- [ ] 9.3 Implement `SseEmitterAiStreamSink` adapter that translates `AiStreamSink` callbacks into typed SSE events (`event: token`, `event: tool_use`, etc.) using `SseEmitter`
- [ ] 9.4 Implement `AiChatController` `POST /api/ai/chat`: authenticate user, validate request, create `SseEmitter(60_000L)`, submit `AiChatService.chat()` to `ExecutorService`, return emitter
- [ ] 9.5 Write integration tests for `/api/ai/chat`: valid message streams tokens, missing API key returns error event, tool use emits tool_use + tool_result events

## 10. Insights Service

- [ ] 10.1 Implement `AiInsightService.generateInsight(UUID userId, AiTaskType type)`: fetch data via tools → select model via `ModelRouter` → call `AiProvider.complete()` → persist to `ai_insights` (UPSERT by unique constraint)
- [ ] 10.2 Implement `AiInsightController` with `GET /api/ai/insights` (paginated, type filter) and `POST /api/ai/insights/generate` (manual trigger)
- [ ] 10.3 Write integration tests for insights endpoints: list empty, list with data, manual trigger

## 11. Scheduled Jobs

- [ ] 11.1 Implement `InsightScheduler @Component` with `@Scheduled(cron = "0 0 2 * * *")` daily summary job: check API key presence, skip with INFO log if missing, call `AiInsightService` wrapped in try-catch with ERROR logging
- [ ] 11.2 Implement weekly report job in `InsightScheduler` at `cron = "0 0 3 * * MON"` using Sonnet model
- [ ] 11.3 Implement monthly aggregate job at `cron = "0 30 2 * * *"` using native SQL UPSERT via `MonthlyAggregateRepository`
- [ ] 11.4 Write unit tests for `InsightScheduler`: verify no AI call when API key absent, verify correct task type passed per schedule

## 12. Frontend — API Client

- [ ] 12.1 Add `react-markdown` dependency to `evolve-log-ui/package.json`
- [ ] 12.2 Implement `aiApi.ts` with `fetchInsights(type, limit)`, `generateInsight(type)`, `getSettings()`, `saveSettings(provider, apiKey)` functions using the existing Axios/fetch client pattern
- [ ] 12.3 Implement `useChatStream(conversationId)` hook that manages `EventSource` lifecycle, parses typed SSE events, and returns `messages`, `isStreaming`, `toolStatus`, and `sendMessage(text, pageContext)` — stores `conversationId` in `localStorage`

## 13. Frontend — Insights Page

- [ ] 13.1 Implement `InsightCard.tsx` component rendering Markdown via `react-markdown`, card header with type/period/date, and "Ask about this" button
- [ ] 13.2 Implement `InsightsPage.tsx` fetching weekly reports and latest daily summary, rendering `InsightCard` list, empty state with "Generate Now" button, and a loading spinner
- [ ] 13.3 Add `/insights` route to the React Router configuration
- [ ] 13.4 Add "Insights" link to the main navigation component

## 14. Frontend — Chat Overlay

- [ ] 14.1 Implement `ToolStatusBanner.tsx` component that appears during `tool_use` events and dismisses on `tool_result` or `done`
- [ ] 14.2 Implement `QuickActions.tsx` component with page-context-aware prompt buttons (different prompts for `progress`, `workouts`, `blood`, `dashboard` pages)
- [ ] 14.3 Implement `ChatOverlay.tsx` with message list (role-based styling), thinking indicator (shown from send until first token), `ToolStatusBanner`, `QuickActions`, text input, and send button — uses `useChatStream` hook
- [ ] 14.4 Implement floating chat FAB button in the root layout component that toggles `ChatOverlay` open/closed
- [ ] 14.5 Add "New chat" action to `ChatOverlay` that clears `localStorage` conversation ID and resets message list

## 15. Frontend — Settings Extension

- [ ] 15.1 Extend the existing Settings page with an "AI Configuration" section: API key input (masked), save button, calls `aiApi.saveSettings()`, shows masked hint after save
- [ ] 15.2 On Settings page load, call `aiApi.getSettings()` and display `apiKeyHint` if `hasApiKey` is true

## 16. Security and Environment

- [ ] 16.1 Add `AI_ENCRYPTION_KEY` to local `.env` / development environment config (document required format: base64, 32 bytes)
- [ ] 16.2 Verify no API key, model name, or system prompt is returned by any API endpoint (manual inspection of all new controller response bodies)
- [ ] 16.3 Verify all new endpoints require authentication (confirm Spring Security config covers `/api/ai/**`)

## 17. End-to-End Verification

- [ ] 17.1 Run the application locally, navigate to Settings, save a Claude API key, verify masked hint appears
- [ ] 17.2 Open the chat overlay, send "What are my recent workouts?" and verify token streaming, tool status banner, and complete response
- [ ] 17.3 Trigger manual insight generation from the Insights page and verify a report card appears with rendered Markdown
- [ ] 17.4 Verify "Ask about this" on an InsightCard opens the chat overlay with context pre-filled
- [ ] 17.5 Verify the monthly aggregate job can be triggered manually (temporary `@PostConstruct` or test endpoint) and rows appear in `monthly_exercise_aggregates`
