-- =====================================================================
-- Hospital Management System — Sample Data + Find/Fetch Queries (MySQL)
-- Run AFTER hospital_schema_mysql.sql, hospital_objects_mysql.sql, and
-- hospital_indexes_mysql.sql.
--
-- Part 1: a small set of INSERTs so there's data to actually query.
-- Part 2: example find/fetch queries, each annotated with which index
--         it uses — pair these with EXPLAIN for your performance report
--         (Epic 4.1: measure query time before/after indexing).
-- =====================================================================

USE hospital_db;

-- =====================================================================
-- PART 1 — SAMPLE DATA (INSERT)
-- =====================================================================

INSERT INTO departments (name, location, phone) VALUES
  ('Cardiology', 'Building A, 2nd Floor', '0788000001'),
  ('Pediatrics', 'Building B, 1st Floor', '0788000002');

INSERT INTO doctors (department_id, first_name, last_name, specialization, phone, email) VALUES
  (1, 'Alice', 'Uwase', 'Cardiologist', '0788111111', 'a.uwase@hospital.rw'),
  (2, 'Jean', 'Mugisha', 'Pediatrician', '0788222222', 'j.mugisha@hospital.rw');

INSERT INTO patients (first_name, last_name, dob, gender, phone, email, address) VALUES
  ('Eric', 'Ndayisenga', '1990-04-12', 'M', '0788333333', 'eric.n@example.com', 'Kigali'),
  ('Grace', 'Umutoni', '1985-09-30', 'F', '0788444444', 'grace.u@example.com', 'Kigali');

INSERT INTO medications (name, generic_name, form, unit_price) VALUES
  ('Amoxicillin 500mg', 'Amoxicillin', 'capsule', 300.00),
  ('Paracetamol 500mg', 'Paracetamol', 'tablet', 100.00);

INSERT INTO medical_inventory (medication_id, batch_number, expiry_date, quantity_in_stock, reorder_level, supplier) VALUES
  (1, 'BATCH-A1', '2027-06-01', 50, 10, 'PharmaCorp'),
  (2, 'BATCH-P1', '2026-12-01', 5, 10, 'PharmaCorp');  -- deliberately low stock, for vw_low_stock_medications

INSERT INTO appointments (patient_id, doctor_id, appointment_date, status, reason) VALUES
  (1, 1, NOW() + INTERVAL 1 DAY, 'scheduled', 'Chest pain follow-up'),
  (2, 2, NOW() + INTERVAL 2 DAY, 'scheduled', 'Routine checkup');

INSERT INTO roles (role_name) VALUES ('Admin'), ('Doctor'), ('Receptionist'), ('Analyst');

INSERT INTO users (doctor_id, username, password_hash, email, is_active) VALUES
  (1, 'a.uwase', 'REPLACE_WITH_REAL_HASH', 'a.uwase@hospital.rw', TRUE),
  (NULL, 'reception1', 'REPLACE_WITH_REAL_HASH', 'reception1@hospital.rw', TRUE);


-- =====================================================================
-- PART 2 — FIND / FETCH QUERIES
-- Each is written to actually use one of the indexes from
-- hospital_schema_mysql.sql or hospital_indexes_mysql.sql.
-- Prefix any of these with EXPLAIN to see which index MySQL picks.
-- =====================================================================

-- Find a patient by (partial) name — uses ftx_patients_name (FULLTEXT)
-- Fast even on large tables; a plain LIKE '%text%' can't use an index at all.
SELECT patient_id, first_name, last_name, phone
FROM patients
WHERE MATCH(first_name, last_name) AGAINST ('Eric' IN NATURAL LANGUAGE MODE)
  AND deleted_at IS NULL;

-- Exact/prefix name search — uses idx_patients_name (last_name, first_name)
SELECT patient_id, first_name, last_name
FROM patients
WHERE last_name = 'Ndayisenga'
  AND deleted_at IS NULL;

-- A specific doctor's appointments on a given day — uses idx_appointments_doctor_date
SELECT appointment_id, appointment_date, status
FROM appointments
WHERE doctor_id = 1
  AND appointment_date BETWEEN CURDATE() AND CURDATE() + INTERVAL 1 DAY
  AND deleted_at IS NULL
ORDER BY appointment_date;

-- One patient's full appointment history, most recent first —
-- uses idx_appointments_patient_date
SELECT appointment_id, appointment_date, status
FROM appointments
WHERE patient_id = 1
  AND deleted_at IS NULL
ORDER BY appointment_date DESC;

-- Find the earliest-expiring batch of a specific medication —
-- uses idx_inventory_medication_expiry (this is what sp_issue_prescription runs internally)
SELECT inventory_id, batch_number, expiry_date, quantity_in_stock
FROM medical_inventory
WHERE medication_id = 1
  AND deleted_at IS NULL
ORDER BY expiry_date ASC
LIMIT 1;

-- Search medications by (partial) name — uses ftx_medications_name (FULLTEXT)
SELECT medication_id, name, generic_name
FROM medications
WHERE MATCH(name, generic_name) AGAINST ('paracetamol' IN NATURAL LANGUAGE MODE)
  AND deleted_at IS NULL;

-- Low-stock medications — uses idx_inventory_expiry / the vw_low_stock_medications view
SELECT * FROM vw_low_stock_medications;

-- Today's full schedule per doctor — uses the vw_doctor_daily_schedule view
SELECT * FROM vw_doctor_daily_schedule;

-- A doctor's referrals they've sent out, filtered by status —
-- uses idx_referrals_referring
SELECT referral_id, appointment_id, reason, status
FROM referrals
WHERE referring_doctor_id = 1
  AND status = 'pending'
  AND deleted_at IS NULL;

-- Full audit trail for one specific appointment — uses idx_audit_log_record
SELECT log_id, action, created_at
FROM audit_log
WHERE table_affected = 'appointments'
  AND record_id = 1
ORDER BY created_at DESC;

-- Currently active sessions for one user — uses idx_sessions_user_active
SELECT session_id, login_at, expires_at
FROM user_sessions
WHERE user_id = 1
  AND is_active = TRUE;


-- =====================================================================
-- PART 3 — EXAMPLE: measuring the index's effect for your report
-- Run each pair (index dropped vs. index present) and compare the
-- "rows examined" and timing MySQL reports.
-- =====================================================================

-- Before/after example — comment out the CREATE INDEX line temporarily
-- to see the difference in EXPLAIN output for this exact query:

-- EXPLAIN SELECT appointment_id, appointment_date, status
-- FROM appointments
-- WHERE doctor_id = 1
--   AND appointment_date BETWEEN CURDATE() AND CURDATE() + INTERVAL 1 DAY
--   AND deleted_at IS NULL;

-- Look at the `key` column in the output: with idx_appointments_doctor_date
-- in place, it should show that index being used instead of a full table scan.
