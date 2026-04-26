CREATE TABLE workout_sets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exercise_id UUID    NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    set_number  INTEGER NOT NULL,
    reps        INTEGER,
    weight_kg   DECIMAL(6, 2),
    CONSTRAINT uq_exercise_set_number UNIQUE (exercise_id, set_number)
);

CREATE INDEX idx_workout_sets_exercise_id ON workout_sets (exercise_id);
