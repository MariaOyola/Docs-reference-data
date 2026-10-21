-- Grants least-privilege dominio reference
GRANT USAGE ON SCHEMA institutional_structure TO reference_reader, reference_writer, reference_admin;
GRANT SELECT ON ALL TABLES IN SCHEMA institutional_structure TO reference_reader;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA institutional_structure TO reference_writer;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA institutional_structure TO reference_writer;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA institutional_structure TO reference_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA institutional_structure TO reference_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA institutional_structure GRANT SELECT ON TABLES TO reference_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA institutional_structure GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO reference_writer;
ALTER DEFAULT PRIVILEGES IN SCHEMA institutional_structure GRANT ALL ON TABLES TO reference_admin;
GRANT USAGE ON SCHEMA parameterization TO reference_reader, reference_writer, reference_admin;
GRANT SELECT ON ALL TABLES IN SCHEMA parameterization TO reference_reader;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA parameterization TO reference_writer;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA parameterization TO reference_writer;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA parameterization TO reference_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA parameterization TO reference_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA parameterization GRANT SELECT ON TABLES TO reference_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA parameterization GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO reference_writer;
ALTER DEFAULT PRIVILEGES IN SCHEMA parameterization GRANT ALL ON TABLES TO reference_admin;
