-- V23: Plan versioning
-- Adds version tracking to training plans and stamps sessions with the plan version used

ALTER TABLE training_plans
    ADD COLUMN current_version INT NOT NULL DEFAULT 1;

CREATE TABLE training_plan_versions (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    training_plan_id UUID        NOT NULL REFERENCES training_plans (id) ON DELETE CASCADE,
    version          INT         NOT NULL,
    exercises        JSONB       NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_plan_version UNIQUE (training_plan_id, version)
);

CREATE INDEX idx_training_plan_versions_plan_id ON training_plan_versions (training_plan_id);

ALTER TABLE workout_sessions
    ADD COLUMN plan_version INT;
