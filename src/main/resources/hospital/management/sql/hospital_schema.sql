-- =====================================================================
-- Hospital Management System — Full Schema DDL
-- Target engine: PostgreSQL (see hospital_db_schema.md §9 for rationale)
-- Includes: all 25 tables, constraints, indexes, and updated_at triggers.
-- Views / stored procedures / DCL grants are provided as a separate
-- follow-up script so this one stays focused on schema creation.
--
-- IMPORTANT — unlike MySQL, PostgreSQL cannot CREATE DATABASE and then
-- USE it in the same script/connection (CREATE DATABASE can't run
-- inside a transaction block, and a session can't switch databases
-- mid-connection). Create the database first, as its own step:
--
--   createdb hospital_db
--   -- or, from psql:
--   -- CREATE DATABASE hospital_db;
--
-- Then connect to it and run this file against that connection:
--
--   psql -U your_user -d hospital_db -f hospital_schema.sql
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Reusable trigger function: keeps updated_at current on every UPDATE
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 1. DEPARTMENTS
-- =====================================================================
CREATE TABLE departments (
  department_id  SERIAL PRIMARY KEY,
  name           VARCHAR(100) NOT NULL UNIQUE,
  location       VARCHAR(150),
  phone          VARCHAR(20),
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at     TIMESTAMP NULL
);

CREATE TRIGGER trg_departments_updated_at
  BEFORE UPDATE ON departments
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 2. DOCTORS
-- =====================================================================
CREATE TABLE doctors (
  doctor_id       SERIAL PRIMARY KEY,
  department_id   INT REFERENCES departments(department_id) ON DELETE RESTRICT,
  first_name      VARCHAR(50) NOT NULL,
  last_name       VARCHAR(50) NOT NULL,
  specialization  VARCHAR(100),
  phone           VARCHAR(20),
  email           VARCHAR(100) UNIQUE,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP NULL
);

CREATE INDEX idx_doctors_specialization ON doctors(specialization);
CREATE INDEX idx_doctors_active ON doctors(doctor_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_doctors_updated_at
  BEFORE UPDATE ON doctors
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 3. PATIENTS
-- =====================================================================
CREATE TABLE patients (
  patient_id   SERIAL PRIMARY KEY,
  first_name   VARCHAR(50) NOT NULL,
  last_name    VARCHAR(50) NOT NULL,
  dob          DATE NOT NULL,
  gender       VARCHAR(10) CHECK (gender IN ('M','F','Other')),
  phone        VARCHAR(20),
  email        VARCHAR(100),
  address      VARCHAR(255),
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at   TIMESTAMP NULL
);

CREATE INDEX idx_patients_name ON patients(last_name, first_name);
CREATE INDEX idx_patients_active ON patients(patient_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_patients_updated_at
  BEFORE UPDATE ON patients
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 4. APPOINTMENTS
-- =====================================================================
CREATE TABLE appointments (
  appointment_id    SERIAL PRIMARY KEY,
  patient_id        INT NOT NULL REFERENCES patients(patient_id) ON DELETE RESTRICT,
  doctor_id         INT NOT NULL REFERENCES doctors(doctor_id) ON DELETE RESTRICT,
  appointment_date  TIMESTAMP NOT NULL,
  status            VARCHAR(20) NOT NULL DEFAULT 'scheduled'
                     CHECK (status IN ('scheduled','completed','cancelled')),
  reason            VARCHAR(255),
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at        TIMESTAMP NULL
);

CREATE INDEX idx_appointments_date ON appointments(appointment_date);
CREATE INDEX idx_appointments_doctor ON appointments(doctor_id);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);

CREATE TRIGGER trg_appointments_updated_at
  BEFORE UPDATE ON appointments
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 5. MEDICAL_RECORDS  (1:0..1 with appointments)
-- =====================================================================
CREATE TABLE medical_records (
  record_id       SERIAL PRIMARY KEY,
  appointment_id  INT NOT NULL UNIQUE REFERENCES appointments(appointment_id) ON DELETE RESTRICT,
  diagnosis       VARCHAR(255),
  symptoms        TEXT,
  notes           TEXT,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP NULL
);

CREATE TRIGGER trg_medical_records_updated_at
  BEFORE UPDATE ON medical_records
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 6. REFERRALS  (doctor -> doctor, self-referencing FK pair)
-- =====================================================================
CREATE TABLE referrals (
  referral_id             SERIAL PRIMARY KEY,
  appointment_id          INT NOT NULL REFERENCES appointments(appointment_id) ON DELETE RESTRICT,
  referring_doctor_id     INT NOT NULL REFERENCES doctors(doctor_id) ON DELETE RESTRICT,
  referred_to_doctor_id   INT NOT NULL REFERENCES doctors(doctor_id) ON DELETE RESTRICT,
  reason                  VARCHAR(255),
  status                  VARCHAR(20) NOT NULL DEFAULT 'pending'
                           CHECK (status IN ('pending','scheduled','completed')),
  created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at              TIMESTAMP NULL,
  CONSTRAINT chk_referral_not_self CHECK (referred_to_doctor_id <> referring_doctor_id)
);

CREATE INDEX idx_referrals_appointment ON referrals(appointment_id);

CREATE TRIGGER trg_referrals_updated_at
  BEFORE UPDATE ON referrals
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 7. PATIENT_ALLERGIES
-- =====================================================================
CREATE TABLE patient_allergies (
  allergy_id   SERIAL PRIMARY KEY,
  patient_id   INT NOT NULL REFERENCES patients(patient_id) ON DELETE RESTRICT,
  allergen     VARCHAR(100) NOT NULL,
  reaction     VARCHAR(255),
  severity     VARCHAR(10) CHECK (severity IN ('mild','moderate','severe')),
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at   TIMESTAMP NULL
);

CREATE INDEX idx_allergies_patient ON patient_allergies(patient_id);

CREATE TRIGGER trg_patient_allergies_updated_at
  BEFORE UPDATE ON patient_allergies
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 8. VITAL_SIGNS
-- =====================================================================
CREATE TABLE vital_signs (
  vital_id                   SERIAL PRIMARY KEY,
  appointment_id             INT NOT NULL REFERENCES appointments(appointment_id) ON DELETE RESTRICT,
  blood_pressure_systolic    SMALLINT CHECK (blood_pressure_systolic BETWEEN 1 AND 300),
  blood_pressure_diastolic   SMALLINT CHECK (blood_pressure_diastolic BETWEEN 1 AND 200),
  heart_rate                 SMALLINT CHECK (heart_rate > 0),
  temperature_celsius        DECIMAL(4,1),
  weight_kg                  DECIMAL(5,2),
  height_cm                  DECIMAL(5,2),
  recorded_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at                 TIMESTAMP NULL
);

CREATE INDEX idx_vitals_appointment ON vital_signs(appointment_id);

CREATE TRIGGER trg_vital_signs_updated_at
  BEFORE UPDATE ON vital_signs
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 9. MEDICATIONS  (drug catalog)
-- =====================================================================
CREATE TABLE medications (
  medication_id   SERIAL PRIMARY KEY,
  name            VARCHAR(150) NOT NULL,
  generic_name    VARCHAR(150),
  form            VARCHAR(50),
  unit_price      DECIMAL(10,2) CHECK (unit_price >= 0),
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP NULL
);

CREATE INDEX idx_medications_name ON medications(name);

CREATE TRIGGER trg_medications_updated_at
  BEFORE UPDATE ON medications
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 10. MEDICAL_INVENTORY  (stock/batches)
-- =====================================================================
CREATE TABLE medical_inventory (
  inventory_id        SERIAL PRIMARY KEY,
  medication_id       INT NOT NULL REFERENCES medications(medication_id) ON DELETE RESTRICT,
  batch_number        VARCHAR(50),
  expiry_date         DATE NOT NULL,
  quantity_in_stock   INT NOT NULL DEFAULT 0 CHECK (quantity_in_stock >= 0),
  reorder_level       INT NOT NULL DEFAULT 10,
  supplier            VARCHAR(100),
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at          TIMESTAMP NULL
);

CREATE INDEX idx_inventory_expiry ON medical_inventory(expiry_date);
CREATE INDEX idx_inventory_medication ON medical_inventory(medication_id);
CREATE INDEX idx_inventory_low_stock ON medical_inventory(quantity_in_stock)
  WHERE quantity_in_stock <= reorder_level;

CREATE TRIGGER trg_medical_inventory_updated_at
  BEFORE UPDATE ON medical_inventory
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 11. PRESCRIPTIONS
-- =====================================================================
CREATE TABLE prescriptions (
  prescription_id  SERIAL PRIMARY KEY,
  appointment_id   INT NOT NULL REFERENCES appointments(appointment_id) ON DELETE RESTRICT,
  date_issued      DATE NOT NULL DEFAULT CURRENT_DATE,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at       TIMESTAMP NULL
);

CREATE INDEX idx_prescriptions_appointment ON prescriptions(appointment_id);

CREATE TRIGGER trg_prescriptions_updated_at
  BEFORE UPDATE ON prescriptions
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 12. PRESCRIPTION_ITEMS
-- =====================================================================
CREATE TABLE prescription_items (
  item_id          SERIAL PRIMARY KEY,
  prescription_id  INT NOT NULL REFERENCES prescriptions(prescription_id) ON DELETE CASCADE,
  medication_id    INT NOT NULL REFERENCES medications(medication_id) ON DELETE RESTRICT,
  dosage           VARCHAR(50),
  quantity         INT NOT NULL CHECK (quantity > 0),
  instructions     VARCHAR(255),
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at       TIMESTAMP NULL
);

CREATE INDEX idx_prescription_items_prescription ON prescription_items(prescription_id);
CREATE INDEX idx_prescription_items_medication ON prescription_items(medication_id);

CREATE TRIGGER trg_prescription_items_updated_at
  BEFORE UPDATE ON prescription_items
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 13. LAB_ORDERS
-- =====================================================================
CREATE TABLE lab_orders (
  lab_order_id     SERIAL PRIMARY KEY,
  appointment_id   INT NOT NULL REFERENCES appointments(appointment_id) ON DELETE RESTRICT,
  doctor_id        INT NOT NULL REFERENCES doctors(doctor_id) ON DELETE RESTRICT,
  test_name        VARCHAR(150) NOT NULL,
  status           VARCHAR(20) NOT NULL DEFAULT 'ordered'
                    CHECK (status IN ('ordered','in_progress','completed','cancelled')),
  ordered_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at       TIMESTAMP NULL
);

CREATE INDEX idx_lab_orders_appointment ON lab_orders(appointment_id);

CREATE TRIGGER trg_lab_orders_updated_at
  BEFORE UPDATE ON lab_orders
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 14. LAB_RESULTS  (1:1 with lab_orders)
-- =====================================================================
CREATE TABLE lab_results (
  lab_result_id     SERIAL PRIMARY KEY,
  lab_order_id      INT NOT NULL UNIQUE REFERENCES lab_orders(lab_order_id) ON DELETE CASCADE,
  result_value      VARCHAR(100),
  unit              VARCHAR(20),
  reference_range   VARCHAR(50),
  is_abnormal       BOOLEAN NOT NULL DEFAULT FALSE,
  completed_at      TIMESTAMP,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at        TIMESTAMP NULL
);

CREATE TRIGGER trg_lab_results_updated_at
  BEFORE UPDATE ON lab_results
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 15. DOCTOR_SCHEDULES
-- =====================================================================
CREATE TABLE doctor_schedules (
  schedule_id   SERIAL PRIMARY KEY,
  doctor_id     INT NOT NULL REFERENCES doctors(doctor_id) ON DELETE CASCADE,
  day_of_week   VARCHAR(3) NOT NULL CHECK (day_of_week IN ('Mon','Tue','Wed','Thu','Fri','Sat','Sun')),
  start_time    TIME NOT NULL,
  end_time      TIME NOT NULL,
  is_available  BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at    TIMESTAMP NULL,
  CONSTRAINT chk_schedule_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_schedules_doctor ON doctor_schedules(doctor_id);

CREATE TRIGGER trg_doctor_schedules_updated_at
  BEFORE UPDATE ON doctor_schedules
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 16. PATIENT_FEEDBACK
-- =====================================================================
CREATE TABLE patient_feedback (
  feedback_id      SERIAL PRIMARY KEY,
  patient_id       INT NOT NULL REFERENCES patients(patient_id) ON DELETE RESTRICT,
  appointment_id   INT REFERENCES appointments(appointment_id) ON DELETE SET NULL,
  rating           SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comments         TEXT,
  date_submitted   DATE NOT NULL DEFAULT CURRENT_DATE,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at       TIMESTAMP NULL
);

CREATE INDEX idx_feedback_patient ON patient_feedback(patient_id);

CREATE TRIGGER trg_patient_feedback_updated_at
  BEFORE UPDATE ON patient_feedback
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 17. INVOICES
-- =====================================================================
CREATE TABLE invoices (
  invoice_id       SERIAL PRIMARY KEY,
  appointment_id   INT NOT NULL REFERENCES appointments(appointment_id) ON DELETE RESTRICT,
  patient_id       INT NOT NULL REFERENCES patients(patient_id) ON DELETE RESTRICT,
  total_amount     DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
  payment_status   VARCHAR(20) NOT NULL DEFAULT 'unpaid'
                    CHECK (payment_status IN ('unpaid','partially_paid','paid')),
  issued_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at       TIMESTAMP NULL
);

CREATE INDEX idx_invoices_patient ON invoices(patient_id);
CREATE INDEX idx_invoices_status ON invoices(payment_status);

CREATE TRIGGER trg_invoices_updated_at
  BEFORE UPDATE ON invoices
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 18. USERS  (staff login accounts; RBAC starts here)
-- =====================================================================
CREATE TABLE users (
  user_id         SERIAL PRIMARY KEY,
  doctor_id       INT REFERENCES doctors(doctor_id) ON DELETE SET NULL,
  username        VARCHAR(50) NOT NULL UNIQUE,
  password_hash   VARCHAR(255) NOT NULL,
  email           VARCHAR(100) UNIQUE,
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP NULL
);

CREATE INDEX idx_users_active ON users(user_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 19. ROLES
-- =====================================================================
CREATE TABLE roles (
  role_id      SERIAL PRIMARY KEY,
  role_name    VARCHAR(50) NOT NULL UNIQUE,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at   TIMESTAMP NULL
);

CREATE TRIGGER trg_roles_updated_at
  BEFORE UPDATE ON roles
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 20. PERMISSIONS
-- =====================================================================
CREATE TABLE permissions (
  permission_id  SERIAL PRIMARY KEY,
  resource       VARCHAR(50) NOT NULL,
  action         VARCHAR(50) NOT NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at     TIMESTAMP NULL,
  CONSTRAINT uq_permission_resource_action UNIQUE (resource, action)
);

CREATE TRIGGER trg_permissions_updated_at
  BEFORE UPDATE ON permissions
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 21. USER_ROLES  (M:N junction, composite PK)
-- =====================================================================
CREATE TABLE user_roles (
  user_id       INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  role_id       INT NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
  assigned_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  revoked_at    TIMESTAMP NULL,
  PRIMARY KEY (user_id, role_id)
);

CREATE TRIGGER trg_user_roles_updated_at
  BEFORE UPDATE ON user_roles
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =====================================================================
-- 22. ROLE_PERMISSIONS  (M:N junction, composite PK)
-- =====================================================================
CREATE TABLE role_permissions (
  role_id         INT NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
  permission_id   INT NOT NULL REFERENCES permissions(permission_id) ON DELETE CASCADE,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP NULL,
  PRIMARY KEY (role_id, permission_id)
);

-- =====================================================================
-- 23. AUDIT_LOG  (append-only — no updated_at/deleted_at, see design notes)
-- =====================================================================
CREATE TABLE audit_log (
  log_id           BIGSERIAL PRIMARY KEY,
  user_id          INT REFERENCES users(user_id) ON DELETE SET NULL,
  action           VARCHAR(50) NOT NULL,
  table_affected   VARCHAR(50) NOT NULL,
  record_id        INT,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);

-- =====================================================================
-- 24. USER_SESSIONS
-- =====================================================================
CREATE TABLE user_sessions (
  session_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  login_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  logout_at     TIMESTAMP NULL,
  expires_at    TIMESTAMP NOT NULL,
  ip_address    VARCHAR(45),
  user_agent    VARCHAR(255),
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_active ON user_sessions(user_id) WHERE is_active = TRUE;

CREATE TRIGGER trg_user_sessions_updated_at
  BEFORE UPDATE ON user_sessions
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Requires pgcrypto for gen_random_uuid(); enable once per database:
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================================
-- 25. SYSTEM_LOGS  (append-only — no updated_at/deleted_at, see design notes)
-- =====================================================================
CREATE TABLE system_logs (
  log_id       BIGSERIAL PRIMARY KEY,
  log_level    VARCHAR(10) NOT NULL CHECK (log_level IN ('DEBUG','INFO','WARNING','ERROR')),
  source       VARCHAR(100),
  message      TEXT NOT NULL,
  user_id      INT REFERENCES users(user_id) ON DELETE SET NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_logs_created ON system_logs(created_at);
CREATE INDEX idx_system_logs_level ON system_logs(log_level);

COMMIT;

-- =====================================================================
-- End of schema creation script.
-- Next: seed data, views, stored procedures, and DCL (GRANT/REVOKE) —
-- see hospital_db_schema.md §§6-7 for the design, ask for the follow-up
-- script when ready.
-- =====================================================================
