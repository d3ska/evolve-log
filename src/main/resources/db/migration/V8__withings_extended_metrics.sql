ALTER TABLE withings_measurements
    ADD COLUMN fat_free_mass_kg      NUMERIC(5, 2),
    ADD COLUMN fat_mass_weight_kg    NUMERIC(5, 2),
    ADD COLUMN heart_pulse_bpm       INTEGER,
    ADD COLUMN hydration_kg          NUMERIC(5, 2),
    ADD COLUMN pulse_wave_velocity   NUMERIC(5, 2),
    ADD COLUMN vo2_max               NUMERIC(5, 2),
    ADD COLUMN vascular_age          INTEGER,
    ADD COLUMN nerve_health_score    INTEGER,
    ADD COLUMN visceral_fat          INTEGER,
    ADD COLUMN basal_metabolic_rate  INTEGER,
    ADD COLUMN metabolic_age         INTEGER;
