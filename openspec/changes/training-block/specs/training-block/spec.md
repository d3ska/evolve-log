## ADDED Requirements

### Requirement: Create training block
The system SHALL allow an authenticated user to create a named training block.

#### Scenario: Create block with name only
- **WHEN** user sends `POST /api/training-blocks` with `{ "name": "Raport 55" }`
- **THEN** system creates a `training_blocks` row with the given name, `description = null`, `is_active = true`, `user_id = current user`
- **AND** returns 201 with `TrainingBlockDto`

#### Scenario: Create block with name and description
- **WHEN** user sends `POST /api/training-blocks` with `{ "name": "Raport 55", "description": "Hypertrophy phase" }`
- **THEN** system creates the block with the provided description
- **AND** returns 201 with `TrainingBlockDto`

#### Scenario: Create block with blank name
- **WHEN** user sends `POST /api/training-blocks` with `{ "name": "" }`
- **THEN** system returns 400

#### Scenario: Create block with name longer than 100 characters
- **WHEN** user sends `POST /api/training-blocks` with a `name` of 101+ characters
- **THEN** system returns 400

---

### Requirement: List training blocks
The system SHALL return all training blocks owned by the authenticated user.

#### Scenario: List blocks for a user with blocks
- **WHEN** user sends `GET /api/training-blocks`
- **THEN** system returns 200 with an array of `TrainingBlockDto` ordered by `created_at` descending
- **AND** only blocks owned by the requesting user are included

#### Scenario: List blocks for a user with no blocks
- **WHEN** user has no blocks and sends `GET /api/training-blocks`
- **THEN** system returns 200 with an empty array

---

### Requirement: Update training block
The system SHALL allow partial updates of a training block's name, description, and active status.

#### Scenario: Rename a block
- **WHEN** user sends `PATCH /api/training-blocks/{id}` with `{ "name": "New Name" }`
- **THEN** system updates only the name and returns 200 with updated `TrainingBlockDto`

#### Scenario: Update description to a new value
- **WHEN** user sends `PATCH /api/training-blocks/{id}` with `{ "description": "Updated summary" }`
- **THEN** system updates only the description and returns 200

#### Scenario: Clear description by setting to null
- **WHEN** user sends `PATCH /api/training-blocks/{id}` with `{ "description": null }`
- **THEN** system sets description to null and returns 200

#### Scenario: Archive a block
- **WHEN** user sends `PATCH /api/training-blocks/{id}` with `{ "isActive": false }`
- **THEN** system sets `is_active = false` and returns 200 with `isActive: false`
- **AND** plans belonging to the block retain their own `is_active` values unchanged

#### Scenario: Activate a block
- **WHEN** user sends `PATCH /api/training-blocks/{id}` with `{ "isActive": true }`
- **THEN** system sets `is_active = true` and returns 200 with `isActive: true`

#### Scenario: Update block belonging to another user
- **WHEN** user sends `PATCH /api/training-blocks/{id}` for a block owned by a different user
- **THEN** system returns 404

#### Scenario: Update non-existent block
- **WHEN** user sends `PATCH /api/training-blocks/{id}` with an unknown id
- **THEN** system returns 404

---

### Requirement: Delete training block
The system SHALL allow a user to delete a training block, with an opt-in to also delete all its plans.

#### Scenario: Delete block — keep plans (default)
- **WHEN** user sends `DELETE /api/training-blocks/{id}` (no `deletePlans` param, or `deletePlans=false`)
- **THEN** system deletes the block and returns 204
- **AND** all `training_plans` that had `block_id = id` now have `block_id = null`

#### Scenario: Delete block — also delete plans
- **WHEN** user sends `DELETE /api/training-blocks/{id}?deletePlans=true`
- **THEN** system deletes all `training_plans` (and their `planned_exercises`) belonging to the block, then deletes the block, and returns 204

#### Scenario: Delete block belonging to another user
- **WHEN** user sends `DELETE /api/training-blocks/{id}` for a block owned by a different user
- **THEN** system returns 404

### Requirement: Confirmation before destructive actions
The frontend SHALL display a confirmation modal before executing any delete or archive action on a block or plan.

#### Scenario: Block delete confirmation shows plan count
- **WHEN** user clicks "Delete" on a block header
- **THEN** a modal appears showing the block name and the number of plans in the block
- **AND** a checkbox "Also delete all N plans" is shown, unchecked by default
- **AND** the delete API call is only made after the user clicks "Delete" in the modal

#### Scenario: Block archive confirmation
- **WHEN** user clicks the archive button on a block header
- **THEN** a modal appears asking for confirmation before the PATCH call is made

#### Scenario: Plan delete confirmation
- **WHEN** user clicks "Delete" on a training day plan card
- **THEN** a modal appears asking for confirmation before the DELETE call is made

#### Scenario: Plan archive — no confirmation needed
- **WHEN** user clicks the archive/activate toggle on a plan card
- **THEN** the PATCH call is made immediately (reversible action, no modal required)

---

## MODIFIED Requirements

### Requirement: Training plan exposes block assignment
The system SHALL include `blockId` in the plan response and accept `blockId` on create and update.

#### Scenario: Get plan assigned to a block
- **WHEN** user fetches `GET /api/training-plans` or reads a single plan
- **THEN** each plan DTO includes `blockId: UUID` (non-null if assigned) or `blockId: null`

#### Scenario: Create plan assigned to a block
- **WHEN** user sends `POST /api/training-plans` with `{ ..., "blockId": "<valid-block-id>" }`
- **AND** the block is owned by the same user
- **THEN** the created plan has `block_id` set and returns 201 with `blockId` populated

#### Scenario: Create plan without block
- **WHEN** user sends `POST /api/training-plans` without a `blockId` field
- **THEN** the created plan has `block_id = null`

#### Scenario: Assign plan to block via PATCH
- **WHEN** user sends `PATCH /api/training-plans/{id}` with `{ "blockId": "<valid-block-id>" }`
- **AND** the block is owned by the same user
- **THEN** the plan's `block_id` is updated and 200 is returned

#### Scenario: Remove plan from block via PATCH
- **WHEN** user sends `PATCH /api/training-plans/{id}` with `{ "blockId": null }`
- **THEN** the plan's `block_id` is set to null and 200 is returned

#### Scenario: Assign plan to block owned by another user
- **WHEN** user sends `POST` or `PATCH` for a plan with a `blockId` belonging to a different user
- **THEN** system returns 404
