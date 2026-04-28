-- A: Migrate exercise-level reps/weight into workout_sets.
-- For exercises that have sets+reps+weight at the exercise level but no
-- individual set rows yet, generate N identical rows (one per set).
-- Exercises with no weight data (bodyweight) are skipped intentionally.
INSERT INTO workout_sets (exercise_id, set_number, reps, weight_kg)
SELECT e.id,
       gs.n,
       e.reps,
       e.weight_kg
FROM exercises e
         CROSS JOIN LATERAL generate_series(1, e.sets) AS gs(n)
WHERE e.sets IS NOT NULL
  AND e.sets > 0
  AND (e.reps IS NOT NULL OR e.weight_kg IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM workout_sets ws WHERE ws.exercise_id = e.id);

-- B: Prevent duplicate rows on repeated Fitatu CSV imports.
-- ON CONFLICT (user_id, date, meal, food_name) DO NOTHING / DO UPDATE
-- will now work correctly at the DB level.
CREATE UNIQUE INDEX uq_fitatu_food_logs_dedup
    ON fitatu_food_logs (user_id, date, meal, food_name);
