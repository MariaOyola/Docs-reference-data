DROP INDEX IF EXISTS parameterization.ix_outbox_event_event_id;
DROP INDEX IF EXISTS parameterization.ix_outbox_event_status_attempts_created_at;
DROP INDEX IF EXISTS parameterization.ix_outbox_event_status_created_at;
DROP TABLE IF EXISTS parameterization.outbox_event;
