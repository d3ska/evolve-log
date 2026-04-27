-- V18__ai_feature.sql
-- AI Personal Trainer: api key storage, insights, chat history, monthly aggregates

CREATE TABLE ai_settings (
    user_id           UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    provider          VARCHAR(50)  NOT NULL DEFAULT 'anthropic',
    api_key_encrypted TEXT         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE ai_insights (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type          VARCHAR(50) NOT NULL,
    period_start  DATE        NOT NULL,
    period_end    DATE        NOT NULL,
    content       TEXT        NOT NULL,
    model_used    VARCHAR(100) NOT NULL,
    generated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ai_insights UNIQUE (user_id, type, period_start)
);

CREATE INDEX idx_ai_insights_user_type ON ai_insights(user_id, type, period_start DESC);

CREATE TABLE ai_chat_history (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID        NOT NULL,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_chat_conv ON ai_chat_history(user_id, conversation_id, created_at DESC);

CREATE TABLE monthly_exercise_aggregates (
    id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_name          VARCHAR(255)  NOT NULL,
    exercise_definition_id UUID          REFERENCES exercise_definitions(id),
    year_month             DATE          NOT NULL,
    max_weight_kg          DECIMAL(8,2),
    total_volume_kg        DECIMAL(12,2),
    session_count          INT           NOT NULL DEFAULT 0,
    total_sets             INT           NOT NULL DEFAULT 0,
    computed_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_monthly_agg UNIQUE (user_id, exercise_name, year_month)
);

CREATE INDEX idx_monthly_agg_lookup ON monthly_exercise_aggregates(user_id, exercise_name, year_month DESC);
