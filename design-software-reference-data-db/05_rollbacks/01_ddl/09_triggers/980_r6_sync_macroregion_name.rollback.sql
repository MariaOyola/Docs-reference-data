-- Primero retiramos el trigger que utiliza la función.
DROP TRIGGER trg_macroregion_sync_name_v2
ON institutional_structure.macroregion;

-- Después retiramos la función.
DROP FUNCTION institutional_structure.sync_macroregion_name_v2();