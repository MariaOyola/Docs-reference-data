CREATE TABLE parameterization.outbox_event (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id     UUID NOT NULL UNIQUE,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type   VARCHAR(120) NOT NULL,
    payload      JSONB NOT NULL,
    status       VARCHAR(30) NOT NULL DEFAULT 'pending',
    attempts     INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at      TIMESTAMPTZ NULL,
    last_error   TEXT NULL,

    CONSTRAINT chk_outbox_status
        CHECK (status IN ('pending', 'publishing', 'published', 'failed'))
);

CREATE INDEX ix_outbox_event_status_created_at
    ON parameterization.outbox_event (status, created_at);

CREATE INDEX ix_outbox_event_event_id
    ON parameterization.outbox_event (event_id);

CREATE INDEX ix_outbox_event_status_attempts_created_at
    ON parameterization.outbox_event (status, attempts, created_at);
