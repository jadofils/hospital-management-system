-- =====================================================================
-- Hospital Management System — Additional Indexes (PostgreSQL)
-- Run AFTER hospital_schema.sql and hospital_objects.sql.
--
-- hospital_schema.sql already creates single-column indexes on the most
-- common filter/sort columns.  This file adds what isn't already covered:
--
--   1. Composite indexes  — multi-column WHERE / ORDER BY patterns
--   2. GIN trigram indexes — fast ILIKE '%text%' for name search
--   3. Partial active-record indexes — soft-delete pattern for tables
--                                      not already covered by the schema
--   4. DCL: pharmacist_role — left as a comment template in
--                             hospital_objects.sql; activated here.
--
-- Safe to re-run: uses CREATE INDEX IF NOT EXISTS and DO $$ blocks.
-- =====================================================================

-- =====================================================================
-- 0. PREREQUISITES
-- pgcrypto  — gen_random_uuid() used by user_sessions (UUID PK)
-- pg_trgm   — GIN trigram operator class for ILIKE '%text%' search
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =====================================================================
-- 1. COMPOSITE INDEXES
-- Speed up queries that filter or sort on two columns together, which a
-- single-column index handles less efficiently.
-- =====================================================================

-- Doctor's appointments on a specific date/time (booking-conflict check,
-- sp_book_appointment, trg_prevent_double_booking)
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date
    ON appointments(doctor_id, appointment_date)
    WHERE deleted_at IS NULL;

-- A patient's appointment history sorted by date (patient timeline view)
CREATE INDEX IF NOT EXISTS idx_appointments_patient_date
    ON appointments(patient_id, appointment_date)
    WHERE deleted_at IS NULL;

-- Earliest-expiring batch per medication (sp_issue_prescription FIFO pick)
CREATE INDEX IF NOT EXISTS idx_inventory_medication_expiry
    ON medical_inventory(medication_id, expiry_date)
    WHERE deleted_at IS NULL;

-- Outgoing referrals per referring doctor, filtered by status
CREATE INDEX IF NOT EXISTS idx_referrals_referring
    ON referrals(referring_doctor_id, status)
    WHERE deleted_at IS NULL;

-- Incoming referrals per receiving doctor, filtered by status
CREATE INDEX IF NOT EXISTS idx_referrals_referred_to
    ON referrals(referred_to_doctor_id, status)
    WHERE deleted_at IS NULL;

-- Audit history for one specific record in any table
-- (e.g. all audit entries for appointment_id=42)
CREATE INDEX IF NOT EXISTS idx_audit_log_record
    ON audit_log(table_affected, record_id);

-- RBAC chain lookup: all permissions reachable from a user_id
CREATE INDEX IF NOT EXISTS idx_user_roles_user
    ON user_roles(user_id)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_role_permissions_role
    ON role_permissions(role_id)
    WHERE deleted_at IS NULL;

-- =====================================================================
-- 2. GIN TRIGRAM INDEXES
-- Enable fast ILIKE '%text%' without a full table scan.
-- The pg_trgm extension (loaded above) supplies gin_trgm_ops.
-- Use: WHERE (first_name || ' ' || last_name) ILIKE '%chen%'
-- =====================================================================

-- Patient full-name search (used by the JavaFX patient search bar)
CREATE INDEX IF NOT EXISTS trgm_patients_name
    ON patients USING GIN ((first_name || ' ' || last_name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

-- Doctor full-name search (appointment booking, referral pick)
CREATE INDEX IF NOT EXISTS trgm_doctors_name
    ON doctors USING GIN ((first_name || ' ' || last_name) gin_trgm_ops)
    WHERE deleted_at IS NULL;

-- Medication name search (prescription / inventory lookup)
CREATE INDEX IF NOT EXISTS trgm_medications_name
    ON medications USING GIN (name gin_trgm_ops)
    WHERE deleted_at IS NULL;

-- =====================================================================
-- 3. PARTIAL ACTIVE-RECORD INDEXES  (soft-delete pattern)
-- hospital_schema.sql already has these for doctors, patients, and users.
-- The indexes below cover the remaining frequently-queried tables.
-- =====================================================================

CREATE INDEX IF NOT EXISTS idx_appointments_active
    ON appointments(appointment_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_medical_records_active
    ON medical_records(record_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_prescriptions_active
    ON prescriptions(prescription_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_lab_orders_active
    ON lab_orders(lab_order_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_invoices_active
    ON invoices(invoice_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_medications_active
    ON medications(medication_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_inventory_active
    ON medical_inventory(inventory_id)
    WHERE deleted_at IS NULL;

-- =====================================================================
-- 4. DCL — pharmacist_role
-- hospital_objects.sql left this as a commented template. Activated
-- here to mirror the Pharmacist application-level role as defense-in-depth.
-- The DO block avoids the "role already exists" error on re-runs.
-- =====================================================================

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'pharmacist_role') THEN
    CREATE ROLE pharmacist_role;
  END IF;
END
$$;

GRANT SELECT, INSERT, UPDATE ON medical_inventory  TO pharmacist_role;
GRANT SELECT, UPDATE          ON prescription_items TO pharmacist_role;
GRANT SELECT                  ON medications        TO pharmacist_role;
GRANT SELECT                  ON prescriptions      TO pharmacist_role;

-- =====================================================================
-- REFERENCE — indexes already in hospital_schema.sql
-- Listed for completeness; do NOT re-create (no IF NOT EXISTS there).
-- =====================================================================
-- idx_doctors_specialization              doctors(specialization)
-- idx_doctors_active                      doctors(doctor_id) WHERE deleted_at IS NULL
-- idx_patients_name                       patients(last_name, first_name)
-- idx_patients_active                     patients(patient_id) WHERE deleted_at IS NULL
-- idx_appointments_date                   appointments(appointment_date)
-- idx_appointments_doctor                 appointments(doctor_id)
-- idx_appointments_patient                appointments(patient_id)
-- idx_referrals_appointment               referrals(appointment_id)
-- idx_allergies_patient                   patient_allergies(patient_id)
-- idx_vitals_appointment                  vital_signs(appointment_id)
-- idx_medications_name                    medications(name)
-- idx_inventory_expiry                    medical_inventory(expiry_date)
-- idx_inventory_medication                medical_inventory(medication_id)
-- idx_inventory_low_stock                 medical_inventory(quantity_in_stock) WHERE qty <= reorder_level
-- idx_prescriptions_appointment           prescriptions(appointment_id)
-- idx_prescription_items_prescription     prescription_items(prescription_id)
-- idx_prescription_items_medication       prescription_items(medication_id)
-- idx_lab_orders_appointment              lab_orders(appointment_id)
-- idx_schedules_doctor                    doctor_schedules(doctor_id)
-- idx_feedback_patient                    patient_feedback(patient_id)
-- idx_invoices_patient                    invoices(patient_id)
-- idx_invoices_status                     invoices(payment_status)
-- idx_users_active                        users(user_id) WHERE deleted_at IS NULL
-- idx_audit_log_user                      audit_log(user_id)
-- idx_audit_log_created                   audit_log(created_at)
-- idx_sessions_user                       user_sessions(user_id)
-- idx_sessions_active                     user_sessions(user_id) WHERE is_active = TRUE
-- idx_system_logs_created                 system_logs(created_at)
-- idx_system_logs_level                   system_logs(log_level)
-- =====================================================================