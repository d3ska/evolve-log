-- Add exercise definition FK (nullable — existing rows remain valid)
ALTER TABLE exercises
    ADD COLUMN exercise_definition_id UUID REFERENCES exercise_definitions(id) ON DELETE SET NULL;

-- RPE per exercise (1.0–10.0 in 0.5 increments by convention, but DB only enforces range)
ALTER TABLE exercises
    ADD COLUMN rpe NUMERIC(3,1) CHECK (rpe BETWEEN 1.0 AND 10.0);

-- Denormalized primary muscle (copied from definition at insert/update time)
ALTER TABLE exercises
    ADD COLUMN primary_muscle VARCHAR(50);

-- Covering index for volume/overload queries by definition
CREATE INDEX idx_exercises_definition_id
    ON exercises (exercise_definition_id)
    INCLUDE (sets, reps, weight_kg, rpe)
    WHERE exercise_definition_id IS NOT NULL;

-- Index for muscle-group aggregation
CREATE INDEX idx_exercises_primary_muscle
    ON exercises (primary_muscle)
    WHERE primary_muscle IS NOT NULL;
