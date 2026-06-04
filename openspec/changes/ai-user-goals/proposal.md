# Proposal: Persistent User Goals

## Problem

The AI has no memory of the user's current fitness objectives. Every conversation
requires re-stating context: "I'm currently trying to bulk", "my goal is to hit a
140kg bench press", "I'm cutting for summer". The AI also cannot calibrate
recommendations without knowing the phase (bulk / cut / maintenance / strength /
hypertrophy / recomp).

Without explicit goals:
- Calorie and macro recommendations are generic
- Volume recommendations can't be phase-appropriate (strength = low rep / hypertrophy = high)
- Progressive overload suggestions ignore whether adding weight or recomp is the target
- The AI asks clarifying questions on every session instead of acting on known context

## Proposed Solution

Add a `goals TEXT` column to `ai_settings`. The user writes their goals in free text
(natural language, not a structured enum) to keep it flexible and expressive.

**Examples:**
- "Currently bulking, target weight 90kg by end of year. Prioritise strength on squat/bench/deadlift."
- "Cutting phase — maintaining muscle, deficit ~300kcal. Sleep is poor, keep volume moderate."
- "Recomp. Improve upper body symmetry. Weaker chest vs back."

Goals are injected into the AI system prompt by `PromptContextBuilder` as a
`## Your Goals` section, present in every chat and insight generation.

## Scope

- V23 migration: `ALTER TABLE ai_settings ADD COLUMN goals TEXT`
- `AiSettings.java`: add `goals` field + `updateGoals()` method
- `AiSettingsService`: add `updateGoals(userId, goals)` + include goals in settings response
- `AiSettingsController`: `PUT /api/ai/settings/goals` endpoint
- `PromptContextBuilder`: inject goals from `AiSettingsRepository` when present
