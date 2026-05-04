## 1. Types & API Layer

- [x] 1.1 Add `TrainingBlock` interface to `src/types/api.ts`: `id`, `name`, `description: string | null`, `isActive`, `createdAt`
- [x] 1.2 Add `blockId: string | null` to the `TrainingPlan` interface in `src/types/api.ts`
- [x] 1.3 Create `src/api/blocks.ts` with `blocksApi.list()` → `GET /api/training-blocks`
- [x] 1.4 Add `blocksApi.create(payload: { name: string; description?: string })` → `POST /api/training-blocks`
- [x] 1.5 Add `blocksApi.update(id, payload: { name?: string; description?: string; isActive?: boolean })` → `PATCH /api/training-blocks/{id}`
- [x] 1.6 Add `blocksApi.delete(id, deletePlans?: boolean)` → `DELETE /api/training-blocks/{id}?deletePlans=true` when flag is set
- [x] 1.7 Add `blockId?: string` to `CreatePlanPayload` in `src/api/plans.ts`
- [x] 1.8 Add `blockId?: string | null` to the update payload type in `src/api/plans.ts`

## 2. TrainingPlansPage — Data Loading & Grouping

- [x] 2.1 Add `blocks` state (`TrainingBlock[]`) alongside existing `plans` state
- [x] 2.2 Replace the sequential `plansApi.list()` call with `Promise.all([blocksApi.list(), plansApi.list()])` on mount
- [x] 2.3 Replace `groupPlans()` with a new function that groups plans by `plan.blockId`, matching against the loaded `blocks` array
- [x] 2.4 Plans with `blockId = null` form an "Unassigned" group; always render it after all named block groups regardless of sort order
- [x] 2.5 Remove all `localStorage` block-name logic (`evolvelog_block_names` key, `blockNames` state, `editingBlockKey` state, `commitBlockName` function)

## 3. Block Header — Inline Editing

- [x] 3.1 Replace the static block name label with an inline-editable `<input>` that activates on click, pre-filled with the block name
- [x] 3.2 On `blur` or `Enter`, call `blocksApi.update(id, { name })` and update local `blocks` state; on `Escape` cancel without saving
- [x] 3.3 Add inline-editable description below the block name (click → textarea; blur/Enter saves; Escape cancels)

## 4. Block Header — Archive / Activate

- [x] 4.1 Add archive button to block header; clicking it opens a confirmation modal ("Archive this block? Plans are not affected.")
- [x] 4.2 On confirm, call `blocksApi.update(id, { isActive: false })` and update local block state
- [x] 4.3 Show activate button for archived blocks; clicking it calls `blocksApi.update(id, { isActive: true })` immediately without a modal

## 5. Block Header — Delete

- [x] 5.1 Add delete button to block header; clicking it opens a confirmation modal showing the block name and plan count
- [x] 5.2 Modal includes "Also delete all N plans in this block" checkbox (unchecked by default)
- [x] 5.3 On confirm with checkbox unchecked: call `blocksApi.delete(id, false)`, remove block from state, set `blockId = null` on affected plans in local state
- [x] 5.4 On confirm with checkbox checked: call `blocksApi.delete(id, true)`, remove block and its plans from local state

## 6. New Block Modal

- [x] 6.1 Add "New Block" button to the page header (alongside "New Plan")
- [x] 6.2 Implement `NewBlockModal` with required name field and optional description field
- [x] 6.3 On submit, call `blocksApi.create({ name, description })` and prepend the returned block to local `blocks` state

## 7. Plan Modal — Block Assignment

- [x] 7.1 Add a "Block" `SelectMenu` dropdown to `PlanModal`, listing loaded blocks by name with a "None" option
- [x] 7.2 On create, include `blockId` in the payload when a block is selected (omit when "None")
- [x] 7.3 On edit, pre-select the plan's current block in the dropdown; selecting "None" sends `blockId: null` in the update payload

## 8. Plan Delete Confirmation Modal

- [x] 8.1 Add `DeletePlanModal` component (or reuse a generic confirm modal) with message "Delete this plan? This cannot be undone."
- [x] 8.2 Replace the `confirm()` call in `handleDelete` with this modal; wire confirm → `plansApi.delete(id)` and remove plan from state

## 9. Archived Blocks Visibility

- [x] 9.1 Hidden archived blocks by default; show "Show archived" toggle in the page header only when at least one block has `isActive = false`
- [x] 9.2 When toggle is on, show archived block groups with visual distinction (reduced opacity or "Archived" badge on the header)
