# Design: AI Tools Quality Fixes

---

## Fix 1: get_recent_workouts — Per-Set Detail + RPE

### Rendering Logic

```
for each Exercise e in session:
  if e.getReps() != null OR e.getWeightKg() != null:
    // Legacy aggregate path (pre-V17 or manually set)
    render: "- {name}: {sets}× {reps} @ {weight} kg"
  else if e.getWorkoutSets() is non-empty:
    // Modern per-set path (V17+)
    render header: "- {name} ({sets} sets):"
    for each WorkoutSet ws ordered by setNumber:
      if ws.isCompleted():
        render: "  set {n}: {reps} reps @ {weight} kg"
      else:
        render: "  set {n}: planned (not completed)"
  else:
    // No data at all
    render: "- {name}: {sets} sets (no rep/weight data)"

  if e.getRpe() != null:
    append " [RPE {rpe}]" to last line of exercise block
```

### Example Output (modern workout)

```
## 2026-06-01 (62 min)
- Bench Press (5 sets):
  set 1: 5 reps @ 100.0 kg
  set 2: 5 reps @ 100.0 kg
  set 3: 4 reps @ 100.0 kg
  set 4: 4 reps @ 97.5 kg
  set 5: 3 reps @ 95.0 kg [RPE 9.0]
- Overhead Press (4 sets):
  set 1: 8 reps @ 60.0 kg
  ...
```

### Why No Extra Queries

`Exercise.workoutSets` is mapped as:
```java
@OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL,
           fetch = FetchType.EAGER)
@Fetch(FetchMode.SUBSELECT)
private List<WorkoutSet> workoutSets = new ArrayList<>();
```
The `findRecentByUserIdWithExercises` repository query already joins exercises,
and `SUBSELECT` fetch mode loads all sets for the returned exercises in one
additional query — not N+1.

---

## Fix 2: get_measurements — Date Range

### Parameter Decision Logic

```
if from_date OR to_date is provided:
  use findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to)
  (defaults: from = today-90days, to = today if only one end specified)
else:
  use findByUserIdOrderByDateDesc(userId, PageRequest.of(0, limit))
```

### New Input Schema

```json
{
  "type": "object",
  "properties": {
    "limit": {
      "type": "integer",
      "description": "Number of most-recent entries (default 10, max 50). Used when no date range is given."
    },
    "from_date": {
      "type": "string",
      "description": "Start date YYYY-MM-DD. When provided, returns all entries in range (ignores limit)."
    },
    "to_date": {
      "type": "string",
      "description": "End date YYYY-MM-DD (default: today). Used together with from_date."
    }
  },
  "required": []
}
```

### Date-range output is ordered ASC (oldest first) for trend readability.
### Limit-based output remains DESC (most recent first).
