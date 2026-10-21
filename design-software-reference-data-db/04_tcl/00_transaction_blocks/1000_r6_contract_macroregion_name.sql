-- Limit the time spent waiting to acquire locks.
SET LOCAL lock_timeout = '3s';

-- Prevent changes between validation and column replacement.
-- This lock also blocks reads while it is held.
LOCK TABLE institutional_structure.macroregion
IN ACCESS EXCLUSIVE MODE;

-- Abort if the new column is incomplete or out of sync.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM institutional_structure.macroregion
        WHERE name_v2 IS NULL
           OR name_v2 IS DISTINCT FROM name
    ) THEN
        RAISE EXCEPTION
            'Contract aborted: macroregion names are not synchronized';
    END IF;
END;
$$;

-- Remove the temporary synchronization mechanism.
DROP TRIGGER trg_macroregion_sync_name_v2
ON institutional_structure.macroregion;

DROP FUNCTION institutional_structure.sync_macroregion_name_v2();

-- Preserve the required name constraint.
ALTER TABLE institutional_structure.macroregion
ALTER COLUMN name_v2 SET NOT NULL;

-- Remove the original column without cascading dependencies.
ALTER TABLE institutional_structure.macroregion
DROP COLUMN name;

-- Preserve the column name expected by the backend.
ALTER TABLE institutional_structure.macroregion
RENAME COLUMN name_v2 TO name;
