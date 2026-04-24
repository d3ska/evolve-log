-- Supplement catalog: what supplements exist
CREATE TABLE supplements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    brand       VARCHAR(200),
    form        VARCHAR(100),   -- capsule, powder, tablet, liquid, …
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Named supplement plans (e.g. "Morning stack", "Pre-workout")
CREATE TABLE supplement_plans (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Entries within a plan: which supplement, which time slot, dose, etc.
CREATE TABLE supplement_plan_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id         UUID        NOT NULL REFERENCES supplement_plans(id) ON DELETE CASCADE,
    supplement_id   UUID        NOT NULL REFERENCES supplements(id) ON DELETE CASCADE,
    time_slot       VARCHAR(20) NOT NULL,   -- MORNING, AFTERNOON, EVENING, CUSTOM
    custom_time     VARCHAR(20),            -- e.g. "14:30", only used when time_slot = CUSTOM
    dose_amount     NUMERIC(10, 3),
    dose_unit       VARCHAR(50),            -- mg, g, IU, capsule, ml, …
    notes           TEXT,
    sort_order      INT NOT NULL DEFAULT 0
);

-- Ad-hoc or plan-driven intake log
CREATE TABLE supplement_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    supplement_id   UUID        NOT NULL REFERENCES supplements(id) ON DELETE CASCADE,
    plan_entry_id   UUID        REFERENCES supplement_plan_entries(id) ON DELETE SET NULL,
    taken_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    dose_amount     NUMERIC(10, 3),
    dose_unit       VARCHAR(50),
    source          VARCHAR(20) NOT NULL DEFAULT 'SPONTANEOUS',  -- PLANNED, SPONTANEOUS
    notes           TEXT
);

CREATE INDEX ON supplements(user_id);
CREATE INDEX ON supplement_plans(user_id);
CREATE INDEX ON supplement_logs(user_id, taken_at DESC);
CREATE INDEX ON supplement_logs(supplement_id);
