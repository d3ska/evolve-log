## Why

The backend `training-block` change introduced a real `training_blocks` table and `block_id` FK on `training_plans`, replacing the old name-parsing hack. The frontend still uses the old approach — grouping plans by a regex on plan names and persisting custom block labels in localStorage — and has no API integration for blocks at all. This change aligns the UI with the new backend model.

## What Changes

- Create `src/api/blocks.ts` with a `blocksApi` object (`list`, `create`, `update`, `delete`)
- Add `blockId: string | null` to the `TrainingPlan` type and `blockId` fields to plan create/update payloads
- Replace the `groupPlans()` name-regex logic with block-FK-based grouping (plans with `blockId = null` → "Unassigned" group at the bottom)
- Remove all `localStorage` block-name logic (`evolvelog_block_names`)
- Load blocks and plans in parallel on mount
- Block header: inline editable name and description (PATCH on blur/Enter)
- Block header: archive/activate button (archive shows confirmation modal; activate is immediate)
- Block header: delete button with confirmation modal (shows plan count, "Also delete all N plans" checkbox)
- Add "New Block" button to page header (modal with name + description)
- Plan create/edit modal: add optional "Block" dropdown populated from loaded blocks
- Plan delete: replace `confirm()` with a proper confirmation modal
- Plan archive/activate: remain immediate (no modal — reversible action)
- Hidden archived blocks by default; "Show archived" toggle appears when any block has `isActive = false`

## Capabilities

### New Capabilities

- `training-block-ui`: Full frontend implementation of training block management — API layer, block-based plan grouping, block CRUD modals, inline editing, plan–block assignment in plan form.

### Modified Capabilities

<!-- No existing promoted spec is changing at the requirement level -->

## Impact

- `evolve-log-ui/src/api/blocks.ts` — new file
- `evolve-log-ui/src/types/api.ts` — `TrainingPlan` gains `blockId`; new `TrainingBlock` interface
- `evolve-log-ui/src/api/plans.ts` — `CreatePlanPayload` and update payload gain `blockId`
- `evolve-log-ui/src/pages/TrainingPlansPage.tsx` — significant refactor (~500 → ~700 lines; page split into sub-components if needed)
- No routing, store, or backend changes required
