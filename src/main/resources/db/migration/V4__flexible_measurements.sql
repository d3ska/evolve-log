-- Make weight optional and rename body measurement columns to be more specific
ALTER TABLE measurements ALTER COLUMN weight_kg DROP NOT NULL;

ALTER TABLE measurements RENAME COLUMN waist_cm TO waist_narrowest_cm;
ALTER TABLE measurements RENAME COLUMN arms_cm TO biceps_cm;
ALTER TABLE measurements RENAME COLUMN legs_cm TO thigh_cm;

ALTER TABLE measurements ADD COLUMN waist_navel_cm NUMERIC(5,1);
ALTER TABLE measurements ADD COLUMN calves_cm NUMERIC(5,1);
