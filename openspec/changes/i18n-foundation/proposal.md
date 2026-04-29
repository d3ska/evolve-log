# i18n Foundation — Proposal

## Why

EvolveLog targets Polish-speaking users but the entire exercise catalog (~80 system exercises),
muscle group names, and UI strings are English-only. This change lays the infrastructure for
PL/EN support without touching the product UI yet — it makes data and APIs locale-aware so
that future UI work can consume translations directly.

## Scope of This Change

**In scope:**
- Translation table for exercise definitions (name + description in PL/EN)
- User locale preference stored in the `users` table (default `'en'`)
- Backend resolves locale from user preference; API returns translated fields
- Seed data: Polish translations for all ~80 system exercises and muscle group display names
- Frontend: locale passed as user profile setting (no UI language switching yet — strings stay EN)

**Out of scope (separate changes):**
- UI string translations (`react-i18next` setup, YAML/JSON translation files)
- Translating blood test parameter labels, supplement names, or other user-created content
- Language switcher in the UI

## Approach: Translation Tables (Option A)

A dedicated `exercise_definition_translations` table keyed by `(exercise_definition_id, locale)`.

Rationale over alternatives:
- Queryable with a single JOIN — no JSONB extraction operators
- Enforces completeness per locale via application logic
- Easy for the AI agent: `WHERE t.locale = :locale` in any tool query
- Extensible: adding `'de'` or `'es'` requires only new rows, no schema change
- The base `exercise_definitions.name` column stays as the canonical EN fallback

## Impact

- 1 migration (V22)
- `users` table: add `locale VARCHAR(10) NOT NULL DEFAULT 'en'`
- New entity `ExerciseDefinitionTranslation`
- `ExerciseDefinitionService`: resolve name/description from translation table for the current user's locale
- Seed data: ~80 PL translations shipped in the migration
- No breaking API changes — name/description fields remain the same shape in the DTO
