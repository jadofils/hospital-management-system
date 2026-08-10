CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE "departments" (
  "department_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "name" varchar UNIQUE NOT NULL,
  "location" varchar,
  "phone" varchar,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "doctors" (
  "doctor_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "department_id" UUID,
  "first_name" varchar NOT NULL,
  "last_name" varchar NOT NULL,
  "specialization" varchar,
  "phone" varchar,
  "email" varchar UNIQUE,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "patients" (
  "patient_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "first_name" varchar NOT NULL,
  "last_name" varchar NOT NULL,
  "dob" date NOT NULL,
  "gender" varchar,
  "phone" varchar,
  "email" varchar,
  "address" varchar,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "appointments" (
  "appointment_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "patient_id" UUID NOT NULL,
  "doctor_id" UUID NOT NULL,
  "appointment_date" timestamp NOT NULL,
  "status" varchar NOT NULL,
  "reason" varchar,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "medical_records" (
  "record_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "appointment_id" UUID UNIQUE NOT NULL,
  "diagnosis" varchar,
  "symptoms" text,
  "notes" text,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "referrals" (
  "referral_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "appointment_id" UUID NOT NULL,
  "referring_doctor_id" UUID NOT NULL,
  "referred_to_doctor_id" UUID NOT NULL,
  "reason" varchar,
  "status" varchar NOT NULL,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "patient_allergies" (
  "allergy_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "patient_id" UUID NOT NULL,
  "allergen" varchar NOT NULL,
  "reaction" varchar,
  "severity" varchar,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "vital_signs" (
  "vital_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "appointment_id" UUID NOT NULL,
  "blood_pressure_systolic" integer,
  "blood_pressure_diastolic" integer,
  "heart_rate" integer,
  "temperature_celsius" decimal,
  "weight_kg" decimal,
  "height_cm" decimal,
  "recorded_at" timestamp NOT NULL,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "medications" (
  "medication_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "name" varchar NOT NULL,
  "generic_name" varchar,
  "form" varchar,
  "unit_price" decimal,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "medical_inventory" (
  "inventory_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "medication_id" UUID NOT NULL,
  "batch_number" varchar,
  "expiry_date" date NOT NULL,
  "quantity_in_stock" integer NOT NULL,
  "reorder_level" integer NOT NULL,
  "supplier" varchar,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "prescriptions" (
  "prescription_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "appointment_id" UUID NOT NULL,
  "date_issued" date NOT NULL,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "prescription_items" (
  "item_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "prescription_id" UUID NOT NULL,
  "medication_id" UUID NOT NULL,
  "dosage" varchar,
  "quantity" integer NOT NULL,
  "instructions" varchar,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "lab_orders" (
  "lab_order_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "appointment_id" UUID NOT NULL,
  "doctor_id" UUID NOT NULL,
  "test_name" varchar NOT NULL,
  "status" varchar NOT NULL,
  "ordered_at" timestamp NOT NULL,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "lab_results" (
  "lab_result_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "lab_order_id" UUID UNIQUE NOT NULL,
  "result_value" varchar,
  "unit" varchar,
  "reference_range" varchar,
  "is_abnormal" boolean NOT NULL,
  "completed_at" timestamp,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "doctor_schedules" (
  "schedule_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "doctor_id" UUID NOT NULL,
  "day_of_week" varchar NOT NULL,
  "start_time" time NOT NULL,
  "end_time" time NOT NULL,
  "is_available" boolean NOT NULL,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "patient_feedback" (
  "feedback_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "patient_id" UUID NOT NULL,
  "appointment_id" UUID,
  "rating" integer NOT NULL,
  "comments" text,
  "date_submitted" date NOT NULL,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "invoices" (
  "invoice_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "appointment_id" UUID NOT NULL,
  "patient_id" UUID NOT NULL,
  "total_amount" decimal NOT NULL,
  "payment_status" varchar NOT NULL,
  "issued_at" timestamp NOT NULL,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "users" (
  "user_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "doctor_id" UUID UNIQUE,
  "username" varchar UNIQUE NOT NULL,
  "password_hash" varchar NOT NULL,
  "email" varchar UNIQUE,
  "is_active" boolean NOT NULL,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "roles" (
  "role_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "role_name" varchar UNIQUE NOT NULL,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "permissions" (
  "permission_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "resource" varchar NOT NULL,
  "action" varchar NOT NULL,
  "created_at" timestamp,
  "updated_at" timestamp,
  "deleted_at" timestamp
);

CREATE TABLE "user_roles" (
  "user_id" UUID NOT NULL,
  "role_id" UUID NOT NULL,
  "assigned_at" timestamp NOT NULL,
  "updated_at" timestamp,
  "revoked_at" timestamp,
  PRIMARY KEY ("user_id", "role_id")
);

CREATE TABLE "role_permissions" (
  "role_id" UUID NOT NULL,
  "permission_id" UUID NOT NULL,
  "created_at" timestamp,
  "deleted_at" timestamp,
  PRIMARY KEY ("role_id", "permission_id")
);

CREATE TABLE "audit_log" (
  "log_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "user_id" UUID,
  "action" varchar NOT NULL,
  "table_affected" varchar NOT NULL,
  "record_id" UUID,
  "created_at" timestamp NOT NULL
);

CREATE TABLE "user_sessions" (
  "session_id" varchar PRIMARY KEY,
  "user_id" UUID NOT NULL,
  "login_at" timestamp NOT NULL,
  "logout_at" timestamp,
  "expires_at" timestamp NOT NULL,
  "ip_address" varchar,
  "user_agent" varchar,
  "is_active" boolean NOT NULL,
  "updated_at" timestamp
);

CREATE TABLE "system_logs" (
  "log_id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "log_level" varchar NOT NULL,
  "source" varchar,
  "message" text NOT NULL,
  "user_id" UUID,
  "created_at" timestamp NOT NULL
);

CREATE UNIQUE INDEX ON "permissions" ("resource", "action");

COMMENT ON COLUMN "appointments"."status" IS 'scheduled, completed, cancelled';

COMMENT ON COLUMN "referrals"."status" IS 'pending, scheduled, completed';

COMMENT ON COLUMN "patient_allergies"."severity" IS 'mild, moderate, severe';

COMMENT ON COLUMN "lab_orders"."status" IS 'ordered, in_progress, completed, cancelled';

COMMENT ON COLUMN "patient_feedback"."rating" IS '1-5';

COMMENT ON COLUMN "invoices"."payment_status" IS 'unpaid, partially_paid, paid';

COMMENT ON COLUMN "system_logs"."log_level" IS 'DEBUG, INFO, WARNING, ERROR';

ALTER TABLE "doctors" ADD FOREIGN KEY ("department_id") REFERENCES "departments" ("department_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "appointments" ADD FOREIGN KEY ("patient_id") REFERENCES "patients" ("patient_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "appointments" ADD FOREIGN KEY ("doctor_id") REFERENCES "doctors" ("doctor_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "medical_records" ADD FOREIGN KEY ("appointment_id") REFERENCES "appointments" ("appointment_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "referrals" ADD FOREIGN KEY ("appointment_id") REFERENCES "appointments" ("appointment_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "referrals" ADD FOREIGN KEY ("referring_doctor_id") REFERENCES "doctors" ("doctor_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "referrals" ADD FOREIGN KEY ("referred_to_doctor_id") REFERENCES "doctors" ("doctor_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "patient_allergies" ADD FOREIGN KEY ("patient_id") REFERENCES "patients" ("patient_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "vital_signs" ADD FOREIGN KEY ("appointment_id") REFERENCES "appointments" ("appointment_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "medical_inventory" ADD FOREIGN KEY ("medication_id") REFERENCES "medications" ("medication_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "prescriptions" ADD FOREIGN KEY ("appointment_id") REFERENCES "appointments" ("appointment_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "prescription_items" ADD FOREIGN KEY ("prescription_id") REFERENCES "prescriptions" ("prescription_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "prescription_items" ADD FOREIGN KEY ("medication_id") REFERENCES "medications" ("medication_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "lab_orders" ADD FOREIGN KEY ("appointment_id") REFERENCES "appointments" ("appointment_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "lab_orders" ADD FOREIGN KEY ("doctor_id") REFERENCES "doctors" ("doctor_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "lab_results" ADD FOREIGN KEY ("lab_order_id") REFERENCES "lab_orders" ("lab_order_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "doctor_schedules" ADD FOREIGN KEY ("doctor_id") REFERENCES "doctors" ("doctor_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "patient_feedback" ADD FOREIGN KEY ("patient_id") REFERENCES "patients" ("patient_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "patient_feedback" ADD FOREIGN KEY ("appointment_id") REFERENCES "appointments" ("appointment_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "invoices" ADD FOREIGN KEY ("appointment_id") REFERENCES "appointments" ("appointment_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "invoices" ADD FOREIGN KEY ("patient_id") REFERENCES "patients" ("patient_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "users" ADD FOREIGN KEY ("doctor_id") REFERENCES "doctors" ("doctor_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "user_roles" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("user_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "user_roles" ADD FOREIGN KEY ("role_id") REFERENCES "roles" ("role_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "role_permissions" ADD FOREIGN KEY ("role_id") REFERENCES "roles" ("role_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "role_permissions" ADD FOREIGN KEY ("permission_id") REFERENCES "permissions" ("permission_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "audit_log" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("user_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "user_sessions" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("user_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "system_logs" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("user_id") DEFERRABLE INITIALLY IMMEDIATE;

