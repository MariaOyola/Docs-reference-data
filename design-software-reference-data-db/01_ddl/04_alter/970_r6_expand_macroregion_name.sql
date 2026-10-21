-- Expansión: agregar una estructura nueva sin retirar la existente.
-- Añadimos name_v2 como columna TEXT, permitiendo temporalmente NULL.
-- Conservamos name y sus datos; el backend sigue utilizando name.
-- La copia y sincronización de los valores se preparan después.


-- Expansión (expand): fase aditiva de una migración de esquema.
-- Introduce estructuras nuevas conservando las existentes
-- y la compatibilidad con la versión actual de la aplicación.

ALTER TABLE institutional_structure.macroregion
ADD COLUMN name_v2 TEXT;
