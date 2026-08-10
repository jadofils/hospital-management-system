-- =====================================================================
-- Hospital Management System — Migration v3
-- Adds read/unread tracking to notifications.
--
-- Run AFTER hospital_schema.sql + hospital_migration_v2.sql (or on any
-- database that already has the `notifications` table from either one).
-- Idempotent: safe to re-run — every statement guards its own existence check.
-- =====================================================================

-- 1. read_at column — NULL means unread. Set once when the recipient opens/
--    clicks the notification (via NotificationService.markAsRead/markAllAsRead).
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at TIMESTAMP NULL;

-- 2. Partial index — the unread-count query (`WHERE read_at IS NULL`) is the
--    hottest read path for this column, so index only the unread rows.
CREATE INDEX IF NOT EXISTS idx_notifications_unread
  ON notifications(read_at) WHERE read_at IS NULL AND deleted_at IS NULL;
