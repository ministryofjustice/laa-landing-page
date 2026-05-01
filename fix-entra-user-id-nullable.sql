-- Manually fix the entra_user_id column to be nullable
-- This is needed because the Liquibase changeset had incorrect precondition logic

-- Check current state
SELECT column_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'user_account_status_audit'
  AND column_name IN ('entra_user_id', 'user_email');

-- Drop the NOT NULL constraint if it exists
ALTER TABLE user_account_status_audit
ALTER COLUMN entra_user_id DROP NOT NULL;

-- Verify the change
SELECT column_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'user_account_status_audit'
  AND column_name IN ('entra_user_id', 'user_email');

-- Mark the Liquibase changeset as executed so it doesn't run again
UPDATE databasechangelog
SET exectype = 'EXECUTED',
    md5sum = (SELECT md5sum FROM databasechangelog WHERE id = '1745683200000-2' LIMIT 1)
WHERE id = '1745683200000-2';

