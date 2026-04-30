# Supplement UX Redesign — Tasks

## Phase 1: Backend

- [x] **T1** Add `SupplementTodayDTO` with `planId`, `planName`, `entries[]` (each entry has `entryId`, `supplementId`, `supplementName`, `timeSlot`, `doseAmount`, `doseUnit`, `takenToday`, `loggedAt`, `logId`)
- [x] **T2** Add `SupplementLogService.getTodayStatus(userId)` — executes the LEFT JOIN query against `CURRENT_DATE`
- [x] **T3** Add `GET /api/supplements/plans/today` endpoint in supplements controller
- [x] **T4** Ensure `DELETE /api/supplements/logs/{logId}` exists (add if missing)

## Phase 2: Backend Tests

- [x] **T5** Integration test: `GET /api/supplements/plans/today` returns correct `takenToday` flags
- [x] **T6** Integration test: `takenToday` is false for inactive plans
- [x] **T7** Integration test: `takenToday` resets correctly across UTC midnight boundary

## Phase 3: Frontend

- [x] **T8** Add `supplementsApi.getToday()` and `supplementsApi.deleteLog(logId)` to `src/api/supplements.ts`
- [x] **T9** Update `SupplementTodayDTO` types in `src/types/api.ts`
- [x] **T10** Redesign `SupplementsPage` — replace tab layout with `PlanCard` list
- [x] **T11** Create `src/components/supplements/PlanCard.tsx` — plan name header + entry rows
- [x] **T12** Create `src/components/supplements/PlanEntryRow.tsx` — supplement info + optimistic checkbox
- [x] **T13** Remove Logs tab/route from Sidebar, MobileNav, and App.tsx routing
- [x] **T14** Add redirect from old logs URL (if it existed) to `/supplements`
