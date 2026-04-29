# Supplement UX Redesign — Proposal

## Why

The supplement module currently has two separate tabs — Plans and Logs — creating unnecessary
cognitive overhead. In daily use, the user just wants to see their stack and check off what
they took. Switching between "what should I take" and "what did I take today" tabs is friction
that does not add value for a personal fitness app.

## What Changes

### Removed
- The "Logs" tab is removed from the UI. There is no separate page for viewing supplement logs.

### Replaced With
- Plans view gains inline **"taken today"** state per plan entry.
- Each entry shows a checkbox. Tapping it logs the intake and marks the entry green for the rest
  of the day.
- The historical log continues to be written to `supplement_logs` in the background — it just
  has no dedicated UI surface.

## What Does NOT Change

- `supplement_logs` table stays — it feeds AI analysis ("what supplements did you take last week?")
- `supplement_plan_entries` table stays unchanged
- Backend APIs are additive only — no existing endpoints are removed

## New Backend Capability

- `GET /api/supplements/plans/today` — returns plan entries enriched with `takenToday: boolean`
  based on whether a `supplement_logs` row exists for that entry today (UTC day boundary)

## Impact

- No DB migration needed
- New query in `SupplementLogService`
- New endpoint `GET /api/supplements/plans/today`
- Frontend: remove Logs tab from Sidebar/MobileNav, redesign Supplements page around Plans + today-state
