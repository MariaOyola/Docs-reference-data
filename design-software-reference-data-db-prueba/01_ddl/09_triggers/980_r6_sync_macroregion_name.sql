CREATE FUNCTION institutional_structure.sync_macroregion_name_v2()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.name_v2 := NEW.name;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_macroregion_sync_name_v2
BEFORE INSERT OR UPDATE
ON institutional_structure.macroregion
FOR EACH ROW
EXECUTE FUNCTION institutional_structure.sync_macroregion_name_v2();