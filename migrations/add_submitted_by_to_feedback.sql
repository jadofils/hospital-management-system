-- Add submitted_by column to patient_feedback table
-- This allows tracking who submitted the feedback (patient, doctor, or admin)
ALTER TABLE "patient_feedback" 
ADD COLUMN "submitted_by" UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

-- Add foreign key constraint
ALTER TABLE "patient_feedback" 
ADD FOREIGN KEY ("submitted_by") REFERENCES "users" ("user_id") DEFERRABLE INITIALLY IMMEDIATE;

-- Update existing records to set submitted_by based on patient_id
-- (You may need to adjust this based on your data)
UPDATE "patient_feedback" pf
SET "submitted_by" = u."user_id"
FROM "patients" p
JOIN "users" u ON p."email" = u."email"
WHERE pf."patient_id" = p."patient_id";

-- Make patient_id nullable since admin/doctor feedback might not be about a specific patient
ALTER TABLE "patient_feedback" 
ALTER COLUMN "patient_id" DROP NOT NULL;

COMMENT ON COLUMN "patient_feedback"."submitted_by" IS 'User who submitted the feedback (FK to users)';
COMMENT ON COLUMN "patient_feedback"."patient_id" IS 'Patient the feedback is about (nullable, FK to patients)';
