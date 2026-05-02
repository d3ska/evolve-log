-- Active workout module: session status lifecycle + per-set completion flag

-- Session status: ACTIVE = in-progress, FINISHED = completed via /finish,
-- MANUAL = retroactively created without using the start flow
ALTER TABLE workout_sessions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

-- Backfill existing rows
UPDATE workout_sessions SET status = 'FINISHED' WHERE finished_at IS NOT NULL;
UPDATE workout_sessions SET status = 'ACTIVE'   WHERE started_at IS NOT NULL AND finished_at IS NULL;

-- Per-set completion flag (user ticks the set as done during active workout)
ALTER TABLE workout_sets
    ADD COLUMN completed BOOLEAN NOT NULL DEFAULT FALSE;

-- Enforce: at most one ACTIVE session per user at a time
CREATE UNIQUE INDEX uq_workout_sessions_one_active_per_user
    ON workout_sessions (user_id)
    WHERE status = 'ACTIVE';
