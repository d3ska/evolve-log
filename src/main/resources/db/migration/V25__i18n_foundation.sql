-- V25 — i18n foundation
-- Adds user locale preference and exercise translation table (EN/PL seed data)

-- ============================================================
-- 1. User locale preference
-- ============================================================
ALTER TABLE users
    ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'en';

-- ============================================================
-- 2. Exercise translation table
-- ============================================================
CREATE TABLE exercise_definition_translations (
    exercise_definition_id UUID         NOT NULL REFERENCES exercise_definitions(id) ON DELETE CASCADE,
    locale                 VARCHAR(10)  NOT NULL,
    name                   VARCHAR(300) NOT NULL,
    description            TEXT,
    PRIMARY KEY (exercise_definition_id, locale)
);

CREATE INDEX idx_exercise_def_translations_locale
    ON exercise_definition_translations (locale);

-- ============================================================
-- 3. Seed EN translations from existing name / description
-- ============================================================
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name, description)
SELECT id, 'en', name, NULL
FROM exercise_definitions
WHERE is_system = true;

-- ============================================================
-- 4. Seed PL translations for all ~83 system exercises
-- ============================================================

-- CHEST
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie sztangi na ławce poziomej' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'barbell bench press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie hantli na ławce poziomej' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'dumbbell bench press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie sztangi na ławce skośnej górnej' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'incline barbell press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie hantli na ławce skośnej górnej' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'incline dumbbell press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie sztangi na ławce skośnej dolnej' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'decline barbell press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Rozpiętki na wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'cable fly';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Rozpiętki z hantlami' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'dumbbell fly';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Pompki' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'push-up';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Dipy na klatce piersiowej' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'chest dip';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Pec deck' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'pec deck';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie na maszynie Hammer Strength' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'hammer strength chest press';

-- BACK
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Martwy ciąg' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'barbell deadlift';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Podciąganie nachwytem' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'pull-up';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Podciąganie podchwytem' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'chin-up';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Podciąganie neutralnym chwytem' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'neutral grip pull-up';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Ściąganie drążka wyciągu pionowego' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'lat pulldown';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Ściąganie drążka wyciągu pionowego (podwójny)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'dual cable lat pulldown';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Ściąganie wyciągu pionowego w staniu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'standing cable lat pulldown';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wiosłowanie sztangą' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'barbell row';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wiosłowanie hantlem' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'dumbbell row';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wiosłowanie na wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'cable row';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wiosłowanie T-barem' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 't-bar row';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wiosłowanie siedząc na wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'seated cable row';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wiosłowanie na maszynie Hammer Strength' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'hammer strength row';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wiosłowanie w podporze na ławce' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'chest supported row';

-- SHOULDERS
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie sztangi nad głowę' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'barbell overhead press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie hantli nad głowę' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'dumbbell overhead press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie na ramiona na suwnicy' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'smith machine shoulder press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie metodą Arnolda' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'arnold press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Odwodzenie ramion w bok' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'lateral raise';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Unoszenie ramion przodem' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'front raise';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Odwodzenie ramion w bok na wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'cable lateral raise';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Podciąganie sztangi pod brodę' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'upright row';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Face pull' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'face pull';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Odwrotny pec deck' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'reverse pec deck';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Rozpiętki odwrotne na wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'reverse cable fly';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Rozpiętki tylnego deltoideu z hantlami' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'dumbbell rear delt fly';

-- BICEPS
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie ramion ze sztangą' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'barbell curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie ramion z hantlami' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'dumbbell curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie ramion chwytem młotkowym' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'hammer curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie ramion z hantlami na ławce skośnej' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'incline dumbbell curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie ramion na modlitewniku' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'preacher curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie ramion na wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'cable curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie ramion w skupieniu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'concentration curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie ramion na górnym wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'high cable bicep curl';

-- TRICEPS
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wyciskanie sztangi wąskim chwytem' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'close-grip bench press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Prostowanie ramion na wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'tricep pushdown';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Łamanie gryfu (skull crusher)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'skull crusher';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Prostowanie ramion nad głową' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'overhead tricep extension';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Pompki diamentowe' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'diamond push-up';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Dipy na triceps' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'tricep dip';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Kickback z hantlem' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'kickback';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Prostowanie ramion na wyciągu skrzyżowanym' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'cross cable tricep extension';

-- QUADS
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Przysiad ze sztangą z tyłu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'barbell back squat';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Przysiad ze sztangą z przodu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'barbell front squat';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Przysiad hack' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'hack squat';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wypychanie ciężaru nogami' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'leg press';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Prostowanie nóg na maszynie' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'leg extension';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Przysiad bułgarski' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'bulgarian split squat';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Przysiad z uniesionym przodem stopy' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'front foot elevated split squat';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Przysiad z hantlem (goblet squat)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'goblet squat';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wykroki' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'lunges';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wypychanie ciężaru jedną nogą' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'leg press (single leg)';

-- HAMSTRINGS
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Martwy ciąg rumuński' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'romanian deadlift';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie nóg w leżeniu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'lying leg curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie nóg w leżeniu (jedna noga)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'lying leg curl (single leg)';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Uginanie nóg w siadzie' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'seated leg curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Good morning' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'good morning';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Nordyckie uginanie nóg' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'nordic hamstring curl';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Martwy ciąg sumo' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'sumo deadlift';

-- GLUTES
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Hip thrust' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'hip thrust';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Unoszenie bioder w leżeniu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'glute bridge';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Odwodzenie nogi na wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'cable kickback';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wchodzenie na podest' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'step-up';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Przysiad sumo' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'sumo squat';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Prostowanie tułowia (hyperextension)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'back extension';

-- CALVES
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wspięcia na palce w staniu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'standing calf raise';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wspięcia na palce w siadzie' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'seated calf raise';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wspięcia na palce (osioł)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'donkey calf raise';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wspięcia na palce na jednej nodze' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'single-leg calf raise';

-- CORE
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Deska (plank)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'plank';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Brzuszki' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'crunch';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Unoszenie nóg w zwisie' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'hanging leg raise';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Unoszenie kolan w zwisie' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'hanging knee raise';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Rollout na kole' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'ab rollout';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Brzuszki na wyciągu' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'cable crunch';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Brzuszki rowerowe' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'bicycle crunch';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Rosyjski skręt' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'russian twist';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Dead bug' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'dead bug';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Deska boczna' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'side plank';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Hollow body' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'hollow body';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Woodchopper (siekacz)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'woodchopper';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Skręt z landminą' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'landmine twist';

-- TRAPS
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wznosy barków ze sztangą (shrug)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'barbell shrug';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Wznosy barków z hantlami (shrug)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'dumbbell shrug';
INSERT INTO exercise_definition_translations (exercise_definition_id, locale, name)
SELECT id, 'pl', 'Martwy ciąg z podpórek (rack pull)' FROM exercise_definitions WHERE is_system = true AND LOWER(name) = 'rack pull';
