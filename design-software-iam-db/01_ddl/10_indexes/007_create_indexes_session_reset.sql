CREATE INDEX IF NOT EXISTS idx_password_reset_token_hash
    ON session.password_reset_request (token_hash);