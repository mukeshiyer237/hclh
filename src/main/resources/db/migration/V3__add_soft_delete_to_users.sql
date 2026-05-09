-- V3: Add soft delete support to users table.
-- deleted_at is NULL for active users, set to the deletion timestamp for deleted users.
-- We never hard-delete users — audit_log references users(id) and must remain intact.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL DEFAULT NULL;

-- Index for the WHERE deleted_at IS NULL filter that every list query will use.
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);