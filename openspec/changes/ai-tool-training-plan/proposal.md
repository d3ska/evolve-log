# Proposal: get_training_plan AI Tool

## Problem

The AI trainer can see *what the user did* (workout history, exercise PRs, volume stats)
but is completely blind to *what the user is supposed to do* — their training plan.

When asked "should I change my chest day?" or "is my push/pull/legs split optimal?",
the AI has to guess the plan from workout history, which is:
- Inaccurate (history diverges from plan over time)
- Noisy (deloads, missed sessions, plan transitions mix in)
- Incomplete (the plan may be newer than existing history)

This means plan-adjustment recommendations are generic instead of targeted.
The AI cannot flag redundant exercises, mismatched volume, or under-recovered
muscle groups within the plan itself.

## Proposed Solution

Add a `get_training_plan` tool that exposes the user's `training_plans` with their
full `planned_exercises` (sets, rep ranges, rest, notes) and `training_block` context.

The tool is already 100% backed by existing infrastructure:
- `TrainingPlanRepository.findByUserIdOrderByCreatedAtAsc` already eager-loads
  `plannedExercises` and `block` via `@EntityGraph`.
- No new queries, no new migrations needed.

## Scope

- New `GetTrainingPlanTool.java` in `com.deska.evolvelog.ai.tools`
- Tool input: `active_only` boolean (default `true`) — filter to active plans only
- Auto-registered by `AiToolRegistry` (Spring list injection, no config change)
- Add availability hint to `PromptContextBuilder`
