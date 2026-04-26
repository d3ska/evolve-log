ALTER TABLE planned_exercises
    ADD COLUMN exercise_definition_id UUID REFERENCES exercise_definitions(id) ON DELETE SET NULL;

ALTER TABLE workout_sessions
    ADD COLUMN started_at  TIMESTAMP,
    ADD COLUMN finished_at TIMESTAMP;

-- Allow null reps so exercises can be pre-populated from a plan without actuals
ALTER TABLE exercises
    ALTER COLUMN reps DROP NOT NULL;

CREATE INDEX idx_planned_exercises_exercise_definition_id
    ON planned_exercises (exercise_definition_id);
