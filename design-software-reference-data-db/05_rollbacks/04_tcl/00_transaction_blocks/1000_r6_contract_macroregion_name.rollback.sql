-- Limit the time spent waiting to acquire locks.
SET LOCAL lock_timeout = '3s';

LOCK TABLE institutional_structure.macroregion
IN ACCESS EXCLUSIVE MODE;

-- Abort if any name exceeds the original column capacity.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM institutional_structure.macroregion
        WHERE char_length(name) > 100
    ) THEN
        RAISE EXCEPTION
            'Rollback aborted: macroregion names exceed 100 characters';
    END IF;
END;
$$;

-- Restore the temporary TEXT column.
ALTER TABLE institutional_structure.macroregion
RENAME COLUMN name TO name_v2;

-- Recreate the original column and preserve the current names.
ALTER TABLE institutional_structure.macroregion
ADD COLUMN name VARCHAR(100);

UPDATE institutional_structure.macroregion
SET name = name_v2;

ALTER TABLE institutional_structure.macroregion
ALTER COLUMN name SET NOT NULL;

-- Restore the expanded schema nullability.
ALTER TABLE institutional_structure.macroregion
ALTER COLUMN name_v2 DROP NOT NULL;

-- Restore the synchronization function and trigger.
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
