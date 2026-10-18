
CREATE INDEX ix_outbox_unpublished
    ON identity.outbox (created_at)
    WHERE published_at IS NULL;
