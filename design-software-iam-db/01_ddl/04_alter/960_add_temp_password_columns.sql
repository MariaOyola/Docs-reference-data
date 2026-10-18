ALTER TABLE identity."user"
    ADD COLUMN IF NOT EXISTS must_change_password       BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS password_temporary_expires_at TIMESTAMPTZ;