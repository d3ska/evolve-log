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
('Barbell Bench Press',       'chest', ARRAY['triceps','shoulders'],          'barbell',    true),
('Dumbbell Bench Press',      'chest', ARRAY['triceps','shoulders'],          'dumbbell',   true),
('Incline Barbell Press',     'chest', ARRAY['triceps','shoulders'],          'barbell',    true),
('Incline Dumbbell Press',    'chest', ARRAY['triceps','shoulders'],          'dumbbell',   true),
('Decline Barbell Press',     'chest', ARRAY['triceps'],                      'barbell',    true),
('Cable Fly',                 'chest', ARRAY[]::text[],                       'cable',      true),
('Dumbbell Fly',              'chest', ARRAY[]::text[],                       'dumbbell',   true),
('Push-Up',                   'chest', ARRAY['triceps','shoulders'],          'bodyweight', true),
('Chest Dip',                 'chest', ARRAY['triceps'],                      'bodyweight', true),
('Pec Deck',                  'chest', ARRAY[]::text[],                       'machine',    true),
('Hammer Strength Chest Press','chest',ARRAY['triceps','shoulders'],          'machine',    true),

-- BACK
-- ↳ Core: add 'core' to Barbell Deadlift secondaries
('Barbell Deadlift',          'back',  ARRAY['glutes','hamstrings','traps','core'], 'barbell', true),
-- ↳ Pulls: add 'forearms' to all vertical pulls
('Pull-Up',                   'back',  ARRAY['biceps','forearms'],            'bodyweight', true),
('Chin-Up',                   'back',  ARRAY['biceps','forearms'],            'bodyweight', true),
('Neutral Grip Pull-Up',      'back',  ARRAY['biceps','forearms'],            'bodyweight', true),
('Lat Pulldown',              'back',  ARRAY['biceps','forearms'],            'cable',      true),
('Dual Cable Lat Pulldown',   'back',  ARRAY['biceps','forearms'],            'cable',      true),
('Standing Cable Lat Pulldown','back', ARRAY['biceps','forearms'],            'cable',      true),
-- ↳ Rows: add 'rear delts' to all row movements
('Barbell Row',               'back',  ARRAY['biceps','rear delts'],          'barbell',    true),
('Dumbbell Row',              'back',  ARRAY['biceps','rear delts'],          'dumbbell',   true),
('Cable Row',                 'back',  ARRAY['biceps','rear delts'],          'cable',      true),
('T-Bar Row',                 'back',  ARRAY['biceps','rear delts'],          'barbell',    true),
('Seated Cable Row',          'back',  ARRAY['biceps','rear delts'],          'cable',      true),
('Hammer Strength Row',       'back',  ARRAY['biceps','rear delts'],          'machine',    true),
('Chest Supported Row',       'back',  ARRAY['biceps','rear delts'],          'dumbbell',   true),

-- SHOULDERS
-- ↳ Pressing: add 'core' to Barbell Overhead Press secondaries
('Barbell Overhead Press',    'shoulders', ARRAY['triceps','upper traps','core'], 'barbell', true),
('Dumbbell Overhead Press',   'shoulders', ARRAY['triceps'],                  'dumbbell',   true),
('Smith Machine Shoulder Press','shoulders',ARRAY['triceps'],                 'machine',    true),
('Arnold Press',              'shoulders', ARRAY['triceps'],                  'dumbbell',   true),
-- ↳ Raises
('Lateral Raise',             'shoulders', ARRAY[]::text[],                   'dumbbell',   true),
('Front Raise',               'shoulders', ARRAY[]::text[],                   'dumbbell',   true),
('Cable Lateral Raise',       'shoulders', ARRAY[]::text[],                   'cable',      true),
('Upright Row',               'shoulders', ARRAY['traps'],                    'barbell',    true),
-- ↳ Rear delt isolation: primary_muscle = 'shoulders' (not 'back')
('Face Pull',                 'shoulders', ARRAY['rear delts','external rotators'], 'cable', true),
('Reverse Pec Deck',          'shoulders', ARRAY['rear delts'],               'machine',    true),
('Reverse Cable Fly',         'shoulders', ARRAY['rear delts'],               'cable',      true),
('Dumbbell Rear Delt Fly',    'shoulders', ARRAY['rear delts'],               'dumbbell',   true),

-- BICEPS
-- ↳ Ensure 'forearms' on all curl variations
('Barbell Curl',              'biceps', ARRAY['forearms'],                    'barbell',    true),
('Dumbbell Curl',             'biceps', ARRAY['forearms'],                    'dumbbell',   true),
('Hammer Curl',               'biceps', ARRAY['forearms','brachialis'],       'dumbbell',   true),
('Incline Dumbbell Curl',     'biceps', ARRAY['forearms'],                    'dumbbell',   true),
('Preacher Curl',             'biceps', ARRAY['forearms'],                    'barbell',    true),
('Cable Curl',                'biceps', ARRAY['forearms'],                    'cable',      true),
('Concentration Curl',        'biceps', ARRAY['forearms'],                    'dumbbell',   true),
('High Cable Bicep Curl',     'biceps', ARRAY['forearms'],                    'cable',      true),

-- TRICEPS
('Close-Grip Bench Press',    'triceps', ARRAY['chest'],                      'barbell',    true),
('Tricep Pushdown',           'triceps', ARRAY[]::text[],                     'cable',      true),
('Skull Crusher',             'triceps', ARRAY[]::text[],                     'barbell',    true),
('Overhead Tricep Extension', 'triceps', ARRAY[]::text[],                     'dumbbell',   true),
('Diamond Push-Up',           'triceps', ARRAY['chest'],                      'bodyweight', true),
('Tricep Dip',                'triceps', ARRAY['chest'],                      'bodyweight', true),
('Kickback',                  'triceps', ARRAY[]::text[],                     'dumbbell',   true),
('Cross Cable Tricep Extension','triceps',ARRAY[]::text[],                    'cable',      true),

-- QUADS
-- ↳ Add 'core' to Barbell Back Squat secondaries
('Barbell Back Squat',        'quads', ARRAY['glutes','hamstrings','core'],   'barbell',    true),
('Barbell Front Squat',       'quads', ARRAY['core'],                         'barbell',    true),
('Hack Squat',                'quads', ARRAY['glutes'],                       'machine',    true),
('Leg Press',                 'quads', ARRAY['glutes','hamstrings'],          'machine',    true),
('Leg Extension',             'quads', ARRAY[]::text[],                       'machine',    true),
('Bulgarian Split Squat',     'quads', ARRAY['glutes','hamstrings'],          'dumbbell',   true),
('Front Foot Elevated Split Squat','quads',ARRAY['glutes','hamstrings'],      'dumbbell',   true),
('Goblet Squat',              'quads', ARRAY['glutes'],                       'dumbbell',   true),
('Lunges',                    'quads', ARRAY['glutes','hamstrings'],          'bodyweight', true),
('Leg Press (Single Leg)',    'quads', ARRAY['glutes','hamstrings'],          'machine',    true),

-- HAMSTRINGS
('Romanian Deadlift',         'hamstrings', ARRAY['glutes','back'],           'barbell',    true),
('Lying Leg Curl',            'hamstrings', ARRAY[]::text[],                  'machine',    true),
('Lying Leg Curl (Single Leg)','hamstrings',ARRAY[]::text[],                  'machine',    true),
('Seated Leg Curl',           'hamstrings', ARRAY[]::text[],                  'machine',    true),
('Good Morning',              'hamstrings', ARRAY['back','glutes'],           'barbell',    true),
('Nordic Hamstring Curl',     'hamstrings', ARRAY[]::text[],                  'bodyweight', true),
-- ↳ Sumo Deadlift: primary = 'glutes' (wider stance shifts load to glutes)
('Sumo Deadlift',             'glutes',     ARRAY['hamstrings','back'],       'barbell',    true),

-- GLUTES
('Hip Thrust',                'glutes', ARRAY['hamstrings'],                  'barbell',    true),
('Glute Bridge',              'glutes', ARRAY['hamstrings'],                  'bodyweight', true),
('Cable Kickback',            'glutes', ARRAY[]::text[],                      'cable',      true),
('Step-Up',                   'glutes', ARRAY['quads'],                       'dumbbell',   true),
('Sumo Squat',                'glutes', ARRAY['quads','hamstrings'],          'dumbbell',   true),

-- CALVES
('Standing Calf Raise',       'calves', ARRAY[]::text[],                      'machine',    true),
('Seated Calf Raise',         'calves', ARRAY[]::text[],                      'machine',    true),
('Donkey Calf Raise',         'calves', ARRAY[]::text[],                      'bodyweight', true),
('Single-Leg Calf Raise',     'calves', ARRAY[]::text[],                      'bodyweight', true),

-- CORE
('Plank',                     'core', ARRAY[]::text[],                        'bodyweight', true),
('Crunch',                    'core', ARRAY[]::text[],                        'bodyweight', true),
('Hanging Leg Raise',         'core', ARRAY['hip flexors'],                   'bodyweight', true),
('Hanging Knee Raise',        'core', ARRAY['hip flexors'],                   'bodyweight', true),
-- ↳ Ab Rollout: equipment = 'bodyweight' (roller is bodyweight-loaded)
('Ab Rollout',                'core', ARRAY[]::text[],                        'bodyweight', true),
('Cable Crunch',              'core', ARRAY[]::text[],                        'cable',      true),
('Bicycle Crunch',            'core', ARRAY['obliques'],                      'bodyweight', true),
('Russian Twist',             'core', ARRAY['obliques'],                      'bodyweight', true),
('Dead Bug',                  'core', ARRAY[]::text[],                        'bodyweight', true),
('Side Plank',                'core', ARRAY['obliques'],                      'bodyweight', true),
('Hollow Body',               'core', ARRAY[]::text[],                        'bodyweight', true),
('Woodchopper',               'core', ARRAY['obliques'],                      'cable',      true),
('Landmine Twist',            'core', ARRAY['obliques','shoulders'],          'barbell',    true),
-- ↳ Back Extension: primary = 'glutes' (hip-hinge movement, glutes are primary driver)
('Back Extension',            'glutes', ARRAY['back','hamstrings'],           'machine',    true),

-- TRAPS / UPPER BACK
('Barbell Shrug',             'traps', ARRAY[]::text[],                       'barbell',    true),
('Dumbbell Shrug',            'traps', ARRAY[]::text[],                       'dumbbell',   true),
('Rack Pull',                 'traps', ARRAY['back'],                         'barbell',    true);
