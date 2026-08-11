-- =====================================================================
-- Hospital Management System — Additional Seed Data
-- Idempotent seed to ensure each application table has at least 10 rows
-- Run AFTER `hospital_schema.sql` and `hospital_rbac_seed_postgresql.sql`.
-- Does NOT create new roles or permissions; users are mapped to existing roles.
-- Safe to re-run: uses ON CONFLICT DO NOTHING where practical.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Departments (ensure at least 10)
-- ---------------------------------------------------------------------
INSERT INTO departments (name, location, phone) VALUES
  ('Cardiology',  'Block A, 1st Floor',    '+250 788 001 001'),
  ('Pediatrics',  'Block B, Ground Floor', '+250 788 001 002'),
  ('Emergency',   'Block C, Entrance',     '+250 788 001 003'),
  ('Orthopedics', 'Block A, 2nd Floor',    '+250 788 001 004'),
  ('Radiology',   'Block D, Basement',     '+250 788 001 005'),
  ('Maternity',   'Block E, 3rd Floor',    '+250 788 001 006'),
  ('Neurology',   'Block F, 2nd Floor',    '+250 788 001 007'),
  ('ENT',         'Block G, 1st Floor',    '+250 788 001 008'),
  ('Dermatology', 'Block H, 1st Floor',    '+250 788 001 009'),
  ('General Medicine', 'Block I, Ground Floor', '+250 788 001 010')
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------------------------------
-- Doctors (ensure at least 10) — map to departments by name
-- ---------------------------------------------------------------------
INSERT INTO doctors (department_id, first_name, last_name, specialization, phone, email) 
SELECT d.department_id, v.first_name, v.last_name, v.specialization, v.phone, v.email
FROM (VALUES
  ('Cardiology','Jean',    'Ishimwe',    'Cardiologist',         '+250 788 100 011', 'jean.ishimwe@hms.com'),
  ('Pediatrics','Alice',   'Mukamana',   'Pediatrician',         '+250 788 100 012', 'alice.mukamana@hms.com'),
  ('Emergency','Emmanuel', 'Nkurunziza', 'Emergency Medicine',   '+250 788 100 013', 'emmanuel.nkurunziza@hms.com'),
  ('Orthopedics','Beatrice','Uwimana',   'Orthopedic Surgeon',   '+250 788 100 014', 'beatrice.uwimana@hms.com'),
  ('Radiology','Fabrice',  'Manirakiza', 'Radiologist',          '+250 788 100 015', 'fabrice.manirakiza@hms.com'),
  ('Maternity','Claire',    'Habimana',   'Obstetrician',         '+250 788 100 016', 'claire.habimana@hms.com'),
  ('Neurology','Daniel',    'Niyonzima',  'Neurologist',          '+250 788 100 017', 'daniel.niyonzima@hms.com'),
  ('ENT','Esther',         'Mukantwari','ENT Specialist',        '+250 788 100 018', 'esther.mukantwari@hms.com'),
  ('Dermatology','Kevin',   'Kagabo',     'Dermatologist',        '+250 788 100 019', 'kevin.kagabo@hms.com'),
  ('General Medicine','Solange','Byiringiro','General Practitioner', '+250 788 100 020', 'solange.byiringiro@hms.com')
) AS v(dept_name, first_name, last_name, specialization, phone, email)
JOIN departments d ON d.name = v.dept_name
ON CONFLICT (email) DO NOTHING;

-- ---------------------------------------------------------------------
-- Medications (drug catalog) — ensure at least 10
-- ---------------------------------------------------------------------
-- medications: insert only when a medication with same name does not exist
INSERT INTO medications (name, generic_name, form, unit_price)
SELECT v.name, v.generic_name, v.form, v.unit_price
FROM (VALUES
  ('Paracetamol 500mg', 'Paracetamol', 'Tablet', 0.05),
  ('Amoxicillin 500mg', 'Amoxicillin', 'Capsule', 0.12),
  ('Ceftriaxone 1g', 'Ceftriaxone', 'Injection', 3.50),
  ('Ibuprofen 200mg', 'Ibuprofen', 'Tablet', 0.08),
  ('Omeprazole 20mg', 'Omeprazole', 'Capsule', 0.30),
  ('Metformin 500mg', 'Metformin', 'Tablet', 0.10),
  ('Salbutamol Inhaler', 'Salbutamol', 'Inhaler', 5.00),
  ('Aspirin 81mg', 'Aspirin', 'Tablet', 0.04),
  ('Ranitidine 150mg', 'Ranitidine', 'Tablet', 0.15),
  ('Atorvastatin 20mg', 'Atorvastatin', 'Tablet', 0.25)
) AS v(name, generic_name, form, unit_price)
WHERE NOT EXISTS (SELECT 1 FROM medications m WHERE m.name = v.name);

-- ---------------------------------------------------------------------
-- Medical inventory — create at least 10 batches (links to medications)
-- ---------------------------------------------------------------------
INSERT INTO medical_inventory (medication_id, batch_number, expiry_date, quantity_in_stock, reorder_level, supplier)
SELECT m.medication_id, 'BATCH-' || substring(md5(m.name || now()::text),1,8), CURRENT_DATE + (365 * 2) * INTERVAL '1 day', 100, 10, 'Rwanda Medical Suppliers'
FROM medications m
LIMIT 10;

-- ---------------------------------------------------------------------
-- Patients — at least 12 (Rwandan-like names)
-- ---------------------------------------------------------------------
-- patients: insert only if email is not already present (email not declared UNIQUE in schema)
INSERT INTO patients (first_name, last_name, dob, gender, phone, email, address)
SELECT v.first_name, v.last_name, v.dob::date, v.gender, v.phone, v.email, v.address
FROM (VALUES
  ('Ishimwe',   'Eric',      '1985-06-12', 'M', '+250 788 200 001', 'eric.ishimwe@example.rw', 'Kigali, Gasabo'),
  ('Mukamana',  'Alice',     '1990-02-03', 'F', '+250 788 200 002', 'alice.mukamana@example.rw', 'Kigali, Nyarugenge'),
  ('Niyonzima', 'Jean',      '1978-11-20', 'M', '+250 788 200 003', 'jean.niyonzima@example.rw', 'Kigali, Kicukiro'),
  ('Habimana',  'Claire',    '1995-09-07', 'F', '+250 788 200 004', 'claire.habimana@example.rw', 'Huye Town'),
  ('Kamanzi',   'Olivier',   '1982-03-15', 'M', '+250 788 200 005', 'olivier.kamanzi@example.rw', 'Musanze'),
  ('Byiringiro','Solange',   '2000-12-01', 'F', '+250 788 200 006', 'solange.byiringiro@example.rw', 'Rubavu'),
  ('Maniraguha','Fabrice',   '1992-05-18', 'M', '+250 788 200 007', 'fabrice.maniraguha@example.rw', 'Gisenyi'),
  ('Uwizeyimana','Esther',   '1988-08-30', 'F', '+250 788 200 008', 'esther.uwizeyimana@example.rw', 'Ruhengeri'),
  ('Ntawukuriryayo','Paul',  '1975-04-10', 'M', '+250 788 200 009', 'paul.ntawuku@example.rw', 'Kigali, Gasabo'),
  ('Mukeshimana','Jeanne',   '1999-07-22', 'F', '+250 788 200 010', 'jeanne.mukeshimana@example.rw', 'Kigali, Nyarugenge'),
  ('Uwimana',   'Pascal',    '1987-01-05', 'M', '+250 788 200 011', 'pascal.uwimana@example.rw', 'Nyagatare'),
  ('Nshuti',    'Aline',     '1993-10-14', 'F', '+250 788 200 012', 'aline.nshuti@example.rw', 'Kigali, Kicukiro')
) AS v(first_name, last_name, dob, gender, phone, email, address)
WHERE NOT EXISTS (SELECT 1 FROM patients p WHERE p.email = v.email);

-- ---------------------------------------------------------------------
-- Appointments — create at least 12 (link patients <> doctors)
-- ---------------------------------------------------------------------
INSERT INTO appointments (patient_id, doctor_id, appointment_date, status, reason)
SELECT p.patient_id, d.doctor_id, (CURRENT_TIMESTAMP + (i * INTERVAL '1 day'))::timestamp, 'scheduled', 'Routine checkup'
FROM (SELECT patient_id, row_number() OVER () as rn FROM patients WHERE deleted_at IS NULL ORDER BY patient_id LIMIT 12) p
CROSS JOIN LATERAL (SELECT doctor_id FROM doctors ORDER BY doctor_id LIMIT 1 OFFSET ((p.rn - 1) % (SELECT COUNT(*) FROM doctors))) d
CROSS JOIN LATERAL (SELECT (p.rn) AS i)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Medical records — create for several appointments (up to 10)
-- ---------------------------------------------------------------------
INSERT INTO medical_records (appointment_id, diagnosis, symptoms, notes)
SELECT a.appointment_id, 'General Check', 'Headache, fatigue', 'Routine exam — vitals normal'
FROM appointments a
LIMIT 10
ON CONFLICT (appointment_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Patient allergies — add entries (>=10)
-- ---------------------------------------------------------------------
INSERT INTO patient_allergies (patient_id, allergen, reaction, severity)
SELECT p.patient_id, v.allergen, v.reaction, v.severity
FROM patients p
CROSS JOIN (VALUES
  ('Peanuts','Hives','severe'),
  ('Dust','Sneezing','moderate'),
  ('Penicillin','Rash','severe'),
  ('Bee sting','Anaphylaxis','severe'),
  ('Latex','Contact rash','moderate'),
  ('Mold','Cough','mild'),
  ('Shellfish','Hives','severe'),
  ('Grass','Itchy eyes','mild'),
  ('Eggs','Nausea','moderate'),
  ('Milk','Diarrhea','moderate')
) v(allergen, reaction, severity)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Vital signs — link to first 10 appointments
-- ---------------------------------------------------------------------
INSERT INTO vital_signs (appointment_id, blood_pressure_systolic, blood_pressure_diastolic, heart_rate, temperature_celsius, weight_kg, height_cm)
SELECT a.appointment_id, 120 + (i % 5), 80 + (i % 3), 70 + (i % 10), 36.6 + ((i%3) * 0.1), 70 + (i % 15), 170 - (i % 10)
FROM (SELECT appointment_id, row_number() OVER () as i FROM appointments ORDER BY appointment_date LIMIT 10) a
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Lab orders + results (>=10)
-- ---------------------------------------------------------------------
INSERT INTO lab_orders (appointment_id, doctor_id, test_name, status, ordered_at)
SELECT a.appointment_id, a.doctor_id, v.test_name, 'ordered', CURRENT_TIMESTAMP
FROM appointments a
CROSS JOIN (VALUES
  ('Complete Blood Count'), ('Chest X-Ray'), ('Liver Function Test'), ('Renal Panel'),
  ('Blood Glucose'), ('Urinalysis'), ('Pregnancy Test'), ('Thyroid Panel'), ('Lipid Panel'), ('Malaria Rapid Test')
) v(test_name)
ON CONFLICT DO NOTHING;

INSERT INTO lab_results (lab_order_id, result_value, unit, reference_range, is_abnormal, completed_at)
SELECT lo.lab_order_id, 'Normal', 'n/a', 'n/a', FALSE, CURRENT_TIMESTAMP
FROM lab_orders lo
LIMIT 10
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Prescriptions + items (>=10 prescriptions, items linked to medications)
-- ---------------------------------------------------------------------
INSERT INTO prescriptions (appointment_id, date_issued)
SELECT a.appointment_id, CURRENT_DATE FROM appointments a LIMIT 10
ON CONFLICT DO NOTHING;

INSERT INTO prescription_items (prescription_id, medication_id, dosage, quantity, instructions)
SELECT pr.prescription_id, m.medication_id, '1 tab', 7, 'Take once daily'
FROM prescriptions pr
CROSS JOIN LATERAL (SELECT medication_id FROM medications ORDER BY medication_id LIMIT 1 OFFSET (abs(hashtext(pr.prescription_id::text)) % (SELECT COUNT(*) FROM medications))) m
LIMIT 20
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Doctor schedules — ensure each doctor has a few schedule rows (>=10 total)
-- ---------------------------------------------------------------------
INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, is_available)
SELECT d.doctor_id, v.day, '08:00', '16:00', TRUE
FROM doctors d
CROSS JOIN (VALUES ('Mon'),('Tue'),('Wed'),('Thu'),('Fri')) v(day)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Prescriptions / Prescription items already added above
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- Patient feedback — >=10 entries
-- ---------------------------------------------------------------------
INSERT INTO patient_feedback (patient_id, appointment_id, rating, comments, date_submitted)
SELECT p.patient_id, a.appointment_id, (3 + (row_number() OVER () % 3)), 'Satisfactory service', CURRENT_DATE
FROM patients p
JOIN appointments a ON a.patient_id = p.patient_id
LIMIT 10
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Invoices — create invoice rows for recent appointments (>=10)
-- ---------------------------------------------------------------------
INSERT INTO invoices (appointment_id, patient_id, total_amount, payment_status, issued_at)
SELECT a.appointment_id, a.patient_id, 100.00 + (row_number() OVER () * 10), 'unpaid', CURRENT_TIMESTAMP
FROM appointments a
LIMIT 10
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Users — add additional user accounts (ensure >=10 users), map to existing roles
-- Note: Roles/permissions are not recreated. We map users to roles in user_roles.
-- ---------------------------------------------------------------------
-- Shared seed password hash used by RBAC seed
-- '$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq'

INSERT INTO users (doctor_id, username, password_hash, email, is_active) VALUES
  (NULL, 'nshuti.admin@hms.com',   '$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', 'nshuti.admin@hms.com', TRUE),
  (NULL, 'uwase.reception@hms.com','$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', 'uwase.reception@hms.com', TRUE),
  (NULL, 'mugisha.analyst@hms.com','$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', 'mugisha.analyst@hms.com', TRUE),
  (NULL, 'nyirahabimana.pharm@hms.com','$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', 'nyirahabimana.pharm@hms.com', TRUE),
  (NULL, 'habimana.doctor@hms.com','$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', 'habimana.doctor@hms.com', TRUE)
ON CONFLICT (username) DO NOTHING;


-- Create doctor-linked user accounts for some doctors (if not already present)
INSERT INTO users (doctor_id, username, password_hash, email, is_active)
SELECT d.doctor_id, lower(split_part(d.email, '@', 1) || '@hms.com'), '$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', lower(split_part(d.email, '@', 1) || '@hms.com'), TRUE
FROM doctors d
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = lower(split_part(d.email, '@', 1) || '@hms.com'))
LIMIT 5
ON CONFLICT (username) DO NOTHING;

-- Map additional users to roles (use existing roles)
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.user_id, r.role_id, NOW()
FROM users u
JOIN roles r ON (
     (u.username LIKE 'admin%' AND r.role_name = 'Admin')
  OR (u.username LIKE 'doctor%' AND r.role_name = 'Doctor')
  OR (u.username LIKE 'reception%' AND r.role_name = 'Receptionist')
  OR (u.username LIKE 'analyst%' AND r.role_name = 'Analyst')
  OR (u.username LIKE 'pharm%' AND r.role_name = 'Pharmacist')
)
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Referrals — create at least 10 (never to self, per chk_referral_not_self)
-- ---------------------------------------------------------------------
INSERT INTO referrals (appointment_id, referring_doctor_id, referred_to_doctor_id, reason, status)
SELECT a.appointment_id, a.doctor_id, d.doctor_id, 'Specialist consultation', 'pending'
FROM appointments a
CROSS JOIN LATERAL (
  SELECT doctor_id FROM doctors WHERE doctor_id <> a.doctor_id ORDER BY doctor_id LIMIT 1
) d
LIMIT 10
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Patient notes — one per recent appointment (>=10)
-- ---------------------------------------------------------------------
INSERT INTO patient_notes (patient_id, appointment_id, author_user_id, author_role, note_text, source)
SELECT a.patient_id, a.appointment_id, u.user_id, 'Doctor', 'Follow-up: patient responding well, continue current plan.', 'medical_records'
FROM appointments a
CROSS JOIN LATERAL (SELECT user_id FROM users WHERE doctor_id IS NOT NULL ORDER BY user_id LIMIT 1) u
LIMIT 10
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Notifications — at least 10
-- ---------------------------------------------------------------------
INSERT INTO notifications (type, actor_user_id, recipients, payload, channels, status, priority, read_at)
SELECT 'APPOINTMENT_REMINDER', u.user_id, jsonb_build_array(u.user_id), jsonb_build_object('appointment_id', a.appointment_id), jsonb_build_array('in_app'), jsonb_build_object('in_app', 'sent'), 'normal', NULL
FROM users u
JOIN appointments a ON a.patient_id IS NOT NULL
LIMIT 10
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Audit log — at least 10 entries
-- ---------------------------------------------------------------------
INSERT INTO audit_log (user_id, action, table_affected, record_id)
SELECT u.user_id, 'CREATE', 'patients', NULL
FROM users u
LIMIT 10
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- User sessions — at least 10
-- ---------------------------------------------------------------------
INSERT INTO user_sessions (user_id, expires_at, ip_address, user_agent, is_active)
SELECT u.user_id, NOW() + INTERVAL '8 hours', '127.0.0.1', 'Seed Data', TRUE
FROM users u
LIMIT 10
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- System logs — at least 10 (append-only)
-- ---------------------------------------------------------------------
INSERT INTO system_logs (log_level, source, message, user_id)
SELECT 'INFO', 'SeedData', 'Sample system log entry generated during seed.', u.user_id
FROM users u
LIMIT 10
ON CONFLICT DO NOTHING;

COMMIT;

-- =====================================================================
-- End of additional seed data.
-- Re-run verification queries from hospital_rbac_seed_postgresql.sql to inspect mapping.
-- =====================================================================
