-- =====================================================================
-- Hospital Management System — RBAC Seed Data (PostgreSQL)
-- Populates application-level RBAC tables plus prerequisite sample data.
--
-- Run AFTER hospital_schema.sql.
-- Idempotent: safe to re-run — ON CONFLICT DO NOTHING throughout.
--
-- Execution order:
--   PART 0  Departments + doctors  (FK prerequisites for users table)
--   PART 1  Roles
--   PART 2  Permissions            (28 resources × 4 actions = 112 rows)
--   PART 3  Role_permissions       (per-role access matrix)
--   PART 4  Users + user_roles     (5 sample accounts, one per role)
--   PART 5  Verification queries   (run after COMMIT to confirm mapping)
-- =====================================================================

BEGIN;

-- =====================================================================
-- PART 0 — PREREQUISITE SAMPLE DATA
-- Departments and doctors must exist before users because the users
-- table has a nullable FK to doctors(doctor_id).  The doctor@hms.com
-- account is resolved by email subquery so no serial ID is hardcoded.
-- =====================================================================

INSERT INTO departments (name, location, phone) VALUES
  ('Cardiology',  'Block A, 1st Floor',    '+250 788 001 001'),
  ('Pediatrics',  'Block B, Ground Floor', '+250 788 001 002'),
  ('Emergency',   'Block C, Entrance',     '+250 788 001 003'),
  ('Orthopedics', 'Block A, 2nd Floor',    '+250 788 001 004'),
  ('Radiology',   'Block D, Basement',     '+250 788 001 005')
ON CONFLICT (name) DO NOTHING;

INSERT INTO doctors (department_id, first_name, last_name, specialization, phone, email)
SELECT dept.department_id,
       v.first_name, v.last_name, v.specialization, v.phone, v.email
FROM (VALUES
  ('Cardiology',  'Sarah',  'Chen',    'Cardiologist',       '+250 788 100 001', 'sarah.chen@hms.com'),
  ('Pediatrics',  'James',  'Okonkwo', 'Pediatrician',       '+250 788 100 002', 'james.okonkwo@hms.com'),
  ('Emergency',   'Amina',  'Nzoya',   'Emergency Medicine', '+250 788 100 003', 'amina.nzoya@hms.com'),
  ('Orthopedics', 'Robert', 'Haas',    'Orthopedic Surgeon', '+250 788 100 004', 'robert.haas@hms.com'),
  ('Radiology',   'Linda',  'Kimura',  'Radiologist',        '+250 788 100 005', 'linda.kimura@hms.com')
) AS v(dept_name, first_name, last_name, specialization, phone, email)
JOIN departments dept ON dept.name = v.dept_name
ON CONFLICT (email) DO NOTHING;

-- =====================================================================
-- PART 1 — ROLES
-- =====================================================================
INSERT INTO roles (role_name) VALUES
  ('Admin'), ('Doctor'), ('Receptionist'), ('Analyst'), ('Pharmacist')
ON CONFLICT (role_name) DO NOTHING;

-- =====================================================================
-- PART 2 — PERMISSIONS  (25 tables × 4 actions = 100 rows)
-- =====================================================================
INSERT INTO permissions (resource, action) VALUES
  ('departments','create'),       ('departments','read'),
  ('departments','update'),       ('departments','delete'),
  ('doctors','create'),           ('doctors','read'),
  ('doctors','update'),           ('doctors','delete'),
  ('patients','create'),          ('patients','read'),
  ('patients','update'),          ('patients','delete'),
  ('appointments','create'),      ('appointments','read'),
  ('appointments','update'),      ('appointments','delete'),
  ('medical_records','create'),   ('medical_records','read'),
  ('medical_records','update'),   ('medical_records','delete'),
  ('referrals','create'),         ('referrals','read'),
  ('referrals','update'),         ('referrals','delete'),
  ('patient_allergies','create'), ('patient_allergies','read'),
  ('patient_allergies','update'), ('patient_allergies','delete'),
  ('vital_signs','create'),       ('vital_signs','read'),
  ('vital_signs','update'),       ('vital_signs','delete'),
  ('medications','create'),       ('medications','read'),
  ('medications','update'),       ('medications','delete'),
  ('medical_inventory','create'), ('medical_inventory','read'),
  ('medical_inventory','update'), ('medical_inventory','delete'),
  ('prescriptions','create'),     ('prescriptions','read'),
  ('prescriptions','update'),     ('prescriptions','delete'),
  ('prescription_items','create'),('prescription_items','read'),
  ('prescription_items','update'),('prescription_items','delete'),
  ('lab_orders','create'),        ('lab_orders','read'),
  ('lab_orders','update'),        ('lab_orders','delete'),
  ('lab_results','create'),       ('lab_results','read'),
  ('lab_results','update'),       ('lab_results','delete'),
  ('doctor_schedules','create'),  ('doctor_schedules','read'),
  ('doctor_schedules','update'),  ('doctor_schedules','delete'),
  ('patient_feedback','create'),  ('patient_feedback','read'),
  ('patient_feedback','update'),  ('patient_feedback','delete'),
  ('invoices','create'),          ('invoices','read'),
  ('invoices','update'),          ('invoices','delete'),
  ('users','create'),             ('users','read'),
  ('users','update'),             ('users','delete'),
  ('roles','create'),             ('roles','read'),
  ('roles','update'),             ('roles','delete'),
  ('permissions','create'),       ('permissions','read'),
  ('permissions','update'),       ('permissions','delete'),
  ('user_roles','create'),        ('user_roles','read'),
  ('user_roles','update'),        ('user_roles','delete'),
  ('role_permissions','create'),  ('role_permissions','read'),
  ('role_permissions','update'),  ('role_permissions','delete'),
  ('audit_log','create'),         ('audit_log','read'),
  ('audit_log','update'),         ('audit_log','delete'),
  ('user_sessions','create'),     ('user_sessions','read'),
  ('user_sessions','update'),     ('user_sessions','delete'),
  ('system_logs','create'),       ('system_logs','read'),
  ('system_logs','update'),       ('system_logs','delete'),
  ('patient_notes','create'),     ('patient_notes','read'),
  ('patient_notes','update'),     ('patient_notes','delete'),
  ('notifications','create'),     ('notifications','read'),
  ('notifications','update'),     ('notifications','delete'),
  ('developer_tools','create'),   ('developer_tools','read'),
  ('developer_tools','update'),   ('developer_tools','delete')
ON CONFLICT (resource, action) DO NOTHING;

-- =====================================================================
-- PART 3 — ROLE_PERMISSIONS  (which role can do what)
-- =====================================================================

-- Admin: every permission — full control including RBAC tables
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM   roles r
JOIN   permissions p ON TRUE
WHERE  r.role_name = 'Admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Doctor: create/read/update clinical tables; read-only on allergies/meds/inventory;
--         create/read/update own availability (doctor_schedules); no delete, no billing, no users/RBAC
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM   roles r
JOIN   permissions p ON (p.resource, p.action) IN (
    ('patients','create'),            ('patients','read'),            ('patients','update'),
    ('appointments','create'),        ('appointments','read'),        ('appointments','update'),
    ('medical_records','create'),     ('medical_records','read'),     ('medical_records','update'),
    ('prescriptions','create'),       ('prescriptions','read'),       ('prescriptions','update'),
    ('prescription_items','create'),  ('prescription_items','read'),  ('prescription_items','update'),
    ('referrals','create'),           ('referrals','read'),           ('referrals','update'),
    ('vital_signs','create'),         ('vital_signs','read'),         ('vital_signs','update'),
    ('lab_orders','create'),          ('lab_orders','read'),          ('lab_orders','update'),
    ('lab_results','create'),         ('lab_results','read'),         ('lab_results','update'),
    ('doctor_schedules','create'),    ('doctor_schedules','read'),    ('doctor_schedules','update'),
    ('patient_allergies','read'),
    ('medications','read'),
    ('medical_inventory','read')
)
WHERE  r.role_name = 'Doctor'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Receptionist: front-desk CRUD on patients/appointments/schedules/invoices;
--               read-only on medications/inventory; no clinical detail, no users/RBAC
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM   roles r
JOIN   permissions p ON (p.resource, p.action) IN (
    ('patients','create'),          ('patients','read'),          ('patients','update'),
    ('appointments','create'),      ('appointments','read'),      ('appointments','update'),
    ('doctor_schedules','create'),  ('doctor_schedules','read'),  ('doctor_schedules','update'),
    ('invoices','create'),          ('invoices','read'),          ('invoices','update'),
    ('patient_feedback','read'),
    ('medications','read'),
    ('medical_inventory','read')
)
WHERE  r.role_name = 'Receptionist'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Analyst: read-only on every business table (reporting/oversight) — excluding
--          developer_tools (Admin-only regardless) and the RBAC-management tables
--          (users/roles/permissions/user_roles/role_permissions), which are an
--          administrative concern, not a reporting one — an analyst should never
--          be able to read who has which access, only the hospital's operational data.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM   roles r
JOIN   permissions p ON p.action = 'read'
    AND p.resource NOT IN ('developer_tools', 'users', 'roles', 'permissions', 'user_roles', 'role_permissions')
WHERE  r.role_name = 'Analyst'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Pharmacist: manage stock, dispense prescriptions; no patient/appointment/billing access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM   roles r
JOIN   permissions p ON (p.resource, p.action) IN (
    ('medications','create'),        ('medications','read'),       ('medications','update'),
    ('medical_inventory','create'),  ('medical_inventory','read'), ('medical_inventory','update'),
    ('prescriptions','read'),
    ('prescription_items','read'),   ('prescription_items','update')
)
WHERE  r.role_name = 'Pharmacist'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- =====================================================================
-- PART 4 — SAMPLE USERS + USER_ROLES
-- All accounts share seed password "Password@12".
-- Value below is its BCrypt hash ($2b$, 12 rounds) — NOT plaintext.
-- Validates correctly against jBCrypt / Spring BCryptPasswordEncoder.
-- IMPORTANT: rotate every password before any real deployment.
--
-- Accounts:
--   admin@hms.com         → Admin        (no doctor FK)
--   doctor@hms.com        → Doctor       (linked to Dr. Sarah Chen)
--   receptionist@hms.com  → Receptionist (no doctor FK)
--   analyst@hms.com       → Analyst      (no doctor FK)
--   pharmacist@hms.com    → Pharmacist   (no doctor FK)
-- =====================================================================

INSERT INTO users (doctor_id, username, password_hash, email, is_active) VALUES
  (NULL, 'admin@hms.com',        '$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', 'admin@hms.com',        TRUE),
  (NULL, 'receptionist@hms.com', '$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', 'receptionist@hms.com', TRUE),
  (NULL, 'analyst@hms.com',      '$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', 'analyst@hms.com',      TRUE),
  (NULL, 'pharmacist@hms.com',   '$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq', 'pharmacist@hms.com',   TRUE)
ON CONFLICT (username) DO NOTHING;

-- Doctor user resolved via Dr. Sarah Chen's unique email — no hardcoded serial ID
INSERT INTO users (doctor_id, username, password_hash, email, is_active)
SELECT d.doctor_id,
       'doctor@hms.com',
       '$2b$12$isGTdNZOLMaT3704mvQzlOqNwWPR/nxzlFAFrxPdncx6SvieC9ohq',
       'doctor@hms.com',
       TRUE
FROM   doctors d
WHERE  d.email = 'sarah.chen@hms.com'
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.user_id, r.role_id, NOW()
FROM   users u
JOIN   roles r ON (
    (u.username = 'admin@hms.com'        AND r.role_name = 'Admin')
 OR (u.username = 'doctor@hms.com'       AND r.role_name = 'Doctor')
 OR (u.username = 'receptionist@hms.com' AND r.role_name = 'Receptionist')
 OR (u.username = 'analyst@hms.com'      AND r.role_name = 'Analyst')
 OR (u.username = 'pharmacist@hms.com'   AND r.role_name = 'Pharmacist')
)
ON CONFLICT (user_id, role_id) DO NOTHING;

COMMIT;

-- =====================================================================
-- PART 5 — VERIFICATION QUERIES
-- Run these after the COMMIT to confirm the mapping is correct.
-- None of these modify data.
-- =====================================================================

-- 5a. Per-user summary: how many roles and total permissions each user holds
SELECT
  u.user_id,
  u.username,
  u.email,
  u.is_active,
  COUNT(DISTINCT r.role_id)        AS role_count,
  COUNT(DISTINCT p.permission_id)  AS permission_count
FROM   users u
LEFT JOIN user_roles       ur  ON ur.user_id      = u.user_id         AND ur.revoked_at IS NULL
LEFT JOIN roles            r   ON r.role_id       = ur.role_id        AND r.deleted_at  IS NULL
LEFT JOIN role_permissions rp  ON rp.role_id      = r.role_id         AND rp.deleted_at IS NULL
LEFT JOIN permissions      p   ON p.permission_id = rp.permission_id  AND p.deleted_at  IS NULL
WHERE  u.deleted_at IS NULL
GROUP  BY u.user_id, u.username, u.email, u.is_active
ORDER  BY u.user_id;

-- Expected result:
--   admin@hms.com        role_count=1  permission_count=112
--   doctor@hms.com       role_count=1  permission_count=30
--   receptionist@hms.com role_count=1  permission_count=15
--   analyst@hms.com      role_count=1  permission_count=22  (every business-table read;
--                                                             excludes developer_tools and
--                                                             the RBAC-management tables)
--   pharmacist@hms.com   role_count=1  permission_count=8
-- Note: developer_tools (Developer Dashboard: bulk drop/regenerate DB objects,
-- backups) is intentionally Admin-only — no other role's grant references it.

-- 5b. Per-role permission count (quick sanity check)
SELECT
  r.role_name,
  COUNT(rp.permission_id) AS permission_count
FROM   roles r
LEFT JOIN role_permissions rp ON rp.role_id = r.role_id AND rp.deleted_at IS NULL
WHERE  r.deleted_at IS NULL
GROUP  BY r.role_name
ORDER  BY permission_count DESC;

-- 5c. Full mapping: every user → role → resource.action (one row per permission)
--     Useful for auditing exact access rights per account.
SELECT
  u.username,
  r.role_name,
  p.resource,
  p.action,
  ur.assigned_at
FROM   users u
JOIN   user_roles       ur  ON ur.user_id      = u.user_id         AND ur.revoked_at IS NULL
JOIN   roles            r   ON r.role_id       = ur.role_id        AND r.deleted_at  IS NULL
JOIN   role_permissions rp  ON rp.role_id      = r.role_id         AND rp.deleted_at IS NULL
JOIN   permissions      p   ON p.permission_id = rp.permission_id  AND p.deleted_at  IS NULL
WHERE  u.deleted_at IS NULL
ORDER  BY u.username, r.role_name, p.resource, p.action;

-- 5d. Sanity check: users with NO role assigned — should return 0 rows
SELECT u.username, u.email, 'NO ROLE ASSIGNED' AS issue
FROM   users u
LEFT JOIN user_roles ur ON ur.user_id = u.user_id AND ur.revoked_at IS NULL
WHERE  u.deleted_at IS NULL
  AND  ur.user_id IS NULL;

-- 5e. Sanity check: roles with NO permissions — should return 0 rows
SELECT r.role_name, 'NO PERMISSIONS ASSIGNED' AS issue
FROM   roles r
LEFT JOIN role_permissions rp ON rp.role_id = r.role_id AND rp.deleted_at IS NULL
WHERE  r.deleted_at IS NULL
  AND  rp.role_id IS NULL;

-- 5f. Confirm doctor user's FK is wired to an actual doctor record
SELECT
  u.username,
  u.doctor_id,
  d.first_name || ' ' || d.last_name AS linked_doctor,
  d.specialization,
  dept.name AS department
FROM   users u
LEFT JOIN doctors     d    ON d.doctor_id     = u.doctor_id
LEFT JOIN departments dept ON dept.department_id = d.department_id
WHERE  u.deleted_at IS NULL
ORDER  BY u.user_id;