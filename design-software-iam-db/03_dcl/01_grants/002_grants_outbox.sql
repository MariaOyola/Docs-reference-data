-- Least-privilege grants for identity.outbox (additive, targeted).
-- Targeted (not "ON ALL TABLES") so it also applies on incremental runs, where
-- the base grants changeset (dcl-grants-iam-001) already ran and will not re-run.
GRANT SELECT                         ON identity.outbox TO iam_reader;
GRANT SELECT, INSERT, UPDATE, DELETE ON identity.outbox TO iam_writer;
GRANT ALL PRIVILEGES                 ON identity.outbox TO iam_admin;
