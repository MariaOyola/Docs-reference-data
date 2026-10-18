-- HU-ACT-012: grants for company and activity log features already in the catalog.
INSERT INTO rbac.role_feature (id, role_id, feature_id, scope_type)
SELECT md5('hu-act-012-seed-actors-rbac-004:' || v.role_name || ':' || v.feature_code)::uuid,
       r.id, f.id, v.scope
FROM (VALUES
  ('SYSTEM_ADMIN', 'ACT_COMPANY_VIEW', 'GLOBAL'),
  ('CENTER_DIRECTOR', 'ACT_COMPANY_VIEW', 'TRAINING_CENTER'),
  ('COORDINATOR', 'ACT_COMPANY_VIEW', 'TRAINING_CENTER'),
  ('INSTRUCTOR', 'ACT_COMPANY_VIEW', 'OWN_FICHAS'),
  ('SYSTEM_ADMIN', 'ACT_COMPANY_MANAGE', 'GLOBAL'),
  ('CENTER_DIRECTOR', 'ACT_COMPANY_MANAGE', 'TRAINING_CENTER'),
  ('COORDINATOR', 'ACT_COMPANY_MANAGE', 'TRAINING_CENTER'),
  ('SYSTEM_ADMIN', 'ACT_ACTIVITY_LOG_VIEW', 'GLOBAL'),
  ('CENTER_DIRECTOR', 'ACT_ACTIVITY_LOG_VIEW', 'TRAINING_CENTER'),
  ('COORDINATOR', 'ACT_ACTIVITY_LOG_VIEW', 'TRAINING_CENTER')
) AS v(role_name, feature_code, scope)
JOIN rbac.role r ON r.name = v.role_name
JOIN rbac_catalog.feature f ON f.code = v.feature_code
ON CONFLICT DO NOTHING;
