## Why

Training plans are currently grouped visually on the frontend by extracting a "Raport NNN" prefix from plan names — a fragile convention with no backend backing. Block names are stored in localStorage (lost on browser clear or device switch). Block descriptions are duplicated on every plan row in the DB. There is no way to archive or manage an entire training block as a unit.

Introducing a proper `TrainingBlock` entity gives blocks a first-class identity: own name, description, and active status, with plans referencing their block via a FK. This unlocks block-level operations (rename, archive, activate, delete) and removes the need for naming conventions and localStorage hacks.

## What Changes

- New `training_blocks` table with `id`, `user_id`, `name`, `description`, `is_active`, `created_at`
- Add nullable `block_id` FK to `training_plans` referencing `training_blocks`
- New REST resource: `GET/POST /api/training-blocks`, `PATCH/DELETE /api/training-blocks/{id}`
- `PATCH /api/training-plans/{id}` accepts optional `blockId` to assign/reassign a plan to a block
- `GET /api/training-plans` response includes `blockId` on each plan
- Frontend: block CRUD UI replaces the current name-convention grouping; block name/description move from localStorage/plan-row to the dedicated block entity

## Capabilities

### New Capabilities

- `training-block`: First-class training block entity with CRUD, archive/activate, and plan assignment

### Modified Capabilities

- `training-plans`: Plans gain a nullable `block_id` FK; create/update requests accept optional `blockId`

## Impact

- Edit `V2__training_plans.sql`: add `training_blocks` table before `training_plans`, add `block_id` FK column on `training_plans`
- New `TrainingBlock` entity + `TrainingBlockRepository`
- New `TrainingBlockService` (CRUD + archive toggle)
- New `TrainingBlockController` (`/api/training-blocks`)
- Update `TrainingPlan` entity: add `block` ManyToOne field
- Update `CreateTrainingPlanRequest` / `UpdateTrainingPlanRequest`: add optional `blockId UUID`
- Update `TrainingPlanDto`: add `blockId UUID`
- Update `TrainingPlanService`: resolve block on create/update
- Frontend `TrainingPlansPage`: replace localStorage grouping with backend block data; add block CRUD
- Frontend `plansApi` + new `blocksApi`: wire up new endpoints
- Update `import_to_evolvelog.py`: create block before plans, pass `blockId`
