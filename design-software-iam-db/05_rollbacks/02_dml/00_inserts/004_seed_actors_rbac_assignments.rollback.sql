-- Delete only rows created with this changeset's deterministic IDs.
DELETE FROM rbac.role_feature rf
USING rbac.role r, rbac_catalog.feature f
WHERE rf.role_id = r.id
  AND rf.feature_id = f.id
  AND (r.name, f.code, rf.scope_type) IN (VALUES
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
  )
  AND rf.id = md5('hu-act-012-seed-actors-rbac-004:' || r.name || ':' || f.code)::uuid;
