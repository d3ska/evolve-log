CREATE TABLE blood_test_reports (
    id          UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID                     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date        DATE                     NOT NULL,
    lab_name    VARCHAR(200),
    filename    VARCHAR(500),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE blood_test_results (
    id                UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id         UUID    NOT NULL REFERENCES blood_test_reports(id) ON DELETE CASCADE,
    parameter_key     VARCHAR(100) NOT NULL,
    parameter_label   VARCHAR(200) NOT NULL,
    value             NUMERIC(12, 4) NOT NULL,
    unit              VARCHAR(50),
    ref_low           NUMERIC(12, 4),
    ref_high          NUMERIC(12, 4),
    flag              VARCHAR(10),
    category          VARCHAR(100)
);

CREATE INDEX idx_blood_test_reports_user_date ON blood_test_reports(user_id, date DESC);
CREATE INDEX idx_blood_test_results_report    ON blood_test_results(report_id);
CREATE INDEX idx_blood_test_results_key       ON blood_test_results(parameter_key);
