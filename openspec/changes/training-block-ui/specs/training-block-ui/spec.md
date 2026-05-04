## ADDED Requirements

### Requirement: Block API module exists
The system SHALL provide a `blocksApi` object in `src/api/blocks.ts` with `list()`, `create(payload)`, `update(id, payload)`, and `delete(id, deletePlans?)` operations. A `TrainingBlock` interface (id, name, description, isActive, createdAt) SHALL be defined in `src/types/api.ts`. The `TrainingPlan` type SHALL include `blockId: string | null`.

#### Scenario: list returns blocks array
- **WHEN** `blocksApi.list()` is called
- **THEN** it sends `GET /api/training-blocks` and returns the data array

#### Scenario: create sends correct payload
- **WHEN** `blocksApi.create({ name, description })` is called
- **THEN** it sends `POST /api/training-blocks` with the payload and returns the created block DTO

#### Scenario: update sends PATCH
- **WHEN** `blocksApi.update(id, { name })` is called
- **THEN** it sends `PATCH /api/training-blocks/{id}` with the partial payload

#### Scenario: delete without deletePlans
- **WHEN** `blocksApi.delete(id)` is called with no second argument
- **THEN** it sends `DELETE /api/training-blocks/{id}` without a query parameter

#### Scenario: delete with deletePlans=true
- **WHEN** `blocksApi.delete(id, true)` is called
- **THEN** it sends `DELETE /api/training-blocks/{id}?deletePlans=true`

---

### Requirement: Plan create/update payloads include blockId
The `CreatePlanPayload` in `src/api/plans.ts` SHALL accept an optional `blockId?: string`. The update payload SHALL accept an optional `blockId?: string | null` (null clears the assignment).

#### Scenario: create plan with blockId
- **WHEN** a plan is created with a `blockId` value
- **THEN** the POST body includes `"blockId": "<uuid>"`

#### Scenario: update plan clearing block
- **WHEN** a plan is updated with `blockId: null`
- **THEN** the PATCH body includes `"blockId": null`

---

### Requirement: Page loads blocks and plans in parallel
On mount, `TrainingPlansPage` SHALL load blocks and plans concurrently via `Promise.all`. A single loading spinner SHALL be shown until both resolve. Both results SHALL be stored in component state.

#### Scenario: parallel load on mount
- **WHEN** the page mounts
- **THEN** `blocksApi.list()` and `plansApi.list()` are called simultaneously, not sequentially

#### Scenario: loading spinner shown during fetch
- **WHEN** the page is loading
- **THEN** a spinner is visible and no plan content is rendered

---

### Requirement: Plans grouped by blockId
Plans SHALL be grouped by their `blockId`. Each group header SHALL display the block name from the loaded blocks array. Plans with `blockId = null` SHALL be collected into an "Unassigned" group rendered after all named block groups, regardless of sort order. The name-regex grouping logic and `localStorage` block-name storage SHALL be removed entirely.

#### Scenario: plan with blockId appears under correct block header
- **WHEN** a plan has `blockId` matching a loaded block
- **THEN** the plan appears under that block's header section

#### Scenario: plan with null blockId appears in Unassigned group
- **WHEN** a plan has `blockId = null`
- **THEN** it appears under an "Unassigned" header at the bottom of the list

#### Scenario: Unassigned group absent when all plans have blocks
- **WHEN** all plans have a non-null `blockId`
- **THEN** no "Unassigned" group header is rendered

#### Scenario: localStorage key is not used
- **WHEN** the page loads or a block name is changed
- **THEN** `localStorage.getItem('evolvelog_block_names')` is never read or written

---

### Requirement: Archived blocks hidden by default
Blocks with `isActive = false` SHALL be hidden by default. A "Show archived" toggle SHALL appear in the page header if any block has `isActive = false`. When the toggle is active, archived blocks are shown with reduced opacity or visual distinction.

#### Scenario: archived block hidden by default
- **WHEN** a block has `isActive = false`
- **THEN** it is not rendered unless "Show archived" is toggled on

#### Scenario: Show archived toggle visible only when relevant
- **WHEN** no block has `isActive = false`
- **THEN** the "Show archived" toggle is not rendered

---

### Requirement: Block header inline editing
Each block header SHALL display the block name as an inline-editable field. Clicking the name SHALL activate an `<input>` pre-filled with the current name. On `blur` or `Enter`, a PATCH request SHALL be sent and local state updated. On `Escape`, editing is cancelled without saving. The description (if present) SHALL be similarly inline-editable below the name.

#### Scenario: click name to edit
- **WHEN** user clicks the block name
- **THEN** an input field appears pre-filled with the current name

#### Scenario: blur saves the name
- **WHEN** user edits the name and blurs the input
- **THEN** `blocksApi.update(id, { name })` is called and the header updates to show the new name

#### Scenario: Escape cancels editing
- **WHEN** user presses Escape while editing
- **THEN** the input closes and the original name is shown unchanged

#### Scenario: description inline editable
- **WHEN** user clicks the description area
- **THEN** a textarea appears pre-filled with the current description (or empty)

---

### Requirement: Block archive and activate
Each block header SHALL include an archive button. Clicking it SHALL open a confirmation modal ("Archive this block? Plans are not affected."). On confirm, `blocksApi.update(id, { isActive: false })` is called and local state updated. An activate button SHALL be shown for archived blocks and execute immediately without a modal.

#### Scenario: archive button opens confirmation modal
- **WHEN** user clicks the archive button on an active block
- **THEN** a confirmation modal appears with the message about plans not being affected

#### Scenario: confirm archive updates block state
- **WHEN** user confirms archiving
- **THEN** `blocksApi.update(id, { isActive: false })` is called and the block becomes archived

#### Scenario: activate executes immediately
- **WHEN** user clicks activate on an archived block
- **THEN** `blocksApi.update(id, { isActive: true })` is called immediately without a confirmation modal

---

### Requirement: Block delete with confirmation modal
Each block header SHALL include a delete button. Clicking it SHALL open a confirmation modal showing the block name, the number of plans in the block, and a checkbox "Also delete all N plans in this block" (unchecked by default). On confirm, `blocksApi.delete(id, deletePlans)` is called. If `deletePlans` is false, the plans remain and their `blockId` is set to null in local state. If `deletePlans` is true, the plans are removed from local state.

#### Scenario: delete modal shows plan count
- **WHEN** user clicks delete on a block with 3 plans
- **THEN** the modal shows "3 plans" and a checkbox to also delete them

#### Scenario: delete without deletePlans keeps plans in Unassigned
- **WHEN** user confirms delete with checkbox unchecked
- **THEN** `blocksApi.delete(id, false)` is called, the block is removed, and its plans appear in Unassigned

#### Scenario: delete with deletePlans removes plans
- **WHEN** user confirms delete with checkbox checked
- **THEN** `blocksApi.delete(id, true)` is called and the plans are removed from local state

#### Scenario: delete unknown block shows error
- **WHEN** the API returns 404 on delete
- **THEN** the modal closes and an error message is shown

---

### Requirement: New Block modal
The page header SHALL include a "New Block" button. Clicking it SHALL open a modal with a required name field and an optional description field. On submit, `blocksApi.create({ name, description })` is called and the new block is prepended to local block state. The new block starts with `isActive: true` and no plans.

#### Scenario: new block modal opens from header button
- **WHEN** user clicks "New Block" button
- **THEN** a modal with name and description fields appears

#### Scenario: create block with name only
- **WHEN** user fills in name and submits
- **THEN** `blocksApi.create({ name })` is called and the new block appears in the list

#### Scenario: empty name is invalid
- **WHEN** user submits the form with an empty name
- **THEN** form validation prevents submission and shows an error

---

### Requirement: Plan create/edit modal includes block assignment
The plan create/edit modal SHALL include an optional "Block" dropdown listing all loaded blocks by name. Selecting a block sets `blockId` on the payload. The dropdown SHALL have a "None" / no-block option. On edit, the current block SHALL be pre-selected.

#### Scenario: create plan with block selected
- **WHEN** user selects a block in the plan modal and saves
- **THEN** the create payload includes `blockId: <selected-id>`

#### Scenario: edit plan pre-selects current block
- **WHEN** user opens edit modal for a plan with a blockId
- **THEN** the block dropdown shows the current block pre-selected

#### Scenario: clear block assignment
- **WHEN** user selects "None" in the block dropdown and saves
- **THEN** the update payload includes `blockId: null`

---

### Requirement: Plan delete uses confirmation modal
The plan delete action SHALL open a confirmation modal ("Delete this plan? This cannot be undone.") instead of the browser `confirm()` dialog. On confirm, `plansApi.delete(id)` is called and the plan is removed from local state.

#### Scenario: delete plan opens modal
- **WHEN** user clicks the delete button on a plan card
- **THEN** a modal appears asking for confirmation

#### Scenario: confirm deletes the plan
- **WHEN** user confirms in the modal
- **THEN** `plansApi.delete(id)` is called and the plan card disappears

#### Scenario: cancel keeps the plan
- **WHEN** user cancels in the modal
- **THEN** the plan remains in the list

---

### Requirement: Plan archive/activate is immediate
Archiving or reactivating a plan SHALL execute immediately without a confirmation modal, as it is a reversible action.

#### Scenario: archive plan immediately
- **WHEN** user clicks archive on a plan
- **THEN** `plansApi.update(id, { isActive: false })` is called immediately and the plan card becomes dimmed

#### Scenario: activate plan immediately
- **WHEN** user clicks activate on an archived plan
- **THEN** `plansApi.update(id, { isActive: true })` is called immediately and the plan card returns to full opacity
