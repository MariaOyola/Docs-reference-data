-- Backfill: completar la estructura nueva con los datos existentes.
-- Copia el nombre actual únicamente cuando ambas columnas difieren.
-- También incluye registros donde name_v2 es NULL.


UPDATE institutional_structure.macroregion -- Modifica filas existentes; no crea registros.
SET name_v2 = name -- Copia el valor actual de la columna origen a la columna destino.
WHERE name_v2 IS DISTINCT FROM name;
--Compara los valores incluyendo NULL, evitando actualizar filas que ya coinciden.