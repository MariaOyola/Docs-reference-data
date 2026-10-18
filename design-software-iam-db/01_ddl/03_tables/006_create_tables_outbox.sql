
CREATE TABLE identity.outbox (
    id            UUID          NOT NULL DEFAULT gen_random_uuid(),
    event_id      UUID          NOT NULL DEFAULT gen_random_uuid(),
    event_type    TEXT          NOT NULL,
    payload       JSONB         NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,

    CONSTRAINT pk_outbox
        PRIMARY KEY (id),

    CONSTRAINT uq_outbox_event_id
        UNIQUE (event_id)
);

COMMENT ON TABLE identity.outbox IS 'Transactional outbox: domain events written in the same tx as the business state change; a relay publishes unpublished rows to the broker (at-least-once). See communication-patterns.md.';
