CREATE TABLE withings_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    access_token    TEXT NOT NULL,
    refresh_token   TEXT NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE withings_measurements (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date             DATE NOT NULL,
    weight_kg        NUMERIC(5,2),
    body_fat_percent NUMERIC(4,1),
    muscle_mass_kg   NUMERIC(5,2),
    bone_mass_kg     NUMERIC(4,2),
    recorded_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(user_id, date)
);

CREATE INDEX idx_withings_measurements_user_date
    ON withings_measurements(user_id, date DESC);
