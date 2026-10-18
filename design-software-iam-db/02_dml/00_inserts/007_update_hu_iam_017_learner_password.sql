UPDATE identity."user"
SET password_hash = '$2a$10$jIgda6NNhnVcDMOvUIIQtuVC6560hcg0MVcJndwDYPIomTB1NDtn6',
    must_change_password = false,
    password_temporary_expires_at = NULL
WHERE id = 'd1000000-0000-0000-0000-000000000001'
  AND email = 'hu-iam-017-learner@example.test';
