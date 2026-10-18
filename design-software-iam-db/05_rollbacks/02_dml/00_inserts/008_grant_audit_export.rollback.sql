-- Roll back only the grants introduced for HU-AUD-004.
DELETE FROM rbac.role_feature rf
USING rbac.role r, rbac_catalog.feature f
WHERE rf.role_id = r.id
  AND rf.feature_id = f.id
  AND f.code = 'AUDIT_EXPORT'
  AND r.name IN ('SYSTEM_ADMIN', 'CENTER_DIRECTOR');
