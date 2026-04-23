CREATE TABLE media_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    media_type VARCHAR(10) NOT NULL,
    original_filename VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_attachments_user_id ON media_attachments(user_id);
CREATE INDEX idx_media_attachments_user_date ON media_attachments(user_id, date DESC);
