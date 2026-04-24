-- Replace the wide withings_measurements table (and V8 extensions) with a
-- generic EAV health_metrics table that works across any device source.
-- Dev data loss is acceptable; a Withings re-sync will repopulate.

DROP TABLE IF EXISTS withings_measurements;

CREATE TABLE health_metrics (
    id          UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID                     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source      VARCHAR(50)              NOT NULL,
    date        DATE                     NOT NULL,
    metric_key  VARCHAR(100)             NOT NULL,
    value       NUMERIC(12, 4)           NOT NULL,
    unit        VARCHAR(20),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (user_id, source, date, metric_key)
);

-- Primary access pattern: all metrics for a user from a source, ordered by date
CREATE INDEX idx_health_metrics_user_source_date
    ON health_metrics (user_id, source, date DESC);

-- Trend queries: one metric for a user over time (e.g., weight trend)
CREATE INDEX idx_health_metrics_user_metric_date
    ON health_metrics (user_id, metric_key, date DESC);
