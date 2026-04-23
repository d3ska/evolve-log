CREATE TABLE training_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    day_of_week VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_training_plans_user_id ON training_plans(user_id);

CREATE TABLE planned_exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    training_plan_id UUID NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    sets INTEGER NOT NULL,
    reps_min INTEGER NOT NULL,
    reps_max INTEGER NOT NULL,
    rest_seconds INTEGER,
    position INTEGER NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_planned_exercises_plan_id ON planned_exercises(training_plan_id);

ALTER TABLE workout_sessions
    ADD COLUMN training_plan_id UUID REFERENCES training_plans(id) ON DELETE SET NULL;

CREATE INDEX idx_workout_sessions_plan_id ON workout_sessions(training_plan_id);
