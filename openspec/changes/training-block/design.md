## Context

`training_plans` already has `name`, `description`, `is_active`, and `created_at`. The frontend groups plans by extracting a "Raport NNN" prefix from plan names and labels them "Block N". Block names live in localStorage. Block descriptions are identical strings duplicated across every plan row belonging to the same block. There is no backend concept of a block.

## Goals / Non-Goals

**Goals:**
- Introduce `training_blocks` as a first-class DB entity owned by a user
- Plans gain a nullable `block_id` FK — unassigned plans remain valid
- Full CRUD for blocks: create, rename, update description, archive/activate, delete
- Deleting a block sets `block_id = null` on its plans (ON DELETE SET NULL) — plans are never lost
- Plan create and update accept optional `blockId` to assign a plan to a block
- Frontend groups plans by their actual `block_id`; falls back to `createdAt` month for unassigned plans

**Non-Goals:**
- Reordering blocks (sorted by `created_at` descending, same as today)
- Block-level volume analytics (future)
- Bulk move plans between blocks (future)
- Block templates or copying (future)

## Decisions

### TrainingBlock Is a Separate Aggregate

`TrainingBlock` has its own service and controller. `TrainingPlanService` holds a reference to `TrainingBlockRepository` only to resolve the FK on plan create/update — it does not call `TrainingBlockService`. This keeps aggregate boundaries clean per the DDD rule: one service per aggregate root.

### plan.block_id Is Nullable — No Mandatory Block Assignment

Existing plans keep working without a block. The import script assigns blocks. Manually created plans can be assigned to a block at creation time or via a later patch. Unassigned plans appear in a catch-all "Unassigned" group on the frontend.

### Block Delete: Optional Cascade to Plans

`DELETE /api/training-blocks/{id}` accepts an optional `?deletePlans=true` query parameter.

- `deletePlans=false` (default): the FK `ON DELETE SET NULL` handles plan nullification automatically — plans survive unassigned.
- `deletePlans=true`: the service explicitly deletes all plans (and their planned exercises) belonging to the block before deleting the block itself. The DB cascade on `planned_exercises` handles child rows.

The default is always safe — the user's workout history is never silently destroyed. The frontend must explicitly pass `deletePlans=true` only when the user has checked the opt-in checkbox in the confirmation modal.

### Block `is_active` Is Independent of Plan `is_active`

Archiving a block does not cascade to its plans. The block `is_active` flag is a display filter hint for the UI (hide archived blocks by default). Individual plan `is_active` flags remain independently controlled. The frontend may offer a "archive all plans in this block" convenience action but that is a UI-level bulk call, not a cascade in the DB.

### Description Moves Off Plans

`training_plans.description` remains in the schema (it exists and removing it is unnecessary churn). On the frontend the block description is shown at block level; the plan-level description field is kept for plan-specific notes but the import script no longer duplicates the block coaching summary into every plan — it stores a block-level description instead and leaves plan descriptions empty.


### Block Ownership Verified on Every Request

`TrainingBlockService` always loads the block by `(id, userId)` — never by `id` alone. A 404 is returned if not found or not owned. `TrainingPlanService` does the same when resolving `blockId` on a plan write: if the block does not belong to the requesting user, throw 404.

### No Dedicated "Assign Plan to Block" Endpoint

Plan-to-block assignment goes through the existing `PATCH /api/training-plans/{id}` with `{ "blockId": "..." }`. This keeps the API surface minimal. A plan's block can be cleared by passing `{ "blockId": null }`.

## Data Shape

### DB Schema Changes (edit V2__training_plans.sql)

```sql
-- Add before CREATE TABLE training_plans
CREATE TABLE training_blocks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_training_blocks_user_id ON training_blocks(user_id);

-- Add column to training_plans table definition
block_id UUID REFERENCES training_blocks(id) ON DELETE SET NULL

-- Add index after table creation
CREATE INDEX idx_training_plans_block_id ON training_plans(block_id);
```

### API Endpoints

```
GET    /api/training-blocks                          → List<TrainingBlockDto>
POST   /api/training-blocks                          → TrainingBlockDto (201)
PATCH  /api/training-blocks/{id}                     → TrainingBlockDto (200)
DELETE /api/training-blocks/{id}?deletePlans=false   → 204
```

`deletePlans` query param (boolean, default `false`):
- `false` — delete block only; plans become unassigned (`block_id = null`)
- `true` — delete block and all its training day plans

### DTOs

```java
// Response
record TrainingBlockDto(
    UUID id,
    String name,
    String description,
    boolean isActive,
    LocalDateTime createdAt
) { ... }

// Create request
record CreateTrainingBlockRequest(
    @NotBlank @Size(max = 100) String name,
    String description
) { ... }

// Update request (all fields optional)
record UpdateTrainingBlockRequest(
    @Size(max = 100) String name,
    String description,
    Boolean isActive
) { ... }
```

`TrainingPlanDto` gains:
```java
UUID blockId   // nullable
```

`CreateTrainingPlanRequest` / `UpdateTrainingPlanRequest` gain:
```java
UUID blockId   // nullable, not validated — 404 if not found/not owned
```

### Frontend API shape

```ts
// src/api/blocks.ts
export interface TrainingBlock {
  id: string
  name: string
  description: string | null
  isActive: boolean
  createdAt: string
}

export const blocksApi = {
  list: () => client.get<ApiResponse<TrainingBlock[]>>('/training-blocks').then(r => r.data),
  create: (payload: { name: string; description?: string }) => ...,
  update: (id: string, payload: { name?: string; description?: string; isActive?: boolean }) => ...,
  delete: (id: string) => ...,
}
```

`TrainingPlan` type gains `blockId: string | null`.

## Frontend UI Design

### TrainingPlansPage grouping

- Load blocks and plans in parallel on mount
- Group plans by `blockId`; plans with `blockId = null` → "Unassigned" group at the bottom
- Block header shows: block name (inline editable via PATCH), block description (inline editable), Active badge, plan count, archive/activate button, delete button
- Archived blocks hidden by default; "Show archived" toggle appears when at least one archived block exists
- Remove all localStorage block name logic

### Block creation

- "New Block" button in page header opens a small modal: name + description fields
- After creation, user can create plans inside it or drag existing plans to it (drag is future scope — for now, plan assignment is via the plan edit modal)

### Confirmation Modals

Destructive and irreversible actions require explicit user confirmation before the API call is made.

**Block delete confirmation:**
```
Delete "Block 55"?

☐ Also delete all N training day plans in this block

[Cancel]  [Delete]
```
- Plan count is shown so the user knows what will be affected.
- Checkbox unchecked by default (`deletePlans=false`).
- On confirm: call `DELETE /api/training-blocks/{id}?deletePlans=<checkbox>`.

**Block archive confirmation:**
```
Archive "Block 55"?
The block will be hidden by default. Its plans are not affected.

[Cancel]  [Archive]
```

**Training day plan delete confirmation:**
```
Delete "Monday Push A"?
This cannot be undone.

[Cancel]  [Delete]
```

**Training day plan archive confirmation:**
No separate modal — archive/activate is a low-stakes toggle (reversible), so it executes immediately on click without a confirmation step. This matches existing behaviour on the plans page.

### Plan assignment to block

- Plan create/edit modal gains a "Block" dropdown (optional) listing the user's blocks
- Unassigning sets `blockId = null`

## Risks / Trade-offs

- **Unassigned plans UX**: plans without a block land in an "Unassigned" group. Manually created plans without a block assignment will appear here.
- **description on TrainingPlan**: kept in schema but UI no longer surfaces it prominently. Could be repurposed for plan-specific notes in the future.
