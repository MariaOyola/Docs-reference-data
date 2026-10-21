-- Roles de aplicacion (least-privilege) para el dominio reference
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='reference_reader') THEN CREATE ROLE reference_reader NOLOGIN; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='reference_writer') THEN CREATE ROLE reference_writer NOLOGIN; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='reference_admin')  THEN CREATE ROLE reference_admin  NOLOGIN; END IF;
END $$;
