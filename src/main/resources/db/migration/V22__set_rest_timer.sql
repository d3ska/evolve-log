-- Add completed_at timestamp to workout_sets for rest timer tracking.
-- Nullable with no default so existing rows are unaffected (historical sets show no rest times).
ALTER TABLE workout_sets
    ADD COLUMN completed_at TIMESTAMPTZ;
