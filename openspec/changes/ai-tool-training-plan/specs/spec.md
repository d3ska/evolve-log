# Spec: get_training_plan AI Tool

## Tool Contract

**Name:** `get_training_plan`

**Description:**
Returns the athlete's training plans with all planned exercises, sets, rep ranges,
rest periods, and notes. Use this whenever the user asks about their training program,
wants to modify a plan, or when comparing planned vs actual performance.

**Input Schema:**
| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `active_only` | boolean | No | `true` | Filter to active plans only |

**Output:** Markdown-formatted list of training plans. Each plan includes:
- Plan name and assigned day of week (if set)
- Training block name (if the plan belongs to a block)
- Exercises in position order: `{pos}. {name} — {sets}×{repsMin}-{repsMax}[, rest {N}s][, notes: ...]`
- Empty-plan message if the plan has no exercises

**Error cases:**
- No plans found → returns `"No training plans found."`
- No active plans but inactive plans exist → returns `"No active training plans found. Use active_only: false to see all plans."`

## Acceptance Criteria

1. Tool is registered via `AiToolRegistry` (Spring `@Component` auto-detection)
2. Only queries the authenticated user's plans (`userId` scoped)
3. `active_only=true` filters plans where `isActive=false`
4. `active_only=false` returns all plans regardless of status, with `[inactive]` label
5. Exercises are rendered in `position ASC` order
6. `repsMin == repsMax` renders as `3×8` not `3×8-8`
7. `restSeconds == null` omits rest field entirely
8. `notes != null` appended as `, notes: {text}`
9. `dayOfWeek == null` omits day field
10. `block == null` omits block field
11. `PromptContextBuilder` includes training-plan count hint when plans exist
