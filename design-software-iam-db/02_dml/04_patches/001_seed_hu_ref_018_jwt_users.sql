-- HU-REF-018: fixture de integración exclusiva para los contextos dev y qa.
-- Las contraseñas no se versionan: el entorno de prueba aporta los valores que
-- verifican estos hashes BCrypt. Los correos .test no pertenecen a usuarios reales.
INSERT INTO identity."user" (id, email, password_hash, first_name, last_name, actor_type, is_active)
VALUES
  ('c0000000-0000-0000-0000-000000000018', 'hu-ref-018-admin@example.test', '$2a$12$BXEZXdaQEr3aHmNVL0CWf.3G7EkV8268bXU6TSNl3Jr9CiNOWN1hu', 'HU REF 018', 'Parameter Admin', 'USER', TRUE),
  ('c0000000-0000-0000-0000-000000000019', 'hu-ref-018-learner@example.test', '$2a$12$BXEZXdaQEr3aHmNVL0CWf.3G7EkV8268bXU6TSNl3Jr9CiNOWN1hu', 'HU REF 018', 'Parameter Learner', 'LEARNER', TRUE)
ON CONFLICT (id) DO NOTHING;

-- ADMIN_STAFF recibe REF_PARAMETER_VIEW y REF_PARAMETER_MANAGE:GLOBAL en el seed RBAC oficial.
INSERT INTO rbac.user_role (user_id, role_id, assigned_by)
SELECT 'c0000000-0000-0000-0000-000000000018', r.id, 'c0000000-0000-0000-0000-000000000018'
FROM rbac.role r
WHERE r.name = 'ADMIN_STAFF'
ON CONFLICT DO NOTHING;

-- LEARNER no tiene ninguno de los features de parámetros y prueba el 403 real.
INSERT INTO rbac.user_role (user_id, role_id, assigned_by)
SELECT 'c0000000-0000-0000-0000-000000000019', r.id, 'c0000000-0000-0000-0000-000000000019'
FROM rbac.role r
WHERE r.name = 'LEARNER'
ON CONFLICT DO NOTHING;
