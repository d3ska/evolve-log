CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE measurements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    weight_kg NUMERIC(5,2) NOT NULL,
    body_fat_percent NUMERIC(4,1),
    chest_cm NUMERIC(5,1),
    waist_cm NUMERIC(5,1),
    arms_cm NUMERIC(5,1),
    legs_cm NUMERIC(5,1),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_measurements_user_id ON measurements(user_id);
CREATE INDEX idx_measurements_user_date ON measurements(user_id, date DESC);

CREATE TABLE workout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workout_sessions_user_id ON workout_sessions(user_id);
CREATE INDEX idx_workout_sessions_user_date ON workout_sessions(user_id, date DESC);

CREATE TABLE exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    sets INTEGER NOT NULL,
    reps INTEGER NOT NULL,
    weight_kg NUMERIC(6,2),
    notes TEXT,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exercises_workout_session_id ON exercises(workout_session_id);

CREATE TABLE progress_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    category VARCHAR(20) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_progress_photos_user_id ON progress_photos(user_id);
CREATE INDEX idx_progress_photos_user_date ON progress_photos(user_id, date DESC);
