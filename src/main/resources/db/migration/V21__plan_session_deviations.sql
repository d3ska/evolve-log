-- V21: Plan session deviations
-- Adds planned_exercise_id to exercises and plan_snapshot to workout_sessions

ALTER TABLE exercises
    ADD COLUMN planned_exercise_id UUID REFERENCES planned_exercises(id) ON DELETE SET NULL;

CREATE INDEX idx_exercises_planned_exercise_id ON exercises(planned_exercise_id);

ALTER TABLE workout_sessions
    ADD COLUMN plan_snapshot JSONB;
