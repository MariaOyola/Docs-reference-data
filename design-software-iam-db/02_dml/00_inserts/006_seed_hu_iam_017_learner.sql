-- Local/integration fixture for HU-IAM-017 and HU-ACT-019.
-- Test credentials are injected by the local/CI environment; no plaintext password is stored here.
INSERT INTO identity."user" (id, email, password_hash, first_name, last_name, actor_type)
VALUES ('d1000000-0000-0000-0000-000000000001', 'hu-iam-017-learner@example.test',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'HU', 'Learner', 'LEARNER')
ON CONFLICT (id) DO NOTHING;
INSERT INTO rbac.user_role (user_id, role_id, assigned_by)
SELECT 'd1000000-0000-0000-0000-000000000001', r.id, 'c0000000-0000-0000-0000-000000000001'
FROM rbac.role r WHERE r.name = 'LEARNER'
ON CONFLICT DO NOTHING;
