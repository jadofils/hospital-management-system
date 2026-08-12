-- Add an administrative status column to patients (active/inactive).
-- This is distinct from deleted_at: soft-deleted patients disappear from every
-- query, while an "inactive" patient stays visible/searchable but flagged.
ALTER TABLE "patients"
ADD COLUMN "status" VARCHAR(20) NOT NULL DEFAULT 'active';

ALTER TABLE "patients"
ADD CONSTRAINT "patients_status_check" CHECK ("status" IN ('active', 'inactive'));

COMMENT ON COLUMN "patients"."status" IS 'Administrative status of the patient record (active/inactive) — separate from deleted_at soft-delete.';
