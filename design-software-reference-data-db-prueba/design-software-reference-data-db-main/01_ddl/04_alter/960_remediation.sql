-- R3 remediación de congruencia (2026-08-06): ENUM nativo -> VARCHAR + CHECK (ADR-004).
-- Changeset NUEVO (remediation-reference-960); NO modifica changesets corridos.
-- Los valores existentes provienen del ENUM, así que satisfacen el CHECK.
ALTER TABLE institutional_structure.institutional_unit
    ALTER COLUMN unit_type TYPE VARCHAR(20) USING unit_type::text;
ALTER TABLE institutional_structure.institutional_unit
    ADD CONSTRAINT ck_institutional_unit_unit_type CHECK (unit_type IN ('ACADEMIC','ADMINISTRATIVE','MIXED'));
DROP TYPE IF EXISTS institutional_structure.institutional_unit_type;
ALTER TABLE parameterization.parameter
    ALTER COLUMN value_type TYPE VARCHAR(20) USING value_type::text;
ALTER TABLE parameterization.parameter
    ADD CONSTRAINT ck_parameter_value_type CHECK (value_type IN ('integer','string','boolean','json'));
DROP TYPE IF EXISTS parameterization.parameter_value_type;
