## ADDED Requirements

### Requirement: Insights page displaying report cards
The system SHALL include an `InsightsPage` React component accessible at the `/insights` route. On mount, it SHALL fetch `GET /api/ai/insights?type=WEEKLY_REPORT&limit=4` and render each result as an `InsightCard`. The page SHALL also display the most recent `DAILY_SUMMARY` insight.

#### Scenario: Insights loaded on page mount
- **WHEN** the user navigates to `/insights`
- **THEN** the page fetches insights from the API and renders one `InsightCard` per result

#### Scenario: Empty state shown when no insights exist
- **WHEN** the API returns an empty list
- **THEN** the page displays an empty state message explaining that insights will appear after the first scheduled generation, with a manual "Generate Now" button

### Requirement: InsightCard renders Markdown content
The system SHALL implement an `InsightCard` component that renders the insight's `content` field as formatted Markdown using `react-markdown`. The card SHALL display the insight type, period dates, and generated timestamp in the card header.

#### Scenario: Markdown content rendered in card
- **WHEN** an `InsightCard` receives an insight with Markdown content containing headings and bullet lists
- **THEN** the content is rendered as formatted HTML, not as raw Markdown text

#### Scenario: Card header shows period dates
- **WHEN** an `InsightCard` renders a weekly report
- **THEN** the header displays the `periodStart` to `periodEnd` date range in a human-readable format

### Requirement: Ask about this deeplink to chat
Each `InsightCard` SHALL include an "Ask about this" button. Clicking it SHALL open the floating `ChatOverlay` with the insight's content pre-loaded as context and a suggested prompt such as "Tell me more about this week's report".

#### Scenario: Ask about this opens chat with context
- **WHEN** the user clicks "Ask about this" on an InsightCard
- **THEN** the ChatOverlay opens and the first message is pre-populated with a reference to the insight period

### Requirement: Manual generation trigger
The `InsightsPage` SHALL include a "Generate Now" button that calls `POST /api/ai/insights/generate`. While generation is in progress, the button SHALL be disabled and display a loading indicator. On completion, the page SHALL refresh the insights list.

#### Scenario: Generate button triggers generation
- **WHEN** the user clicks "Generate Now"
- **THEN** the button becomes disabled, a loading indicator appears, and the API call is initiated

#### Scenario: Page refreshes after generation
- **WHEN** the `POST /api/ai/insights/generate` call returns successfully
- **THEN** the insights list is re-fetched and the new insight appears in the list

### Requirement: Insights page linked in navigation
The system SHALL add an "Insights" link to the main application navigation. The link SHALL be visible to authenticated users and SHALL navigate to the `/insights` route.

#### Scenario: Insights link appears in nav
- **WHEN** an authenticated user views any page with the main navigation
- **THEN** an "Insights" navigation item is visible and clicking it navigates to `/insights`
