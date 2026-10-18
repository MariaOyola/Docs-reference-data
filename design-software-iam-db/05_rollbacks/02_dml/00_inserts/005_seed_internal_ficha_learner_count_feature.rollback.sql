-- Rollback for hu-act-012-seed-internal-ficha-learner-count-005.
-- Delete only the deterministic role_feature row created by this changeset.
DELETE FROM rbac.role_feature rf
USING rbac.role r, rbac_catalog.feature f
WHERE rf.role_id = r.id
  AND rf.feature_id = f.id
  AND r.name = 'SYSTEM_ADMIN'
  AND f.code = 'ACT_INTERNAL_FICHA_LEARNER_COUNT'
  AND rf.scope_type = 'GLOBAL'
  AND rf.id = md5('hu-act-012-seed-internal-ficha-learner-count-005:role-feature:' || r.name || ':ACT_INTERNAL_FICHA_LEARNER_COUNT')::uuid;

-- Delete only the feature row created by this changeset, after its grant is removed.
DELETE FROM rbac_catalog.feature
WHERE code = 'ACT_INTERNAL_FICHA_LEARNER_COUNT'
  AND id = md5('hu-act-012-seed-internal-ficha-learner-count-005:feature:ACT_INTERNAL_FICHA_LEARNER_COUNT')::uuid;
