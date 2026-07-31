-- =====================================================================
-- Hospital Management System — Database Objects
-- Run this AFTER hospital_schema.sql (tables must exist first).
-- Contains: views, stored procedures/functions (with TCL: COMMIT,
-- ROLLBACK, SAVEPOINT), business-logic triggers, and DCL (GRANT/REVOKE).
-- Target engine: PostgreSQL.
-- Safe to re-run: uses CREATE OR REPLACE, DROP TRIGGER IF EXISTS,
-- and DO $$ IF NOT EXISTS blocks for roles.
-- =====================================================================

-- =====================================================================
-- PART 1 — VIEWS
-- =====================================================================

-- Full appointment context in one row: patient, doctor, department
CREATE OR REPLACE VIEW vw_appointment_details AS
SELECT
  a.appointment_id,
  a.appointment_date,
  a.status,
  a.reason,
  p.patient_id,
  p.first_name || ' ' || p.last_name AS patient_name,
  d.doctor_id,
  d.first_name || ' ' || d.last_name AS doctor_name,
  dept.name AS department_name
FROM appointments a
JOIN patients p    ON p.patient_id    = a.patient_id
JOIN doctors d     ON d.doctor_id     = a.doctor_id
LEFT JOIN departments dept ON dept.department_id = d.department_id
WHERE a.deleted_at IS NULL;

-- Each doctor's bookings for today — feeds the JavaFX schedule screen
CREATE OR REPLACE VIEW vw_doctor_daily_schedule AS
SELECT
  d.doctor_id,
  d.first_name || ' ' || d.last_name AS doctor_name,
  a.appointment_id,
  a.appointment_date,
  a.status,
  p.first_name || ' ' || p.last_name AS patient_name
FROM doctors d
JOIN appointments a ON a.doctor_id  = d.doctor_id
JOIN patients p     ON p.patient_id = a.patient_id
WHERE a.appointment_date::date = CURRENT_DATE
  AND a.deleted_at IS NULL
ORDER BY d.doctor_id, a.appointment_date;

-- Inventory at or below reorder point — feeds a pharmacy alert screen
CREATE OR REPLACE VIEW vw_low_stock_medications AS
SELECT
  m.medication_id,
  m.name,
  i.inventory_id,
  i.batch_number,
  i.quantity_in_stock,
  i.reorder_level,
  i.expiry_date
FROM medical_inventory i
JOIN medications m ON m.medication_id = i.medication_id
WHERE i.quantity_in_stock <= i.reorder_level
  AND i.deleted_at IS NULL;

-- One-row clinical summary per patient: visit count, allergies, latest diagnosis
CREATE OR REPLACE VIEW vw_patient_summary AS
SELECT
  p.patient_id,
  p.first_name || ' ' || p.last_name AS patient_name,
  p.dob,
  (SELECT COUNT(*) FROM appointments a
     WHERE a.patient_id = p.patient_id AND a.deleted_at IS NULL)      AS total_appointments,
  (SELECT string_agg(DISTINCT allergen, ', ')
     FROM patient_allergies pa
     WHERE pa.patient_id = p.patient_id AND pa.deleted_at IS NULL)    AS allergies,
  (SELECT mr.diagnosis
     FROM medical_records mr
     JOIN appointments a2 ON a2.appointment_id = mr.appointment_id
     WHERE a2.patient_id = p.patient_id
     ORDER BY mr.created_at DESC LIMIT 1)                              AS latest_diagnosis
FROM patients p
WHERE p.deleted_at IS NULL;

-- Admin-facing view of who is currently logged in
CREATE OR REPLACE VIEW vw_active_sessions AS
SELECT
  s.session_id,
  u.username,
  s.login_at,
  s.expires_at,
  s.ip_address
FROM user_sessions s
JOIN users u ON u.user_id = s.user_id
WHERE s.is_active = TRUE
  AND s.expires_at > CURRENT_TIMESTAMP;

-- Invoice list with patient name, doctor name, appointment date, and total
CREATE OR REPLACE VIEW vw_invoice_summary AS
SELECT
  i.invoice_id,
  i.issued_at,
  i.payment_status,
  i.total_amount,
  p.patient_id,
  p.first_name || ' ' || p.last_name AS patient_name,
  d.first_name || ' ' || d.last_name AS doctor_name,
  a.appointment_date
FROM invoices i
JOIN appointments a ON a.appointment_id = i.appointment_id
JOIN patients p     ON p.patient_id     = i.patient_id
JOIN doctors d      ON d.doctor_id      = a.doctor_id
WHERE i.deleted_at IS NULL;

-- Lab orders not yet completed — feeds the lab-technician work queue
CREATE OR REPLACE VIEW vw_pending_lab_orders AS
SELECT
  lo.lab_order_id,
  lo.test_name,
  lo.status,
  lo.ordered_at,
  p.first_name || ' ' || p.last_name AS patient_name,
  d.first_name || ' ' || d.last_name AS ordering_doctor
FROM lab_orders lo
JOIN appointments a ON a.appointment_id = lo.appointment_id
JOIN patients p     ON p.patient_id     = a.patient_id
JOIN doctors d      ON d.doctor_id      = lo.doctor_id
WHERE lo.status IN ('ordered', 'in_progress')
  AND lo.deleted_at IS NULL
ORDER BY lo.ordered_at;


-- =====================================================================
-- PART 2 — STORED PROCEDURES & FUNCTIONS
-- (TCL: COMMIT / ROLLBACK TO SAVEPOINT / RELEASE SAVEPOINT)
-- =====================================================================

-- Books an appointment only if the doctor has no conflicting slot.
-- Raises an exception (implicit ROLLBACK) on conflict.
CREATE OR REPLACE PROCEDURE sp_book_appointment(
  p_patient_id       UUID,
  p_doctor_id        UUID,
  p_appointment_date TIMESTAMP,
  p_reason           VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_conflict_count INT;
BEGIN
  SELECT COUNT(*) INTO v_conflict_count
  FROM appointments
  WHERE doctor_id       = p_doctor_id
    AND appointment_date = p_appointment_date
    AND status          <> 'cancelled'
    AND deleted_at IS NULL;

  IF v_conflict_count > 0 THEN
    RAISE EXCEPTION 'Doctor % already has an appointment at %',
      p_doctor_id, p_appointment_date;
  END IF;

  INSERT INTO appointments (patient_id, doctor_id, appointment_date, reason, status)
  VALUES (p_patient_id, p_doctor_id, p_appointment_date, p_reason, 'scheduled');

  COMMIT;
END;
$$;

-- Issues a prescription with multiple line items.
-- SAVEPOINT per item: one out-of-stock drug is skipped and logged via
-- RAISE NOTICE while the rest of the prescription still commits.
-- p_items JSON shape: [{"medication_id":"<uuid>","dosage":"..","quantity":.,"instructions":".."}]
CREATE OR REPLACE PROCEDURE sp_issue_prescription(
  p_appointment_id UUID,
  p_items          JSONB
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_prescription_id UUID;
  v_item            JSONB;
  v_medication_id   UUID;
  v_quantity        INT;
  v_available       INT;
  v_inventory_id    UUID;
BEGIN
  INSERT INTO prescriptions (appointment_id)
  VALUES (p_appointment_id)
  RETURNING prescription_id INTO v_prescription_id;

  FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
  LOOP
    v_medication_id := (v_item->>'medication_id')::UUID;
    v_quantity      := (v_item->>'quantity')::INT;

    SAVEPOINT before_item;

    SELECT inventory_id, quantity_in_stock
      INTO v_inventory_id, v_available
      FROM medical_inventory
      WHERE medication_id = v_medication_id
        AND deleted_at IS NULL
      ORDER BY expiry_date ASC
      LIMIT 1;

    IF v_available IS NULL OR v_available < v_quantity THEN
      RAISE NOTICE 'Insufficient stock for medication %, skipping', v_medication_id;
      ROLLBACK TO SAVEPOINT before_item;
      CONTINUE;
    END IF;

    INSERT INTO prescription_items
      (prescription_id, medication_id, dosage, quantity, instructions)
    VALUES
      (v_prescription_id, v_medication_id,
       v_item->>'dosage', v_quantity, v_item->>'instructions');

    UPDATE medical_inventory
      SET quantity_in_stock = quantity_in_stock - v_quantity
      WHERE inventory_id = v_inventory_id;

    RELEASE SAVEPOINT before_item;
  END LOOP;

  COMMIT;
END;
$$;

-- Computes the medication sub-total for an appointment from prescription items.
-- Does not include consultation fees (those should be added at invoice creation).
CREATE OR REPLACE FUNCTION fn_calculate_invoice_total(p_appointment_id UUID)
RETURNS DECIMAL(10,2)
LANGUAGE plpgsql
AS $$
DECLARE
  v_total DECIMAL(10,2);
BEGIN
  SELECT COALESCE(SUM(pi.quantity * m.unit_price), 0)
    INTO v_total
    FROM prescriptions pr
    JOIN prescription_items pi ON pi.prescription_id = pr.prescription_id
    JOIN medications m         ON m.medication_id    = pi.medication_id
    WHERE pr.appointment_id = p_appointment_id
      AND pr.deleted_at IS NULL;

  RETURN v_total;
END;
$$;

-- Creates an invoice for an appointment, auto-calculating the total
-- from its prescription items. Idempotent: raises notice if one already exists.
CREATE OR REPLACE PROCEDURE sp_create_invoice(
  p_appointment_id UUID,
  p_patient_id     UUID
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_total  DECIMAL(10,2);
  v_exists INT;
BEGIN
  SELECT COUNT(*) INTO v_exists
  FROM invoices
  WHERE appointment_id = p_appointment_id AND deleted_at IS NULL;

  IF v_exists > 0 THEN
    RAISE NOTICE 'Invoice already exists for appointment %', p_appointment_id;
    RETURN;
  END IF;

  v_total := fn_calculate_invoice_total(p_appointment_id);

  INSERT INTO invoices (appointment_id, patient_id, total_amount, payment_status)
  VALUES (p_appointment_id, p_patient_id, v_total, 'unpaid');

  COMMIT;
END;
$$;

-- Closes a login session cleanly
CREATE OR REPLACE PROCEDURE sp_close_session(p_session_id UUID)
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE user_sessions
    SET logout_at = CURRENT_TIMESTAMP,
        is_active = FALSE
    WHERE session_id = p_session_id;

  COMMIT;
END;
$$;

-- Soft-deletes a patient and all records owned by that patient.
-- Cascades to: appointments, patient_allergies, patient_feedback, invoices.
-- Always writes an audit entry for traceability.
CREATE OR REPLACE PROCEDURE sp_soft_delete_patient(
  p_patient_id    UUID,
  p_actor_user_id UUID
)
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE patients
    SET deleted_at = CURRENT_TIMESTAMP
    WHERE patient_id = p_patient_id AND deleted_at IS NULL;

  UPDATE appointments
    SET deleted_at = CURRENT_TIMESTAMP
    WHERE patient_id = p_patient_id AND deleted_at IS NULL;

  UPDATE patient_allergies
    SET deleted_at = CURRENT_TIMESTAMP
    WHERE patient_id = p_patient_id AND deleted_at IS NULL;

  UPDATE patient_feedback
    SET deleted_at = CURRENT_TIMESTAMP
    WHERE patient_id = p_patient_id AND deleted_at IS NULL;

  UPDATE invoices
    SET deleted_at = CURRENT_TIMESTAMP
    WHERE patient_id = p_patient_id AND deleted_at IS NULL;

  INSERT INTO audit_log (user_id, action, table_affected, record_id)
  VALUES (p_actor_user_id, 'soft_delete', 'patients', p_patient_id);

  COMMIT;
END;
$$;


-- =====================================================================
-- PART 3 — BUSINESS-LOGIC TRIGGERS
-- updated_at triggers live in hospital_schema.sql.
-- These enforce domain rules and write audit entries.
-- DROP TRIGGER IF EXISTS makes each block idempotent on re-run.
-- =====================================================================

-- Logs every appointment status change into audit_log
CREATE OR REPLACE FUNCTION log_appointment_status_change()
RETURNS TRIGGER AS $$
BEGIN
  IF OLD.status IS DISTINCT FROM NEW.status THEN
    INSERT INTO audit_log (user_id, action, table_affected, record_id)
    VALUES (NULL,
            'status: ' || OLD.status || ' -> ' || NEW.status,
            'appointments',
            NEW.appointment_id);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_log_appointment_status ON appointments;
CREATE TRIGGER trg_log_appointment_status
  AFTER UPDATE ON appointments
  FOR EACH ROW EXECUTE FUNCTION log_appointment_status_change();

-- Rejects a booking that conflicts with an existing (non-cancelled) slot
-- for the same doctor — second layer of protection on top of sp_book_appointment.
-- On INSERT, NEW.appointment_id may be NULL before the DEFAULT applies,
-- so the NULL guard prevents the row from excluding itself.
CREATE OR REPLACE FUNCTION prevent_double_booking()
RETURNS TRIGGER AS $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM appointments
    WHERE doctor_id      = NEW.doctor_id
      AND appointment_date = NEW.appointment_date
      AND status         <> 'cancelled'
      AND deleted_at IS NULL
      AND (NEW.appointment_id IS NULL OR appointment_id <> NEW.appointment_id)
  ) THEN
    RAISE EXCEPTION 'Doctor % already booked at %',
      NEW.doctor_id, NEW.appointment_date;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_double_booking ON appointments;
CREATE TRIGGER trg_prevent_double_booking
  BEFORE INSERT OR UPDATE ON appointments
  FOR EACH ROW EXECUTE FUNCTION prevent_double_booking();

-- Writes an audit entry whenever key patient fields change
-- (name, dob, or soft-delete). Useful for compliance reporting.
CREATE OR REPLACE FUNCTION log_patient_changes()
RETURNS TRIGGER AS $$
BEGIN
  IF OLD.first_name  IS DISTINCT FROM NEW.first_name
  OR OLD.last_name   IS DISTINCT FROM NEW.last_name
  OR OLD.dob         IS DISTINCT FROM NEW.dob
  OR OLD.deleted_at  IS DISTINCT FROM NEW.deleted_at THEN
    INSERT INTO audit_log (user_id, action, table_affected, record_id)
    VALUES (NULL, 'update', 'patients', NEW.patient_id);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_log_patient_changes ON patients;
CREATE TRIGGER trg_log_patient_changes
  AFTER UPDATE ON patients
  FOR EACH ROW EXECUTE FUNCTION log_patient_changes();

-- Writes an audit entry when a user account changes active status,
-- password hash, or is soft-deleted.
CREATE OR REPLACE FUNCTION log_user_changes()
RETURNS TRIGGER AS $$
BEGIN
  IF OLD.is_active     IS DISTINCT FROM NEW.is_active
  OR OLD.password_hash IS DISTINCT FROM NEW.password_hash
  OR OLD.deleted_at    IS DISTINCT FROM NEW.deleted_at THEN
    INSERT INTO audit_log (user_id, action, table_affected, record_id)
    VALUES (NEW.user_id, 'update', 'users', NEW.user_id);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_log_user_changes ON users;
CREATE TRIGGER trg_log_user_changes
  AFTER UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION log_user_changes();


-- =====================================================================
-- PART 4 — DCL (GRANT / REVOKE)
-- All CREATE ROLE statements use DO $$ IF NOT EXISTS blocks so this
-- script is safe to re-run against an existing database.
-- =====================================================================

DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'admin_role') THEN
    CREATE ROLE admin_role;
  END IF;
END $$;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin_role;

DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'doctor_role') THEN
    CREATE ROLE doctor_role;
  END IF;
END $$;
GRANT SELECT, INSERT, UPDATE ON
  patients, appointments, medical_records, prescriptions,
  prescription_items, referrals, vital_signs, lab_orders, lab_results
  TO doctor_role;
REVOKE DELETE ON patients, medical_records FROM doctor_role;

DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'receptionist_role') THEN
    CREATE ROLE receptionist_role;
  END IF;
END $$;
GRANT SELECT, INSERT, UPDATE ON
  patients, appointments, doctor_schedules, invoices
  TO receptionist_role;
GRANT SELECT ON medications, medical_inventory TO receptionist_role;
REVOKE DELETE ON ALL TABLES IN SCHEMA public FROM receptionist_role;

DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'analyst_role') THEN
    CREATE ROLE analyst_role;
  END IF;
END $$;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analyst_role;
REVOKE INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public FROM analyst_role;

DO $$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'pharmacist_role') THEN
    CREATE ROLE pharmacist_role;
  END IF;
END $$;
GRANT SELECT, INSERT, UPDATE ON medical_inventory  TO pharmacist_role;
GRANT SELECT, UPDATE          ON prescription_items TO pharmacist_role;
GRANT SELECT                  ON medications        TO pharmacist_role;
GRANT SELECT                  ON prescriptions      TO pharmacist_role;

-- =====================================================================
-- EXAMPLE: staff login accounts (uncomment and customize per environment).
-- In PostgreSQL, granting a role to a LOGIN role activates its privileges
-- on connect with no extra session-level SET ROLE needed.
-- =====================================================================
-- CREATE ROLE dr_uwase LOGIN PASSWORD 'ChangeMe123!';
-- GRANT doctor_role TO dr_uwase;

-- CREATE ROLE reception1 LOGIN PASSWORD 'ChangeMe123!';
-- GRANT receptionist_role TO reception1;

-- =====================================================================
-- End of database objects script.
-- Next: hospital_rbac_seed_postgresql.sql → hospital_indexes_postgresql.sql
-- =====================================================================