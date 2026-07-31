-- =====================================================================
-- Hospital Management System — Additional Indexes (MySQL)
-- Run AFTER hospital_schema_mysql.sql and hospital_objects_mysql.sql.
--
-- IMPORTANT: hospital_schema_mysql.sql already creates single-column
-- indexes for most search/filter columns (patient name, appointment
-- date, doctor specialization, medication name, etc.) — see the list
-- at the bottom of this file. Also, InnoDB automatically creates an
-- index on any FOREIGN KEY column that doesn't already have one, so
-- every *_id FK (patient_id, doctor_id, appointment_id, ...) already
-- has basic lookup support even without an explicit CREATE INDEX.
--
-- This file adds what ISN'T already covered: composite indexes for
-- common multi-column WHERE/ORDER BY patterns, and full-text indexes
-- for fast name/drug search. Running CREATE INDEX on a name that
-- already exists errors with "Duplicate key name" — these are all new.
-- =====================================================================

USE hospital_db;

-- ---------------------------------------------------------------------
-- COMPOSITE INDEXES — speed up queries that filter/sort on two columns
-- together, which a single-column index can't do as efficiently.
-- ---------------------------------------------------------------------

-- Doctor's schedule for a specific day / booking-conflict check
-- (used by sp_book_appointment and trg_prevent_double_booking_*)
CREATE INDEX idx_appointments_doctor_date ON appointments(doctor_id, appointment_date);

-- A patient's appointment history, most relevant sorted by date
CREATE INDEX idx_appointments_patient_date ON appointments(patient_id, appointment_date);

-- Finding the earliest-expiring batch of a specific medication
-- (used by sp_issue_prescription: WHERE medication_id = ? ORDER BY expiry_date)
CREATE INDEX idx_inventory_medication_expiry ON medical_inventory(medication_id, expiry_date);

-- Checking a user's currently active sessions
CREATE INDEX idx_sessions_user_active ON user_sessions(user_id, is_active);

-- Pulling a doctor's incoming vs. outgoing referrals separately
CREATE INDEX idx_referrals_referring ON referrals(referring_doctor_id, status);
CREATE INDEX idx_referrals_referred_to ON referrals(referred_to_doctor_id, status);

-- Looking up all audit entries for one specific record (e.g. one appointment's history)
CREATE INDEX idx_audit_log_record ON audit_log(table_affected, record_id);

-- ---------------------------------------------------------------------
-- FULL-TEXT INDEXES — for fast, case-insensitive substring/partial-word
-- search (Epic 3.1: "search for patients quickly by name"). A LIKE
-- '%text%' query can't use a normal index at all; FULLTEXT can.
-- ---------------------------------------------------------------------

ALTER TABLE patients   ADD FULLTEXT INDEX ftx_patients_name (first_name, last_name);
ALTER TABLE medications ADD FULLTEXT INDEX ftx_medications_name (name, generic_name);

-- =====================================================================
-- REFERENCE — indexes already created inline in hospital_schema_mysql.sql
-- (listed here for completeness; do NOT re-run these, they already exist)
-- =====================================================================
-- idx_doctors_specialization         doctors(specialization)
-- idx_patients_name                  patients(last_name, first_name)
-- idx_appointments_date              appointments(appointment_date)
-- idx_appointments_doctor            appointments(doctor_id)
-- idx_appointments_patient           appointments(patient_id)
-- idx_referrals_appointment          referrals(appointment_id)
-- idx_allergies_patient               patient_allergies(patient_id)
-- idx_vitals_appointment             vital_signs(appointment_id)
-- idx_medications_name               medications(name)
-- idx_inventory_expiry               medical_inventory(expiry_date)
-- idx_inventory_medication           medical_inventory(medication_id)
-- idx_prescriptions_appointment      prescriptions(appointment_id)
-- idx_prescription_items_prescription prescription_items(prescription_id)
-- idx_prescription_items_medication  prescription_items(medication_id)
-- idx_lab_orders_appointment         lab_orders(appointment_id)
-- idx_schedules_doctor               doctor_schedules(doctor_id)
-- idx_feedback_patient               patient_feedback(patient_id)
-- idx_invoices_patient               invoices(patient_id)
-- idx_invoices_status                invoices(payment_status)
-- idx_audit_log_user                 audit_log(user_id)
-- idx_audit_log_created              audit_log(created_at)
-- idx_sessions_user                  user_sessions(user_id)
-- idx_system_logs_created            system_logs(created_at)
-- idx_system_logs_level              system_logs(log_level)
-- =====================================================================
