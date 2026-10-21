-- Sincroniza name_v2 con name al crear o modificar una macrorregión.
-- No copia por sí solo los registros antiguos: eso corresponde al backfill.

-- Crea la función que contiene la acción de sincronización.
CREATE FUNCTION institutional_structure.sync_macroregion_name_v2()

-- Indica que esta función se utilizará desde un trigger.
RETURNS trigger

-- Define el lenguaje procedural de PostgreSQL.
LANGUAGE plpgsql

-- Delimita el cuerpo de la función.
AS $$
BEGIN
    -- NEW contiene la fila que se va a guardar.
    -- := asigna el valor de name a name_v2.
    NEW.name_v2 := NEW.name;

    -- Devuelve la fila modificada para continuar el guardado.
    RETURN NEW;
END;
$$;

-- Crea el disparador que ejecutará la función.
CREATE TRIGGER trg_macroregion_sync_name_v2

-- Actúa antes de insertar o actualizar.
BEFORE INSERT OR UPDATE

-- Define la tabla donde se activa.
ON institutional_structure.macroregion

-- Ejecuta la función por cada fila afectada.
FOR EACH ROW

-- Indica qué función debe ejecutar.
EXECUTE FUNCTION institutional_structure.sync_macroregion_name_v2();