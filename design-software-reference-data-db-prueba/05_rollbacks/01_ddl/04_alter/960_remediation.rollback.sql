-- Rollback de remediation-reference-960 (VARCHAR+CHECK -> ENUM nativo)
CREATE TYPE institutional_structure.institutional_unit_type AS ENUM ('ACADEMIC','ADMINISTRATIVE','MIXED');
ALTER TABLE institutional_structure.institutional_unit DROP CONSTRAINT IF EXISTS ck_institutional_unit_unit_type;
ALTER TABLE institutional_structure.institutional_unit
    ALTER COLUMN unit_type TYPE institutional_structure.institutional_unit_type USING unit_type::institutional_structure.institutional_unit_type;
CREATE TYPE parameterization.parameter_value_type AS ENUM ('integer','string','boolean','json');
ALTER TABLE parameterization.parameter DROP CONSTRAINT IF EXISTS ck_parameter_value_type;
ALTER TABLE parameterization.parameter
    ALTER COLUMN value_type TYPE parameterization.parameter_value_type USING value_type::parameterization.parameter_value_type;
