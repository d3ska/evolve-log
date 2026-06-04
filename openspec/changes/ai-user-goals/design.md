# Design: Persistent User Goals

## Database

Migration `V23__ai_goals.sql`:
```sql
ALTER TABLE ai_settings ADD COLUMN goals TEXT;
```

Nullable — existing rows remain valid without goals set.

## Entity

`AiSettings.java` gains:
```java
@Column(columnDefinition = "TEXT")
private String goals;

public void updateGoals(String goals) {
    this.goals = goals;
}
```

## API

**Endpoint:** `PUT /api/ai/settings/goals`

**Request body:**
```json
{ "goals": "Currently bulking, target 90kg..." }
```

**Response:** `204 No Content`

**Validation:**
- `goals` may be null (clears the field)
- Max length enforced at service layer: 2000 characters

`GET /api/ai/settings` response includes `goals` field (null when not set).

## PromptContextBuilder Injection

`PromptContextBuilder.buildContext()` reads goals from `AiSettingsRepository.findById(userId)`:

```
## Your Goals
{goals text, verbatim}
```

Placed at the **top** of the context block (before `## Available Data`) so it
appears in the system prompt before any data availability hints. This anchors
the AI's recommendations to the user's stated objectives from the start.

When `goals == null`, the `## Your Goals` section is omitted entirely.

## Prompt Template Impact

The existing `CHAT.txt` system prompt uses `{{context}}` substitution.
When goals are set, the substituted block becomes:

```
## Your Goals
Currently bulking, target weight 90kg by end of year...

## Available Data
- Workouts: recorded, last session on 2026-06-01
- Body measurements: recorded, last entry on 2026-05-28
...
```

No changes to prompt template files are needed.
