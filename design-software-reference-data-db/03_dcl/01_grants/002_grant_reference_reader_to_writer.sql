-- HU-REF-017: el backend abre las lecturas con SET LOCAL ROLE reference_reader.
-- El usuario de aplicación recibe reference_writer desde docker-infra; la
-- jerarquía permite adoptar explícitamente el rol de lectura sin grants
-- manuales ni cambios a la fixture institucional.
GRANT reference_reader TO reference_writer;
