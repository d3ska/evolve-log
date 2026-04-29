# Spec: today-state

### Requirement: Plans view shows whether each entry has been taken today

#### Scenario: Entry not yet taken
- **WHEN** `GET /api/supplements/plans/today` is called and no log row exists for an entry today
- **THEN** the entry has `takenToday: false` and `loggedAt: null`

#### Scenario: Entry taken today
- **WHEN** a `supplement_logs` row exists for that `plan_entry_id` with `DATE(taken_at) = CURRENT_DATE`
- **THEN** the entry has `takenToday: true` and `loggedAt` set to the timestamp

#### Scenario: Only active plans returned
- **WHEN** the user has an inactive plan (`active = false`)
- **THEN** that plan's entries are not included in the response

#### Scenario: Response at midnight UTC
- **WHEN** the request arrives after midnight UTC on a new day
- **THEN** all `takenToday` flags reset to `false` (prior day's logs are in history but not "today")

### Requirement: Tapping checkbox logs intake optimistically

#### Scenario: User taps unchecked entry
- **WHEN** the user taps the checkbox on an untaken entry
- **THEN** the UI immediately shows the entry as taken (green)
- **AND** `POST /api/supplements/logs` is called with `planEntryId`, `source = "PLANNED"`
- **AND** on success the `logId` is stored in local state

#### Scenario: Log POST fails
- **WHEN** `POST /api/supplements/logs` returns an error
- **THEN** the checkbox reverts to unchecked
- **AND** a toast is shown: "Failed to log supplement — try again"

### Requirement: Tapping checked entry removes today's log

#### Scenario: User untaps a taken entry
- **WHEN** the user taps the checkbox on an entry marked `takenToday: true`
- **THEN** the UI immediately shows it as untaken
- **AND** `DELETE /api/supplements/logs/{logId}` is called

#### Scenario: Delete fails
- **WHEN** the DELETE call fails
- **THEN** the checkbox reverts to checked
- **AND** a toast is shown

### Requirement: Logs tab is removed

#### Scenario: Nav items
- **WHEN** the app is rendered
- **THEN** there is no "Logs" nav item in the Sidebar or MobileNav for supplements
- **AND** navigating directly to any old Logs URL redirects to `/supplements`
