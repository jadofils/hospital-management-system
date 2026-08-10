-- =============================================================================
-- Migration v2: Add submitted_by to patient_feedback, patient_notes, notifications
-- Run once against an existing database that was created from hospital_schema v1.
-- All statements are idempotent (IF NOT EXISTS / DO $$ guards).
-- =============================================================================

-- 1. Add submitted_by column to patient_feedback, make patient_id nullable
ALTER TABLE patient_feedback
  ADD COLUMN IF NOT EXISTS submitted_by UUID;

ALTER TABLE patient_feedback
  ALTER COLUMN patient_id DROP NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_feedback_submitted_by'
  ) THEN
    ALTER TABLE patient_feedback
      ADD CONSTRAINT fk_feedback_submitted_by
      FOREIGN KEY (submitted_by) REFERENCES users(user_id) ON DELETE SET NULL;
  END IF;
END$$;

CREATE INDEX IF NOT EXISTS idx_feedback_submitted_by ON patient_feedback(submitted_by);

-- 2. patient_notes table
CREATE TABLE IF NOT EXISTS patient_notes (
  note_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  patient_id       UUID NOT NULL REFERENCES patients(patient_id) ON DELETE RESTRICT,
  appointment_id   UUID REFERENCES appointments(appointment_id) ON DELETE SET NULL,
  author_user_id   UUID REFERENCES users(user_id) ON DELETE SET NULL,
  author_role      VARCHAR(50),
  note_text        TEXT NOT NULL,
  source           VARCHAR(50) NOT NULL DEFAULT 'medical_records',
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at       TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_notes_patient     ON patient_notes(patient_id)     WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notes_appointment ON patient_notes(appointment_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notes_author      ON patient_notes(author_user_id) WHERE deleted_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'trg_patient_notes_updated_at'
  ) THEN
    CREATE TRIGGER trg_patient_notes_updated_at
      BEFORE UPDATE ON patient_notes
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;

-- 3. notifications table
CREATE TABLE IF NOT EXISTS notifications (
  notification_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type             VARCHAR(100) NOT NULL,
  actor_user_id    UUID REFERENCES users(user_id) ON DELETE SET NULL,
  recipients       JSONB NOT NULL DEFAULT '[]',
  payload          JSONB,
  channels         JSONB,
  status           JSONB,
  priority         VARCHAR(20) NOT NULL DEFAULT 'normal'
                    CHECK (priority IN ('low','normal','high')),
  read_at          TIMESTAMP NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at       TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_actor     ON notifications(actor_user_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_created   ON notifications(created_at)    WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_recipients ON notifications USING GIN (recipients);
CREATE INDEX IF NOT EXISTS idx_notifications_unread    ON notifications(read_at) WHERE read_at IS NULL AND deleted_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'trg_notifications_updated_at'
  ) THEN
    CREATE TRIGGER trg_notifications_updated_at
      BEFORE UPDATE ON notifications
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END$$;

-- 4. New RBAC permissions for patient_notes and notifications
INSERT INTO permissions (resource, action)
VALUES
  ('patient_notes',  'create'),
  ('patient_notes',  'read'),
  ('patient_notes',  'update'),
  ('patient_notes',  'delete'),
  ('notifications',  'create'),
  ('notifications',  'read'),
  ('notifications',  'update'),
  ('notifications',  'delete')
ON CONFLICT (resource, action) DO NOTHING;