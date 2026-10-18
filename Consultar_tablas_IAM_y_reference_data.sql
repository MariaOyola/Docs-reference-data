-- Ejecutar en la base design-software-develop.
-- Consultas de lectura según los DDL de ambos repositorios.
-- Seleccionar una consulta y pulsar F5 en Query Tool para ver su resultado.

-- IAM: identidad
SELECT * FROM identity."user";
SELECT * FROM identity.outbox;

-- IAM: catálogo de módulos y permisos
SELECT * FROM rbac_catalog.module;
SELECT * FROM rbac_catalog.feature;

-- IAM: roles y asignaciones
SELECT * FROM rbac.role;
SELECT * FROM rbac.role_feature;
SELECT * FROM rbac.user_role;
SELECT * FROM rbac.user_scope_override;

-- IAM: sesiones y recuperación de contraseña
SELECT * FROM session.refresh_token;
SELECT * FROM session.password_reset_request;

-- IAM: auditoría
SELECT * FROM identity_audit.audit_login;

-- Reference Data: geografía y estructura institucional
SELECT * FROM institutional_structure.macroregion;
SELECT * FROM institutional_structure.microregion;
SELECT * FROM institutional_structure.department;
SELECT * FROM institutional_structure.municipality;
SELECT * FROM institutional_structure.training_center;
SELECT * FROM institutional_structure.institutional_unit;

-- Reference Data: catálogos y parámetros
SELECT * FROM parameterization.catalog;
SELECT * FROM parameterization.catalog_detail;
SELECT * FROM parameterization.parameter;
