# Hospital Management System — Database Reference

**Engine:** PostgreSQL  
**Schema version:** UUID-based (all PKs use `gen_random_uuid()` via the `pgcrypto` extension)  
**Total tables:** 25  
**Script execution order:** `hospital_schema.sql` → `hospital_objects.sql` → `hospital_rbac_seed_postgresql.sql` → `hospital_indexes_postgresql.sql`

Conceptual diagrams: `CONCEPTUAL_LEVEL_DIAGRAMS.md`
Root sample-data entrypoint: `sample_data_postgresql.sql`

---

## Execution Order

| Step | File | Purpose |
|------|------|---------|
| 1 | `hospital_schema.sql` | Creates all 25 tables, constraints, basic indexes, `updated_at` triggers |
| 2 | `hospital_objects.sql` | Views, stored procedures, business-logic triggers, DCL roles |
| 3 | `hospital_rbac_seed_postgresql.sql` | Seed: departments, doctors, roles, permissions, users |
| 4 | `hospital_indexes_postgresql.sql` | Composite, GIN trigram, and partial active-record indexes |

Quick start on a fresh database:

```bash
createdb hospital_db
psql -U your_user -d hospital_db \
  -f hospital_schema.sql \
  -f hospital_objects.sql \
  -f hospital_rbac_seed_postgresql.sql \
  -f hospital_indexes_postgresql.sql
```

---

## Design Decisions

### UUID primary keys
Every table uses `UUID PRIMARY KEY DEFAULT gen_random_uuid()`. The `pgcrypto` extension (enabled in `hospital_schema.sql`) supplies `gen_random_uuid()`. UUIDs eliminate sequential-ID enumeration attacks and make cross-environment data merges safe.

### Soft delete
All entity tables carry a `deleted_at TIMESTAMP NULL` column. A row is "deleted" when `deleted_at IS NOT NULL`. Hard-delete is never performed on patient-facing data. Partial indexes (`WHERE deleted_at IS NULL`) keep active-record queries fast.

### Receipt = paid Invoice
A receipt is not a separate table or model. An `invoices` row whose `payment_status = 'paid'` is the receipt. Print or export that row as-is. This avoids data duplication and keeps the finance model minimal.

### RBAC: two-layer enforcement
Application-level RBAC is handled by the `roles`, `permissions`, `user_roles`, `role_permissions` tables and checked in the Java service layer.  
Database-level RBAC is enforced by PostgreSQL roles (`admin_role`, `doctor_role`, `receptionist_role`, `analyst_role`, `pharmacist_role`) as defense-in-depth — both must agree for an operation to succeed.

### `updated_at` trigger pattern
A single reusable function `set_updated_at()` is attached via `BEFORE UPDATE` triggers on every table that has an `updated_at` column, so the column is always current without application-side logic.

---

## Tables

### Clinical domain

#### `departments`
| Column | Type | Notes |
|--------|------|-------|
| `department_id` | UUID PK | `gen_random_uuid()` |
| `name` | VARCHAR(100) | NOT NULL, UNIQUE |
| `location` | VARCHAR(150) | |
| `phone` | VARCHAR(20) | |
| `created_at` / `updated_at` | TIMESTAMP | auto-managed |
| `deleted_at` | TIMESTAMP | soft-delete |

#### `doctors`
| Column | Type | Notes |
|--------|------|-------|
| `doctor_id` | UUID PK | |
| `department_id` | UUID FK → departments | ON DELETE RESTRICT |
| `first_name` / `last_name` | VARCHAR(50) | NOT NULL |
| `specialization` | VARCHAR(100) | indexed |
| `phone` / `email` | VARCHAR | email UNIQUE |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Indexes: `idx_doctors_specialization`, `idx_doctors_active` (partial)

#### `patients`
| Column | Type | Notes |
|--------|------|-------|
| `patient_id` | UUID PK | |
| `first_name` / `last_name` | VARCHAR(50) | NOT NULL |
| `dob` | DATE | NOT NULL |
| `gender` | VARCHAR(10) | CHECK ('M','F','Other') |
| `phone` / `email` / `address` | VARCHAR | |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Indexes: `idx_patients_name` (last\_name, first\_name), `idx_patients_active` (partial), `trgm_patients_name` (GIN trigram)

#### `appointments`
| Column | Type | Notes |
|--------|------|-------|
| `appointment_id` | UUID PK | |
| `patient_id` | UUID FK → patients | NOT NULL, ON DELETE RESTRICT |
| `doctor_id` | UUID FK → doctors | NOT NULL, ON DELETE RESTRICT |
| `appointment_date` | TIMESTAMP | NOT NULL |
| `status` | VARCHAR(20) | CHECK ('scheduled','completed','cancelled'), default 'scheduled' |
| `reason` | VARCHAR(255) | |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Indexes: `idx_appointments_date`, `idx_appointments_doctor`, `idx_appointments_patient`, `idx_appointments_doctor_date` (composite, partial), `idx_appointments_patient_date` (composite, partial), `idx_appointments_active` (partial)

#### `medical_records` (1:0..1 with appointments)
| Column | Type | Notes |
|--------|------|-------|
| `record_id` | UUID PK | |
| `appointment_id` | UUID FK → appointments | NOT NULL, UNIQUE, ON DELETE RESTRICT |
| `diagnosis` / `symptoms` / `notes` | VARCHAR / TEXT | |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Index: `idx_medical_records_active` (partial)

#### `referrals`
| Column | Type | Notes |
|--------|------|-------|
| `referral_id` | UUID PK | |
| `appointment_id` | UUID FK → appointments | NOT NULL |
| `referring_doctor_id` | UUID FK → doctors | NOT NULL |
| `referred_to_doctor_id` | UUID FK → doctors | NOT NULL |
| `reason` | VARCHAR(255) | |
| `status` | VARCHAR(20) | CHECK ('pending','scheduled','completed') |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Constraint: `chk_referral_not_self` (`referred_to_doctor_id <> referring_doctor_id`)  
Indexes: `idx_referrals_appointment`, `idx_referrals_referring` (composite status, partial), `idx_referrals_referred_to` (composite status, partial)

#### `patient_allergies`
| Column | Type | Notes |
|--------|------|-------|
| `allergy_id` | UUID PK | |
| `patient_id` | UUID FK → patients | NOT NULL |
| `allergen` | VARCHAR(100) | NOT NULL |
| `reaction` | VARCHAR(255) | |
| `severity` | VARCHAR(10) | CHECK ('mild','moderate','severe') |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Index: `idx_allergies_patient`

#### `vital_signs`
| Column | Type | Notes |
|--------|------|-------|
| `vital_id` | UUID PK | |
| `appointment_id` | UUID FK → appointments | NOT NULL |
| `blood_pressure_systolic` | SMALLINT | CHECK 1–300 |
| `blood_pressure_diastolic` | SMALLINT | CHECK 1–200 |
| `heart_rate` | SMALLINT | CHECK > 0 |
| `temperature_celsius` | DECIMAL(4,1) | |
| `weight_kg` / `height_cm` | DECIMAL(5,2) | |
| `recorded_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Index: `idx_vitals_appointment`

---

### Pharmacy domain

#### `medications`
| Column | Type | Notes |
|--------|------|-------|
| `medication_id` | UUID PK | |
| `name` | VARCHAR(150) | NOT NULL, indexed |
| `generic_name` | VARCHAR(150) | |
| `form` | VARCHAR(50) | e.g. tablet, syrup |
| `unit_price` | DECIMAL(10,2) | CHECK >= 0 |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Indexes: `idx_medications_name`, `idx_medications_active` (partial), `trgm_medications_name` (GIN trigram)

#### `medical_inventory`
| Column | Type | Notes |
|--------|------|-------|
| `inventory_id` | UUID PK | |
| `medication_id` | UUID FK → medications | NOT NULL |
| `batch_number` | VARCHAR(50) | |
| `expiry_date` | DATE | NOT NULL |
| `quantity_in_stock` | INT | NOT NULL, CHECK >= 0 |
| `reorder_level` | INT | NOT NULL, default 10 |
| `supplier` | VARCHAR(100) | |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Indexes: `idx_inventory_expiry`, `idx_inventory_medication`, `idx_inventory_low_stock` (partial — stock ≤ reorder), `idx_inventory_medication_expiry` (composite, partial), `idx_inventory_active` (partial)

#### `prescriptions`
| Column | Type | Notes |
|--------|------|-------|
| `prescription_id` | UUID PK | |
| `appointment_id` | UUID FK → appointments | NOT NULL |
| `date_issued` | DATE | default CURRENT\_DATE |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Indexes: `idx_prescriptions_appointment`, `idx_prescriptions_active` (partial)

#### `prescription_items`
| Column | Type | Notes |
|--------|------|-------|
| `item_id` | UUID PK | |
| `prescription_id` | UUID FK → prescriptions | NOT NULL, ON DELETE CASCADE |
| `medication_id` | UUID FK → medications | NOT NULL |
| `dosage` | VARCHAR(50) | |
| `quantity` | INT | NOT NULL, CHECK > 0 |
| `instructions` | VARCHAR(255) | |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Indexes: `idx_prescription_items_prescription`, `idx_prescription_items_medication`

---

### Lab domain

#### `lab_orders`
| Column | Type | Notes |
|--------|------|-------|
| `lab_order_id` | UUID PK | |
| `appointment_id` | UUID FK → appointments | NOT NULL |
| `doctor_id` | UUID FK → doctors | NOT NULL |
| `test_name` | VARCHAR(150) | NOT NULL |
| `status` | VARCHAR(20) | CHECK ('ordered','in\_progress','completed','cancelled') |
| `ordered_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Indexes: `idx_lab_orders_appointment`, `idx_lab_orders_active` (partial)

#### `lab_results` (1:1 with lab_orders)
| Column | Type | Notes |
|--------|------|-------|
| `lab_result_id` | UUID PK | |
| `lab_order_id` | UUID FK → lab\_orders | NOT NULL, UNIQUE, ON DELETE CASCADE |
| `result_value` | VARCHAR(100) | |
| `unit` / `reference_range` | VARCHAR | |
| `is_abnormal` | BOOLEAN | default FALSE |
| `completed_at` | TIMESTAMP | |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

---

### Scheduling & feedback

#### `doctor_schedules`
| Column | Type | Notes |
|--------|------|-------|
| `schedule_id` | UUID PK | |
| `doctor_id` | UUID FK → doctors | NOT NULL, ON DELETE CASCADE |
| `day_of_week` | VARCHAR(3) | CHECK ('Mon'–'Sun') |
| `start_time` / `end_time` | TIME | `chk_schedule_time_order`: end > start |
| `is_available` | BOOLEAN | default TRUE |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Index: `idx_schedules_doctor`

#### `patient_feedback`
| Column | Type | Notes |
|--------|------|-------|
| `feedback_id` | UUID PK | |
| `patient_id` | UUID FK → patients | NOT NULL |
| `appointment_id` | UUID FK → appointments | ON DELETE SET NULL |
| `rating` | SMALLINT | NOT NULL, CHECK 1–5 |
| `comments` | TEXT | |
| `date_submitted` | DATE | default CURRENT\_DATE |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Index: `idx_feedback_patient`

---

### Finance domain

#### `invoices`
| Column | Type | Notes |
|--------|------|-------|
| `invoice_id` | UUID PK | |
| `appointment_id` | UUID FK → appointments | NOT NULL |
| `patient_id` | UUID FK → patients | NOT NULL |
| `total_amount` | DECIMAL(10,2) | NOT NULL, CHECK >= 0, default 0 |
| `payment_status` | VARCHAR(20) | CHECK ('unpaid','partially\_paid','paid'), default 'unpaid' |
| `issued_at` | TIMESTAMP | acts as `created_at` |
| `updated_at` / `deleted_at` | TIMESTAMP | |

Indexes: `idx_invoices_patient`, `idx_invoices_status`, `idx_invoices_active` (partial)

> **Receipt:** When `payment_status = 'paid'`, this row is the receipt. No separate table or model is needed — generate a receipt document by querying `vw_invoice_summary` filtered by `invoice_id` and rendering it.

---

### Auth / RBAC domain

#### `users`
| Column | Type | Notes |
|--------|------|-------|
| `user_id` | UUID PK | |
| `doctor_id` | UUID FK → doctors | nullable — only set for doctor accounts |
| `username` | VARCHAR(50) | NOT NULL, UNIQUE |
| `password_hash` | VARCHAR(255) | NOT NULL (BCrypt, 12 rounds) |
| `email` | VARCHAR(100) | UNIQUE |
| `is_active` | BOOLEAN | NOT NULL, default TRUE |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Index: `idx_users_active` (partial)

#### `roles`
| Column | Type | Notes |
|--------|------|-------|
| `role_id` | UUID PK | |
| `role_name` | VARCHAR(50) | NOT NULL, UNIQUE |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

#### `permissions`
| Column | Type | Notes |
|--------|------|-------|
| `permission_id` | UUID PK | |
| `resource` | VARCHAR(50) | NOT NULL |
| `action` | VARCHAR(50) | NOT NULL |
| `created_at` / `updated_at` / `deleted_at` | TIMESTAMP | |

Constraint: `uq_permission_resource_action` UNIQUE(resource, action)

#### `user_roles` (junction)
| Column | Type | Notes |
|--------|------|-------|
| `user_id` | UUID FK → users | composite PK, ON DELETE CASCADE |
| `role_id` | UUID FK → roles | composite PK, ON DELETE CASCADE |
| `assigned_at` | TIMESTAMP | |
| `revoked_at` | TIMESTAMP | NULL = active |

Index: `idx_user_roles_user` (partial, `WHERE revoked_at IS NULL`)

#### `role_permissions` (junction)
| Column | Type | Notes |
|--------|------|-------|
| `role_id` | UUID FK → roles | composite PK, ON DELETE CASCADE |
| `permission_id` | UUID FK → permissions | composite PK, ON DELETE CASCADE |
| `created_at` | TIMESTAMP | |
| `deleted_at` | TIMESTAMP | soft-revoke |

Index: `idx_role_permissions_role` (partial, `WHERE deleted_at IS NULL`)

#### `user_sessions`
| Column | Type | Notes |
|--------|------|-------|
| `session_id` | UUID PK | |
| `user_id` | UUID FK → users | NOT NULL, ON DELETE CASCADE |
| `login_at` | TIMESTAMP | acts as `created_at` |
| `logout_at` | TIMESTAMP | NULL = still active |
| `expires_at` | TIMESTAMP | NOT NULL |
| `ip_address` | VARCHAR(45) | |
| `user_agent` | VARCHAR(255) | |
| `is_active` | BOOLEAN | default TRUE |
| `updated_at` | TIMESTAMP | |

Indexes: `idx_sessions_user`, `idx_sessions_active` (partial, `WHERE is_active = TRUE`)

---

### Audit domain

#### `audit_log` (append-only)
| Column | Type | Notes |
|--------|------|-------|
| `log_id` | UUID PK | |
| `user_id` | UUID FK → users | ON DELETE SET NULL — preserved if user deleted |
| `action` | VARCHAR(50) | NOT NULL (e.g. 'status: scheduled → completed') |
| `table_affected` | VARCHAR(50) | NOT NULL |
| `record_id` | UUID | UUID of the affected row |
| `created_at` | TIMESTAMP | NOT NULL |

Indexes: `idx_audit_log_user`, `idx_audit_log_created`, `idx_audit_log_record` (composite: table\_affected, record\_id)

#### `system_logs` (append-only)
| Column | Type | Notes |
|--------|------|-------|
| `log_id` | UUID PK | |
| `log_level` | VARCHAR(10) | CHECK ('DEBUG','INFO','WARNING','ERROR') |
| `source` | VARCHAR(100) | e.g. class name or service name |
| `message` | TEXT | NOT NULL |
| `user_id` | UUID FK → users | ON DELETE SET NULL |
| `created_at` | TIMESTAMP | NOT NULL |

Indexes: `idx_system_logs_created`, `idx_system_logs_level`

---

## Views

| View | Source tables | Purpose |
|------|--------------|---------|
| `vw_appointment_details` | appointments, patients, doctors, departments | Full appointment row with names; used by appointment screens |
| `vw_doctor_daily_schedule` | doctors, appointments, patients | Today's appointment list per doctor |
| `vw_low_stock_medications` | medical\_inventory, medications | Inventory at or below reorder level; pharmacy alert feed |
| `vw_patient_summary` | patients, appointments, patient\_allergies, medical\_records | One-row clinical summary per patient |
| `vw_active_sessions` | user\_sessions, users | Who is currently logged in (admin panel) |
| `vw_invoice_summary` | invoices, appointments, patients, doctors | Invoice list with patient/doctor names; billing table feed |
| `vw_pending_lab_orders` | lab\_orders, appointments, patients, doctors | Ordered/in-progress lab work; lab technician queue |

All views filter `deleted_at IS NULL` on their primary table.

---

## Stored Procedures & Functions

### `sp_book_appointment(p_patient_id UUID, p_doctor_id UUID, p_appointment_date TIMESTAMP, p_reason VARCHAR)`
Inserts an appointment after checking for a conflicting active slot on the same doctor/time. Raises an exception (implicit ROLLBACK) on conflict. COMMITs on success.

### `sp_issue_prescription(p_appointment_id UUID, p_items JSONB)`
Creates a prescription header and processes each item in the JSONB array. A SAVEPOINT is used per item: if a medication has insufficient stock the item is skipped with RAISE NOTICE and the savepoint is rolled back, but the rest of the prescription still commits.  
JSONB item shape: `{"medication_id":"<uuid>","dosage":"..","quantity":<int>,"instructions":".."}`

### `fn_calculate_invoice_total(p_appointment_id UUID) → DECIMAL(10,2)`
Returns the sum of `quantity × unit_price` across all non-deleted prescription items for the given appointment. Returns 0 if no items found.

### `sp_create_invoice(p_appointment_id UUID, p_patient_id UUID)`
Calls `fn_calculate_invoice_total`, then inserts an `invoices` row with `payment_status = 'unpaid'`. Idempotent — raises NOTICE and returns early if an active invoice already exists for the appointment.

### `sp_close_session(p_session_id UUID)`
Sets `logout_at = NOW()` and `is_active = FALSE` on the given session.

### `sp_soft_delete_patient(p_patient_id UUID, p_actor_user_id UUID)`
Cascades `deleted_at = NOW()` to: patients, appointments, patient\_allergies, patient\_feedback, invoices. Always writes an `audit_log` entry for traceability. Hard-delete is not used.

---

## Triggers

### `updated_at` maintenance (hospital_schema.sql)
All tables with an `updated_at` column have a `BEFORE UPDATE` trigger named `trg_<table>_updated_at` that calls the shared `set_updated_at()` function. Tables covered: departments, doctors, patients, appointments, medical\_records, referrals, patient\_allergies, vital\_signs, medications, medical\_inventory, prescriptions, prescription\_items, lab\_orders, lab\_results, doctor\_schedules, patient\_feedback, invoices, users, roles, permissions, user\_roles, user\_sessions.

### Business-logic triggers (hospital_objects.sql)

| Trigger | Table | When | What |
|---------|-------|------|------|
| `trg_log_appointment_status` | appointments | AFTER UPDATE | Writes to `audit_log` whenever `status` changes |
| `trg_prevent_double_booking` | appointments | BEFORE INSERT OR UPDATE | Raises exception if same doctor/time slot is already taken (non-cancelled) |
| `trg_log_patient_changes` | patients | AFTER UPDATE | Writes to `audit_log` when name, DOB, or `deleted_at` changes |
| `trg_log_user_changes` | users | AFTER UPDATE | Writes to `audit_log` when `is_active`, `password_hash`, or `deleted_at` changes |

All triggers use `CREATE OR REPLACE FUNCTION` + `DROP TRIGGER IF EXISTS` before recreation, making `hospital_objects.sql` idempotent.

---

## DCL Roles (PostgreSQL-level)

| Role | Tables granted | Restrictions |
|------|---------------|-------------|
| `admin_role` | ALL TABLES — full privileges | None |
| `doctor_role` | patients, appointments, medical\_records, prescriptions, prescription\_items, referrals, vital\_signs, lab\_orders, lab\_results (SELECT/INSERT/UPDATE); patient\_allergies, medications, medical\_inventory (SELECT only) | DELETE revoked on patients, medical\_records |
| `receptionist_role` | patients, appointments, doctor\_schedules, invoices (SELECT/INSERT/UPDATE); medications, medical\_inventory (SELECT only); patient\_feedback (SELECT only) | DELETE revoked on all tables |
| `analyst_role` | ALL TABLES — SELECT only | INSERT/UPDATE/DELETE revoked on all |
| `pharmacist_role` | medical\_inventory (SELECT/INSERT/UPDATE); prescription\_items (SELECT/UPDATE); medications, prescriptions (SELECT only) | No access to patient/appointment/billing tables |

All `CREATE ROLE` statements use `DO $$ IF NOT EXISTS` guards — safe to re-run.

---

## Indexes Summary

### From `hospital_schema.sql`
Single-column indexes on the most common filter/join/sort columns — see the schema file for the full list.

### From `hospital_indexes_postgresql.sql`

**Composite indexes (multi-column WHERE + ORDER BY):**

| Index | Table | Columns | Condition |
|-------|-------|---------|-----------|
| `idx_appointments_doctor_date` | appointments | doctor\_id, appointment\_date | `deleted_at IS NULL` |
| `idx_appointments_patient_date` | appointments | patient\_id, appointment\_date | `deleted_at IS NULL` |
| `idx_inventory_medication_expiry` | medical\_inventory | medication\_id, expiry\_date | `deleted_at IS NULL` |
| `idx_referrals_referring` | referrals | referring\_doctor\_id, status | `deleted_at IS NULL` |
| `idx_referrals_referred_to` | referrals | referred\_to\_doctor\_id, status | `deleted_at IS NULL` |
| `idx_audit_log_record` | audit\_log | table\_affected, record\_id | — |
| `idx_user_roles_user` | user\_roles | user\_id | `revoked_at IS NULL` |
| `idx_role_permissions_role` | role\_permissions | role\_id | `deleted_at IS NULL` |

**GIN trigram indexes (ILIKE '%text%' search):**

| Index | Table | Expression |
|-------|-------|-----------|
| `trgm_patients_name` | patients | `(first_name \|\| ' ' \|\| last_name)` |
| `trgm_doctors_name` | doctors | `(first_name \|\| ' ' \|\| last_name)` |
| `trgm_medications_name` | medications | `name` |

**Partial active-record indexes:** appointments, medical\_records, prescriptions, lab\_orders, invoices, medications, medical\_inventory — each has an `idx_<table>_active` index on the PK `WHERE deleted_at IS NULL`, so soft-delete-filtered queries skip deleted rows cheaply.

---

## Seeded Data

Run `hospital_rbac_seed_postgresql.sql` to populate the following. The script is idempotent (`ON CONFLICT DO NOTHING` throughout).

### Departments (5)
Cardiology, Pediatrics, Emergency, Orthopedics, Radiology

### Doctors (5)
| Name | Department | Specialization |
|------|-----------|----------------|
| Sarah Chen | Cardiology | Cardiologist |
| James Okonkwo | Pediatrics | Pediatrician |
| Amina Nzoya | Emergency | Emergency Medicine |
| Robert Haas | Orthopedics | Orthopedic Surgeon |
| Linda Kimura | Radiology | Radiologist |

### Roles & Application Permissions

| Role | Permission count | Description |
|------|-----------------|-------------|
| Admin | 100 (all 25 tables × 4 actions) | Full system access |
| Doctor | 30 | Clinical tables create/read/update; read-only on allergies, meds, inventory |
| Receptionist | 15 | Front-desk: patients, appointments, schedules, invoices; read-only on meds/inventory |
| Analyst | 25 | Read-only on every table |
| Pharmacist | 8 | Manage medications/inventory; read prescriptions; no patient or billing access |

### Seed Users

| Username | Role | Doctor FK |
|----------|------|-----------|
| `admin@hms.com` | Admin | — |
| `doctor@hms.com` | Doctor | Dr. Sarah Chen (resolved by email) |
| `receptionist@hms.com` | Receptionist | — |
| `analyst@hms.com` | Analyst | — |
| `pharmacist@hms.com` | Pharmacist | — |

**Seed password for all accounts:** `Password@12`  
Password is stored as a BCrypt hash (cost factor 12). Rotate before any real deployment.

### Verification

Run the queries in **PART 5** of `hospital_rbac_seed_postgresql.sql` after seeding to confirm:

```
-- Expected output of query 5a:
admin@hms.com        role_count=1  permission_count=100
doctor@hms.com       role_count=1  permission_count=30
receptionist@hms.com role_count=1  permission_count=15
analyst@hms.com      role_count=1  permission_count=25
pharmacist@hms.com   role_count=1  permission_count=8
```

---

## Java Model Mapping

| DB table | Java class | Package |
|----------|-----------|---------|
| departments | `Department` | `backend.model.doctor` |
| doctors | `Doctor` | `backend.model.doctor` |
| doctor\_schedules | `DoctorSchedule` | `backend.model.doctor` |
| referrals | `Referral` | `backend.model.doctor` |
| patients | `Patient` | `backend.model.patient` |
| appointments | `Appointment` | `backend.model.patient` |
| medical\_records | `MedicalRecord` | `backend.model.patient` |
| patient\_allergies | `PatientAllergy` | `backend.model.patient` |
| patient\_feedback | `PatientFeedback` | `backend.model.patient` |
| vital\_signs | `VitalSign` | `backend.model.patient` |
| medications | `Medication` | `backend.model.pharmacy` |
| medical\_inventory | `MedicalInventory` | `backend.model.pharmacy` |
| prescriptions | `Prescription` | `backend.model.pharmacy` |
| prescription\_items | `PrescriptionItem` | `backend.model.pharmacy` |
| lab\_orders | `LabOrder` | `backend.model.lab` |
| lab\_results | `LabResult` | `backend.model.lab` |
| invoices | `Invoice` | `backend.model.finance` |
| users | `User` | `backend.model.user` |
| roles | `Role` | `backend.model.user` |
| permissions | `Permission` | `backend.model.user` |
| user\_roles | `UserRole` | `backend.model.user` |
| role\_permissions | `RolePermission` | `backend.model.user` |
| user\_sessions | `UserSession` | `backend.model.user` |
| audit\_log | `AuditLog` | `backend.model.user` |
| system\_logs | `SystemLog` | `backend.model.user` |

All model classes use JavaFX `SimpleXxxProperty` fields with `getXxx()` / `xxxProperty()` accessors for direct `TableView` / `PropertyValueFactory` binding.