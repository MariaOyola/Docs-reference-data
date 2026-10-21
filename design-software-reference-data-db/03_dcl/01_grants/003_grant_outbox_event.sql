GRANT SELECT, INSERT, UPDATE, DELETE ON parameterization.outbox_event TO reference_writer;
GRANT SELECT ON parameterization.outbox_event TO reference_reader;
