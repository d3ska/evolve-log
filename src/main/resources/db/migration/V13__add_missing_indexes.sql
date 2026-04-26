-- Missing FK indexes identified in deep review
CREATE INDEX IF NOT EXISTS idx_supplement_plan_entries_plan_id
    ON supplement_plan_entries(plan_id);

CREATE INDEX IF NOT EXISTS idx_supplement_plan_entries_supplement_id
    ON supplement_plan_entries(supplement_id);

CREATE INDEX IF NOT EXISTS idx_supplement_logs_plan_entry_id
    ON supplement_logs(plan_entry_id);
