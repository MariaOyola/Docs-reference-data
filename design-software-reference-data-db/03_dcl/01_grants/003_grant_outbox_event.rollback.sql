REVOKE SELECT ON parameterization.outbox_event FROM reference_reader;
REVOKE SELECT, INSERT, UPDATE, DELETE ON parameterization.outbox_event FROM reference_writer;
