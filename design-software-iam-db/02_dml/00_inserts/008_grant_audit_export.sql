-- HU-AUD-004: export is an independent audit feature. Its role grants mirror
-- AUDIT_LOG_VIEW's documented GLOBAL and TRAINING_CENTER scopes.
INSERT INTO rbac.role_feature (role_id, feature_id, scope_type)
SELECT r.id, f.id, v.scope_type
FROM (VALUES
  ('SYSTEM_ADMIN', 'AUDIT_EXPORT', 'GLOBAL'),
  ('CENTER_DIRECTOR', 'AUDIT_EXPORT', 'TRAINING_CENTER')
) AS v(role_name, feature_code, scope_type)
JOIN rbac.role r ON r.name = v.role_name
JOIN rbac_catalog.feature f ON f.code = v.feature_code
ON CONFLICT (role_id, feature_id) DO UPDATE
SET scope_type = EXCLUDED.scope_type;
