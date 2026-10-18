-- HU-REF-018 rollback: las FK de sesión eliminan sus filas en cascada y la
-- auditoría conserva el registro con user_id en NULL, conforme al modelo IAM.
DELETE FROM identity."user"
WHERE id IN (
  'c0000000-0000-0000-0000-000000000018',
  'c0000000-0000-0000-0000-000000000019'
);
