-- Raw per-item food log from Fitatu CSV exports.
-- Daily aggregates go into health_metrics; this table preserves
-- the original rows (food name, meal, quantity, all nutrients)
-- for future per-food analysis and correlation queries.

CREATE TABLE fitatu_food_logs (
    id          UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID                     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date        DATE                     NOT NULL,
    meal        VARCHAR(100)             NOT NULL DEFAULT '',
    food_name   VARCHAR(500)             NOT NULL DEFAULT '',
    quantity_g  NUMERIC(10, 2),
    nutrients   JSONB                    NOT NULL DEFAULT '{}',
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fitatu_food_logs_dedup UNIQUE (user_id, date, meal, food_name)
);

CREATE INDEX idx_fitatu_food_logs_user_date
    ON fitatu_food_logs (user_id, date DESC);
