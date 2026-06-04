-- Add goals field to ai_settings so users can persist their current fitness objectives.
-- Goals are injected into every AI system prompt automatically via PromptContextBuilder.
ALTER TABLE ai_settings ADD COLUMN goals TEXT;
