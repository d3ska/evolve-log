# Supplement UX Redesign — Tasks

## Phase 1: Backend

- [ ] **T1** Add `SupplementTodayDTO` with `planId`, `planName`, `entries[]` (each entry has `entryId`, `supplementId`, `supplementName`, `timeSlot`, `doseAmount`, `doseUnit`, `takenToday`, `loggedAt`, `logId`)
- [ ] **T2** Add `SupplementLogService.getTodayStatus(userId)` — executes the LEFT JOIN query against `CURRENT_DATE`
- [ ] **T3** Add `GET /api/supplements/plans/today` endpoint in supplements controller
- [ ] **T4** Ensure `DELETE /api/supplements/logs/{logId}` exists (add if missing)

## Phase 2: Backend Tests

- [ ] **T5** Integration test: `GET /api/supplements/plans/today` returns correct `takenToday` flags
- [ ] **T6** Integration test: `takenToday` is false for inactive plans
- [ ] **T7** Integration test: `takenToday` resets correctly across UTC midnight boundary

## Phase 3: Frontend

- [ ] **T8** Add `supplementsApi.getToday()` and `supplementsApi.deleteLog(logId)` to `src/api/supplements.ts`
- [ ] **T9** Update `SupplementTodayDTO` types in `src/types/api.ts`
- [ ] **T10** Redesign `SupplementsPage` — replace tab layout with `PlanCard` list
- [ ] **T11** Create `src/components/supplements/PlanCard.tsx` — plan name header + entry rows
- [ ] **T12** Create `src/components/supplements/PlanEntryRow.tsx` — supplement info + optimistic checkbox
- [ ] **T13** Remove Logs tab/route from Sidebar, MobileNav, and App.tsx routing
- [ ] **T14** Add redirect from old logs URL (if it existed) to `/supplements`
