CREATE TABLE exercise_definitions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(100) NOT NULL,
    primary_muscle   VARCHAR(50)  NOT NULL,
    secondary_muscles TEXT[]       NOT NULL DEFAULT '{}',
    equipment        VARCHAR(50),
    is_system        BOOLEAN      NOT NULL DEFAULT false,
    user_id          UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Unique constraint: system exercise names must be globally unique (case-insensitive)
CREATE UNIQUE INDEX idx_exercise_definitions_system_name
    ON exercise_definitions (LOWER(name))
    WHERE is_system = true;

-- Index for user-defined exercises
CREATE INDEX idx_exercise_definitions_user_id
    ON exercise_definitions (user_id)
    WHERE is_system = false;

-- ============================================================
-- Seed data: ~80 common system exercises
-- ============================================================

INSERT INTO exercise_definitions (name, primary_muscle, secondary_muscles, equipment, is_system) VALUES

-- CHEST
('Barbell Bench Press',       'chest', ARRAY['triceps','shoulders'], 'barbell', true),
('Dumbbell Bench Press',      'chest', ARRAY['triceps','shoulders'], 'dumbbell', true),
('Incline Barbell Press',     'chest', ARRAY['triceps','shoulders'], 'barbell', true),
('Incline Dumbbell Press',    'chest', ARRAY['triceps','shoulders'], 'dumbbell', true),
('Decline Barbell Press',     'chest', ARRAY['triceps'], 'barbell', true),
('Cable Fly',                 'chest', ARRAY[]::text[], 'cable', true),
('Dumbbell Fly',              'chest', ARRAY[]::text[], 'dumbbell', true),
('Push-Up',                   'chest', ARRAY['triceps','shoulders'], 'bodyweight', true),
('Chest Dip',                 'chest', ARRAY['triceps'], 'bodyweight', true),

-- BACK
('Barbell Deadlift',          'back',  ARRAY['glutes','hamstrings','traps'], 'barbell', true),
('Pull-Up',                   'back',  ARRAY['biceps'], 'bodyweight', true),
('Chin-Up',                   'back',  ARRAY['biceps'], 'bodyweight', true),
('Barbell Row',               'back',  ARRAY['biceps','rear delts'], 'barbell', true),
('Dumbbell Row',              'back',  ARRAY['biceps'], 'dumbbell', true),
('Cable Row',                 'back',  ARRAY['biceps'], 'cable', true),
('Lat Pulldown',              'back',  ARRAY['biceps'], 'cable', true),
('T-Bar Row',                 'back',  ARRAY['biceps'], 'barbell', true),
('Face Pull',                 'back',  ARRAY['rear delts','external rotators'], 'cable', true),
('Seated Cable Row',          'back',  ARRAY['biceps'], 'cable', true),

-- SHOULDERS
('Barbell Overhead Press',    'shoulders', ARRAY['triceps','upper traps'], 'barbell', true),
('Dumbbell Overhead Press',   'shoulders', ARRAY['triceps'], 'dumbbell', true),
('Lateral Raise',             'shoulders', ARRAY[]::text[], 'dumbbell', true),
('Front Raise',               'shoulders', ARRAY[]::text[], 'dumbbell', true),
('Arnold Press',              'shoulders', ARRAY['triceps'], 'dumbbell', true),
('Upright Row',               'shoulders', ARRAY['traps'], 'barbell', true),
('Cable Lateral Raise',       'shoulders', ARRAY[]::text[], 'cable', true),

-- BICEPS
('Barbell Curl',              'biceps', ARRAY['forearms'], 'barbell', true),
('Dumbbell Curl',             'biceps', ARRAY['forearms'], 'dumbbell', true),
('Hammer Curl',               'biceps', ARRAY['forearms','brachialis'], 'dumbbell', true),
('Incline Dumbbell Curl',     'biceps', ARRAY[]::text[], 'dumbbell', true),
('Preacher Curl',             'biceps', ARRAY[]::text[], 'barbell', true),
('Cable Curl',                'biceps', ARRAY[]::text[], 'cable', true),
('Concentration Curl',        'biceps', ARRAY[]::text[], 'dumbbell', true),

-- TRICEPS
('Close-Grip Bench Press',    'triceps', ARRAY['chest'], 'barbell', true),
('Tricep Pushdown',           'triceps', ARRAY[]::text[], 'cable', true),
('Skull Crusher',             'triceps', ARRAY[]::text[], 'barbell', true),
('Overhead Tricep Extension', 'triceps', ARRAY[]::text[], 'dumbbell', true),
('Diamond Push-Up',           'triceps', ARRAY['chest'], 'bodyweight', true),
('Tricep Dip',                'triceps', ARRAY['chest'], 'bodyweight', true),
('Kickback',                  'triceps', ARRAY[]::text[], 'dumbbell', true),

-- QUADS
('Barbell Back Squat',        'quads', ARRAY['glutes','hamstrings'], 'barbell', true),
('Barbell Front Squat',       'quads', ARRAY['core'], 'barbell', true),
('Hack Squat',                'quads', ARRAY['glutes'], 'machine', true),
('Leg Press',                 'quads', ARRAY['glutes','hamstrings'], 'machine', true),
('Leg Extension',             'quads', ARRAY[]::text[], 'machine', true),
('Bulgarian Split Squat',     'quads', ARRAY['glutes','hamstrings'], 'dumbbell', true),
('Goblet Squat',              'quads', ARRAY['glutes'], 'dumbbell', true),
('Lunges',                    'quads', ARRAY['glutes','hamstrings'], 'bodyweight', true),

-- HAMSTRINGS
('Romanian Deadlift',         'hamstrings', ARRAY['glutes','back'], 'barbell', true),
('Lying Leg Curl',            'hamstrings', ARRAY[]::text[], 'machine', true),
('Seated Leg Curl',           'hamstrings', ARRAY[]::text[], 'machine', true),
('Good Morning',              'hamstrings', ARRAY['back','glutes'], 'barbell', true),
('Sumo Deadlift',             'hamstrings', ARRAY['glutes','back'], 'barbell', true),
('Nordic Hamstring Curl',     'hamstrings', ARRAY[]::text[], 'bodyweight', true),

-- GLUTES
('Hip Thrust',                'glutes', ARRAY['hamstrings'], 'barbell', true),
('Glute Bridge',              'glutes', ARRAY['hamstrings'], 'bodyweight', true),
('Cable Kickback',            'glutes', ARRAY[]::text[], 'cable', true),
('Step-Up',                   'glutes', ARRAY['quads'], 'dumbbell', true),
('Sumo Squat',                'glutes', ARRAY['quads','hamstrings'], 'dumbbell', true),

-- CALVES
('Standing Calf Raise',       'calves', ARRAY[]::text[], 'machine', true),
('Seated Calf Raise',         'calves', ARRAY[]::text[], 'machine', true),
('Donkey Calf Raise',         'calves', ARRAY[]::text[], 'bodyweight', true),
('Single-Leg Calf Raise',     'calves', ARRAY[]::text[], 'bodyweight', true),

-- CORE
('Plank',                     'core', ARRAY[]::text[], 'bodyweight', true),
('Crunch',                    'core', ARRAY[]::text[], 'bodyweight', true),
('Hanging Leg Raise',         'core', ARRAY['hip flexors'], 'bodyweight', true),
('Ab Rollout',                'core', ARRAY[]::text[], 'other', true),
('Cable Crunch',              'core', ARRAY[]::text[], 'cable', true),
('Bicycle Crunch',            'core', ARRAY['obliques'], 'bodyweight', true),
('Russian Twist',             'core', ARRAY['obliques'], 'bodyweight', true),
('Dead Bug',                  'core', ARRAY[]::text[], 'bodyweight', true),
('Side Plank',                'core', ARRAY['obliques'], 'bodyweight', true),

-- TRAPS / UPPER BACK
('Barbell Shrug',             'traps', ARRAY[]::text[], 'barbell', true),
('Dumbbell Shrug',            'traps', ARRAY[]::text[], 'dumbbell', true),
('Rack Pull',                 'traps', ARRAY['back'], 'barbell', true);
