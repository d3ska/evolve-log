## 1. Database — Edit V2__training_plans.sql

- [ ] 1.1 Add `CREATE TABLE training_blocks` before `CREATE TABLE training_plans`:
  ```sql
  CREATE TABLE training_blocks (
      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      name        VARCHAR(100) NOT NULL,
      description TEXT,
      is_active   BOOLEAN NOT NULL DEFAULT TRUE,
      created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
  );
  CREATE INDEX idx_training_blocks_user_id ON training_blocks(user_id);
  ```
- [ ] 1.2 Add `block_id UUID REFERENCES training_blocks(id) ON DELETE SET NULL` column to the `training_plans` table definition
- [ ] 1.3 Add `CREATE INDEX idx_training_plans_block_id ON training_plans(block_id)` after the `training_plans` table

## 2. Backend — TrainingBlock Entity & Repository

- [ ] 2.1 Create `TrainingBlock` entity in `domain/`:
  - Fields: `id UUID`, `userId UUID`, `name String`, `description String`, `isActive boolean`, `createdAt OffsetDateTime`
  - No public setters; use constructor + `applyPatch(name, description, isActive)` method
  - `@Table(name = "training_blocks")`
- [ ] 2.2 Create `TrainingBlockRepository extends JpaRepository<TrainingBlock, UUID>`:
  - Add `List<TrainingBlock> findByUserIdOrderByCreatedAtDesc(UUID userId)`
  - Add `Optional<TrainingBlock> findByIdAndUserId(UUID id, UUID userId)`

## 3. Backend — TrainingBlockService

- [ ] 3.1 Create `TrainingBlockService` with constructor injection of `TrainingBlockRepository`
- [ ] 3.2 Implement `list(User user): List<TrainingBlockDto>` — calls `findByUserIdOrderByCreatedAtDesc`
- [ ] 3.3 Implement `create(User user, CreateTrainingBlockRequest req): TrainingBlockDto` — builds entity, saves, returns DTO
- [ ] 3.4 Implement `update(User user, UUID id, UpdateTrainingBlockRequest req): TrainingBlockDto`:
  - Load by `(id, userId)` — throw `ResourceNotFoundException` if absent
  - Call `applyPatch` only for non-null fields from request
  - Save and return DTO
- [ ] 3.5 Implement `delete(User user, UUID id, boolean deletePlans)`:
  - Load by `(id, userId)` — throw `ResourceNotFoundException` if absent
  - If `deletePlans = true`: call `trainingPlanRepository.deleteByBlockIdAndUserId(id, userId)` first (cascade on `planned_exercises` handles child rows)
  - Delete the block entity (DB FK `ON DELETE SET NULL` handles remaining plan nullification when `deletePlans = false`)

## 4. Backend — DTOs

- [ ] 4.1 Create `TrainingBlockDto` record: `id`, `name`, `description`, `isActive`, `createdAt`; add static `from(TrainingBlock)` factory
- [ ] 4.2 Create `CreateTrainingBlockRequest` record: `@NotBlank @Size(max=100) String name`, `String description`
- [ ] 4.3 Create `UpdateTrainingBlockRequest` record: `@Size(max=100) String name`, `String description`, `Boolean isActive`
- [ ] 4.4 Add `UUID blockId` (nullable) to `TrainingPlanDto` and update `TrainingPlanDto.from(TrainingPlan)`
- [ ] 4.5 Add `UUID blockId` (nullable, no validation) to `CreateTrainingPlanRequest`
- [ ] 4.6 Add `UUID blockId` (nullable, no validation) to `UpdateTrainingPlanRequest`

## 5. Backend — TrainingBlockController

- [ ] 5.1 Create `TrainingBlockController` mapped to `/api/training-blocks`
- [ ] 5.2 `GET /` → `list(...)` → 200 with `ApiResponse<List<TrainingBlockDto>>`
- [ ] 5.3 `POST /` with `@Valid @RequestBody CreateTrainingBlockRequest` → `create(...)` → 201 with `ApiResponse<TrainingBlockDto>`
- [ ] 5.4 `PATCH /{id}` with `@RequestBody UpdateTrainingBlockRequest` → `update(...)` → 200 with `ApiResponse<TrainingBlockDto>`
- [ ] 5.5 `DELETE /{id}` with `@RequestParam(defaultValue = "false") boolean deletePlans` → `delete(..., deletePlans)` → 204 empty body

## 6. Backend — TrainingPlan Changes

- [ ] 6.1 Add `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "block_id") private TrainingBlock block` to `TrainingPlan` entity
- [ ] 6.2 Add `blockId` getter: `getBlockId()` returning `block != null ? block.getId() : null`
- [ ] 6.3 Update `TrainingPlan` constructor/builder to accept nullable `TrainingBlock block`
- [ ] 6.4 Update `TrainingPlan.applyPatch` to accept a second-phase block reference or add `setBlock(TrainingBlock)` — resolving done in service before calling patch
- [ ] 6.5 Update `TrainingPlanService.createPlan`:
  - If `blockId` present in request: load block via `TrainingBlockRepository.findByIdAndUserId` — throw 404 if absent
  - Pass resolved `TrainingBlock` (or null) to entity constructor
- [ ] 6.6 Update `TrainingPlanService.updatePlan`:
  - If `blockId` field present in request payload (including explicit null): resolve and apply block assignment
  - Throw 404 if provided `blockId` resolves to no block for this user

## 7. Backend — Tests

- [ ] 7.1 `TrainingBlockServiceTest`: create stores block with correct fields; list returns only caller's blocks; update renames and toggles isActive; update unknown id → 404; update other user's block → 404; delete with `deletePlans=false` nullifies plans (verify via plan query); delete with `deletePlans=true` removes plans and block; delete other user's block → 404
- [ ] 7.2 `TrainingBlockControllerTest` (MockMvc): `POST /` → 201; `POST /` blank name → 400; `GET /` → 200 list; `PATCH /{id}` → 200; `PATCH /unknown` → 404; `DELETE /{id}` → 204; `DELETE /{id}?deletePlans=true` → 204 (plans gone); `DELETE /unknown` → 404
- [ ] 7.3 `TrainingPlanServiceTest`: create plan with valid blockId sets block FK; create plan with blockId of another user → 404; patch plan with `blockId = null` clears FK; patch plan with blockId of another user → 404

## 8. Frontend — `src/api/blocks.ts`

- [ ] 8.1 Create `src/api/blocks.ts` with `TrainingBlock` interface: `id`, `name`, `description`, `isActive`, `createdAt`
- [ ] 8.2 Implement `blocksApi.list()`, `blocksApi.create(payload)`, `blocksApi.update(id, payload)`, `blocksApi.delete(id, deletePlans?: boolean)` — passes `?deletePlans=true` when flag is set
- [ ] 8.3 Add `blockId: string | null` to `TrainingPlan` type in `src/types/api.ts`
- [ ] 8.4 Add `blockId?: string` to `CreatePlanPayload` in `src/api/plans.ts`
- [ ] 8.5 Add `blockId?: string | null` to the `update` payload type in `src/api/plans.ts`

## 9. Frontend — TrainingPlansPage Refactor

- [ ] 9.1 Load blocks (`blocksApi.list()`) and plans (`plansApi.list()`) in parallel on mount via `Promise.all`
- [ ] 9.2 Replace `groupPlans()` logic: group plans by `plan.blockId`; plans with `blockId = null` → "Unassigned" group sorted to the bottom
- [ ] 9.3 Map block groups using the `TrainingBlock` data (name, description, isActive) from the API instead of deriving from plan name/localStorage
- [ ] 9.4 Block header: show block name (inline editable — `PATCH /api/training-blocks/{id}` on blur/Enter)
- [ ] 9.5 Block header: show block description below name (inline editable — same PATCH call)
- [ ] 9.6 Block header: archive button opens confirmation modal ("Archive this block? Plans are not affected."); on confirm calls `blocksApi.update(id, { isActive: false })` and updates local block state; activate executes immediately (no modal — it's a safe action)
- [ ] 9.7 Block header: delete button opens confirmation modal showing block name, plan count, and "Also delete all N plans" checkbox (unchecked by default); on confirm calls `blocksApi.delete(id, deletePlans)`, then removes block from state and sets affected plans' `blockId` to null (or removes them if `deletePlans=true`)
- [ ] 9.8 "New Block" button in page header — opens modal with name + description, calls `blocksApi.create()`
- [ ] 9.9 Plan create/edit modal: add "Block" dropdown listing user's blocks (optional); sends `blockId` on create/update
- [ ] 9.13 Plan card: delete button opens confirmation modal ("Delete this plan? This cannot be undone."); on confirm calls `plansApi.delete(id)`
- [ ] 9.14 Plan card: archive toggle executes immediately without confirmation (reversible action); activate likewise
- [ ] 9.10 "Unassigned" group: render at bottom with a distinct header label; plans with `blockId = null` belong here
- [ ] 9.11 Remove all `localStorage` block name logic (`evolvelog_block_names` key)
- [ ] 9.12 Archived blocks hidden by default; show "Show archived" toggle if any block has `isActive = false`

## 10. Backend CLAUDE.md

- [ ] 10.1 Add `training-block` to the active changes list in `CLAUDE.md`
- [ ] 10.2 Add `TrainingBlock` to the Training Plans domain area row in the Domain Areas table
