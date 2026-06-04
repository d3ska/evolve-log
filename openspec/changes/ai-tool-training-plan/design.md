# Design: get_training_plan AI Tool

## Data Flow

```
Claude → tool_use: get_training_plan { active_only: true }
             ↓
GetTrainingPlanTool.execute()
             ↓
TrainingPlanRepository.findByUserIdOrderByCreatedAtAsc(userId)
  [EntityGraph: plannedExercises, block — no extra queries]
             ↓
format as markdown text
             ↓
tool_result injected into conversation
```

## Entity Mapping

```
TrainingPlan
  ├── name          → section header
  ├── dayOfWeek     → "Day: MONDAY" (nullable — some plans are not day-specific)
  ├── isActive      → filtered by active_only param
  ├── block.name    → "Block: Strength Phase" (nullable)
  └── plannedExercises (ordered by position ASC)
        ├── name        → exercise name
        ├── sets        → "3 sets"
        ├── repsMin     → "8"
        ├── repsMax     → "12"  → "3×8-12"
        ├── restSeconds → "rest 90s" (nullable)
        └── notes       → appended if present
```

## Output Format (example)

```
Training plans (active):

### Push Day (MONDAY) [Block: Strength Phase]
1. Bench Press — 4×4-6, rest 180s
2. Overhead Press — 3×6-8, rest 120s
3. Incline Dumbbell Press — 3×10-12, rest 90s
4. Lateral Raise — 4×15-20, rest 60s
5. Tricep Pushdown — 3×12-15, rest 60s

### Pull Day (WEDNESDAY) [Block: Strength Phase]
1. Deadlift — 3×3-5, rest 240s
2. Barbell Row — 4×6-8, rest 120s
...
```

## Tool Schema

```json
{
  "type": "object",
  "properties": {
    "active_only": {
      "type": "boolean",
      "description": "If true (default), return only active plans. Set false to include inactive/archived plans."
    }
  },
  "required": []
}
```

## PromptContextBuilder Hint

Add a line in `buildContext()`:
```
- **Training plans**: N active plan(s) — use get_training_plan to read exercises and structure
```
This signals to Claude that plan data exists before it decides which tools to call.
