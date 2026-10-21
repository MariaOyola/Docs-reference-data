-- Rollback sin modificación de datos.
-- El backfill conserva name y solo copia su valor a name_v2.
-- name_v2 se eliminará al revertir la expansión.
SELECT 1;