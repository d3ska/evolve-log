## Context

The `training-block` backend change shipped a real `training_blocks` table and a `block_id` FK on `training_plans`. The frontend (`TrainingPlansPage.tsx`, ~500 lines) still groups plans by a name regex (`/^Raport\s+(\d+)/i`) and persists custom labels in `localStorage` under `evolvelog_block_names`. There is no `blocks` API module, `TrainingPlan.blockId` does not exist in the type system, and the create/update plan payloads have no `blockId` field.

The UI codebase is React 19 + TypeScript 6 + Tailwind 4. API calls go through a shared Axios client in `src/api/client.ts`. All domain types live in `src/types/api.ts`. The page is self-contained — no Zustand store is involved.

## Goals / Non-Goals

**Goals:**
- Introduce `src/api/blocks.ts` with a full `blocksApi` (`list`, `create`, `update`, `delete`)
- Add `TrainingBlock` interface and `blockId: string | null` to `TrainingPlan` in `src/types/api.ts`
- Update plan API payloads with optional `blockId`
- Replace name-regex grouping with `blockId`-based grouping; "Unassigned" group for `blockId = null` plans, sorted to the bottom
- Remove all `localStorage` block-name logic
- Block header: inline editable name + description, archive/activate button, delete button
- "New Block" button and modal in the page header
- Plan create/edit modal: optional "Block" dropdown
- Plan delete: proper confirmation modal (replaces `confirm()`)
- Archived blocks hidden by default with "Show archived" toggle

**Non-Goals:**
- Drag-and-drop plan reordering across blocks
- Server-side pagination of plans or blocks
- i18n / translations
- Moving plans between blocks via drag-and-drop (block assignment is done through the plan edit modal only)
- Any backend changes

## Decisions

### 1. Parallel data load with Promise.all

Blocks and plans are independent resources. Loading them with `Promise.all([blocksApi.list(), plansApi.list()])` on mount eliminates waterfall latency. Both are stored in separate `useState` arrays. A single `loading` flag covers the combined load.

*Alternative considered*: load blocks lazily on first interaction — rejected because the grouping logic needs block data before rendering any plan.

### 2. Group plans by blockId FK, not by name regex

`groupPlans()` is replaced by a function that buckets plans by `plan.blockId`. Blocks are matched by id from the loaded `blocks[]` array. Plans with `blockId = null` form an "Unassigned" group rendered after all named blocks.

Sort order (asc/desc) applies to block `createdAt`; "Unassigned" always appears last regardless of sort.

*Alternative considered*: keep name-regex as fallback for legacy plans — rejected to eliminate dual-path complexity. All plans received from the new backend will have a proper `blockId` or null.

### 3. Inline editing saves on blur/Enter, no explicit save button

Block name and description fields become `<input>` / `<textarea>` on click. On `blur` or `Enter`, a PATCH is fired. On `Escape`, the edit is cancelled without saving. Optimistic local state update on success; revert on error.

*Alternative considered*: edit form inside a modal — rejected because the spec explicitly calls for inline editing.

### 4. Archive vs Delete — different confirmation UX

- **Archive** (`isActive: false`): shows a confirmation modal ("Archive this block? Plans are not affected."). Reversible, so the modal is informational.
- **Activate** (`isActive: true`): immediate, no modal (safe action).
- **Delete**: confirmation modal showing block name, plan count, and an "Also delete all N plans" checkbox (unchecked by default). Calls `DELETE /api/training-blocks/{id}?deletePlans=true/false`.

### 5. Plan delete modal replaces browser confirm()

`handleDelete` currently uses `confirm()`. It is replaced with a small confirmation modal ("Delete this plan? This cannot be undone."). This is consistent with the block delete UX and avoids browser dialog styling issues.

### 6. Page file size — extract sub-components if needed

The refactored page will grow beyond 500 lines. To stay within the 800-line file cap:
- `BlockHeader` inline-editing state and rendering → own component or hook
- `NewBlockModal` → own component
- `DeleteBlockModal` → own component
- `DeletePlanModal` → own component

Existing `PlanModal` stays in the same file (already co-located).

### 7. Block state is local — no Zustand store

Blocks are only consumed on `TrainingPlansPage`. A global store would be premature. Both `blocks` and `plans` live in component `useState`.

## Risks / Trade-offs

- **Stale local state after PATCH/DELETE**: after any mutation, the page updates local state directly (optimistic or re-fetch). If a network error occurs mid-update, a full re-fetch is the fallback. → Mitigation: wrap every mutation in try/catch; re-fetch on error.
- **Page grows large**: the refactor adds ~200 lines. → Mitigation: extract sub-components as described in Decision 6.
- **"Unassigned" group for legacy plans**: any plan that was created before `block_id` existed on the backend will have `blockId = null` and appear in the Unassigned group. → Acceptable — the user can assign them via the plan edit modal.
