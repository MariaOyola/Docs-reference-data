-- HU-ACT-012 / HU-SOL-ACT-001: internal feature required by actors-service.
-- The endpoint GET /internal/fichas/{fichaId}/active-learners/count checks this authority.
INSERT INTO rbac_catalog.feature (id, module_id, code, name, description, action_level)
SELECT md5('hu-act-012-seed-internal-ficha-learner-count-005:feature:ACT_INTERNAL_FICHA_LEARNER_COUNT')::uuid,
       m.id,
       'ACT_INTERNAL_FICHA_LEARNER_COUNT',
       'Conteo interno de aprendices activos por ficha',
       'Permite consultar desde integraciones internas el numero de aprendices activos por ficha en actors-service.',
       'READ'
FROM rbac_catalog.module m
WHERE m.code = 'MOD_ACTORS'
ON CONFLICT (code) DO NOTHING;

-- Current IAM DB has no service-account/client-feature table. This grants the
-- feature through the existing RBAC model; IAM/Architecture must bind the
-- academic-management service principal to the appropriate role/token flow.
INSERT INTO rbac.role_feature (id, role_id, feature_id, scope_type)
SELECT md5('hu-act-012-seed-internal-ficha-learner-count-005:role-feature:' || r.name || ':ACT_INTERNAL_FICHA_LEARNER_COUNT')::uuid,
       r.id,
       f.id,
       'GLOBAL'
FROM rbac.role r
JOIN rbac_catalog.feature f ON f.code = 'ACT_INTERNAL_FICHA_LEARNER_COUNT'
WHERE r.name = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, feature_id) DO NOTHING;
