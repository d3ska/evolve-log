# Supplement UX Redesign — Design

## No Migration Required

The `supplement_logs` table already supports `plan_entry_id` FK (nullable). Logging a planned
intake just requires posting with `plan_entry_id` set. The "taken today" check is a query.

## New Endpoint: GET /api/supplements/plans/today

Returns all active plans for the user, with each entry carrying a `takenToday` flag.

```json
[
  {
    "planId": "uuid",
    "planName": "Morning Stack",
    "entries": [
      {
        "entryId": "uuid",
        "supplementId": "uuid",
        "supplementName": "Vitamin D",
        "timeSlot": "MORNING",
        "doseAmount": 2000,
        "doseUnit": "IU",
        "takenToday": true,
        "loggedAt": "2026-04-29T07:32:00Z"   // null if not taken
      }
    ]
  }
]
```

### Query Logic

```sql
SELECT
    spe.id          AS entry_id,
    spe.plan_id,
    spe.supplement_id,
    s.name          AS supplement_name,
    spe.time_slot,
    spe.dose_amount,
    spe.dose_unit,
    sl.id IS NOT NULL           AS taken_today,
    sl.taken_at                 AS logged_at
FROM supplement_plan_entries spe
JOIN supplement_plans sp     ON sp.id = spe.plan_id
JOIN supplements s           ON s.id  = spe.supplement_id
LEFT JOIN supplement_logs sl ON sl.plan_entry_id = spe.id
                             AND sl.user_id = :userId
                             AND DATE(sl.taken_at) = CURRENT_DATE
WHERE sp.user_id = :userId
  AND sp.active  = true
ORDER BY sp.name, spe.sort_order
```

## Log Intake (Existing + Extended)

Tapping the checkbox calls the existing `POST /api/supplements/logs` with:
```json
{
  "supplementId": "uuid",
  "planEntryId":  "uuid",
  "doseAmount":   2000,
  "doseUnit":     "IU",
  "source":       "PLANNED"
}
```

Un-tapping deletes the log row: `DELETE /api/supplements/logs/{logId}`.
The frontend stores `logId` from the POST response to enable deletion.

## Frontend Architecture

### Page Structure

```
/supplements
└── SupplementsPage
    └── PlanCard[]          (one card per active plan)
        └── PlanEntryRow[]  (supplement name, dose, time slot, checkbox)
```

The "Logs" route (`/supplements/logs` if it existed) and its nav items are removed.

### Optimistic UI

- Tapping checkbox immediately marks the row as taken (green) and fires `POST /api/supplements/logs`
- On error: revert to unchecked + show toast
- Un-tapping immediately reverts the row and fires `DELETE /api/supplements/logs/{logId}`
- The `logId` is stored in component state from the POST response

### Today Refresh

Entries reset at midnight UTC. `GET /api/supplements/plans/today` is called:
- On page mount
- After each successful log/unlog action (re-fetch or update local state from response)

## AI Agent Impact

The AI trainer can still query `supplement_logs` for historical data. No tools change.
