-- =====================================================================
-- Hospital Management System — Database Objects
-- Run this AFTER hospital_schema.sql (tables must exist first).
-- Contains: views, stored procedures/functions (with TCL: COMMIT,
-- ROLLBACK, SAVEPOINT), business-logic triggers, and DCL (GRANT/REVOKE).
-- Target engine: PostgreSQL.
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
JOIN patients p  ON p.patient_id = a.patient_id
JOIN doctors d   ON d.doctor_id = a.doctor_id
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
JOIN appointments a ON a.doctor_id = d.doctor_id
JOIN patients p     ON p.patient_id = a.patient_id
WHERE a.appointment_date::date = CURRENT_DATE
  AND a.deleted_at IS NULL
ORDER BY d.doctor_id, a.appointment_date;

-- Inventory below its reorder point — feeds a pharmacy alert screen
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

-- One-row clinical summary per patient: appointment count, allergies, latest diagnosis
CREATE OR REPLACE VIEW vw_patient_summary AS
SELECT
  p.patient_id,
  p.first_name || ' ' || p.last_name AS patient_name,
  p.dob,
  (SELECT COUNT(*) FROM appointments a
     WHERE a.patient_id = p.patient_id AND a.deleted_at IS NULL) AS total_appointments,
  (SELECT string_agg(DISTINCT allergen, ', ') FROM patient_allergies pa
     WHERE pa.patient_id = p.patient_id AND pa.deleted_at IS NULL) AS allergies,
  (SELECT mr.diagnosis FROM medical_records mr
     JOIN appointments a2 ON a2.appointment_id = mr.appointment_id
     WHERE a2.patient_id = p.patient_id
     ORDER BY mr.created_at DESC LIMIT 1) AS latest_diagnosis
FROM patients p
WHERE p.deleted_at IS NULL;

-- Admin-facing view of who's currently logged in
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


-- =====================================================================
-- PART 2 — STORED PROCEDURES & FUNCTIONS (TCL: COMMIT / ROLLBACK / SAVEPOINT)
-- =====================================================================

-- Books an appointment only if the doctor has no conflicting slot.
-- Demonstrates: single-statement transaction with explicit COMMIT,
-- and RAISE EXCEPTION causing an implicit ROLLBACK on conflict.
CREATE OR REPLACE PROCEDURE sp_book_appointment(
  p_patient_id INT,
  p_doctor_id INT,
  p_appointment_date TIMESTAMP,
  p_reason VARCHAR
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_conflict_count INT;
BEGIN
  SELECT COUNT(*) INTO v_conflict_count
  FROM appointments
  WHERE doctor_id = p_doctor_id
    AND appointment_date = p_appointment_date
    AND status <> 'cancelled'
    AND deleted_at IS NULL;

  IF v_conflict_count > 0 THEN
    RAISE EXCEPTION 'Doctor % already has an appointment at %', p_doctor_id, p_appointment_date;
  END IF;

  INSERT INTO appointments (patient_id, doctor_id, appointment_date, reason, status)
  VALUES (p_patient_id, p_doctor_id, p_appointment_date, p_reason, 'scheduled');

  COMMIT;
END;
$$;

-- Issues a prescription with multiple line items.
-- Demonstrates: SAVEPOINT per item so one out-of-stock drug doesn't
-- abort the whole prescription — it's skipped and logged via RAISE NOTICE,
-- while the rest of the transaction still COMMITs together.
CREATE OR REPLACE PROCEDURE sp_issue_prescription(
  p_appointment_id INT,
  p_items JSONB  -- array of {"medication_id":.., "dosage":"..", "quantity":.., "instructions":".."}
)
LANGUAGE plpgsql
AS $$
DECLARE
  v_prescription_id INT;
  v_item JSONB;
  v_medication_id INT;
  v_quantity INT;
  v_available INT;
  v_inventory_id INT;
BEGIN
  INSERT INTO prescriptions (appointment_id) VALUES (p_appointment_id)
  RETURNING prescription_id INTO v_prescription_id;

  FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
  LOOP
    v_medication_id := (v_item->>'medication_id')::INT;
    v_quantity := (v_item->>'quantity')::INT;

    SAVEPOINT before_item;

    SELECT inventory_id, quantity_in_stock
      INTO v_inventory_id, v_available
      FROM medical_inventory
      WHERE medication_id = v_medication_id AND deleted_at IS NULL
      ORDER BY expiry_date ASC
      LIMIT 1;

    IF v_available IS NULL OR v_available < v_quantity THEN
      RAISE NOTICE 'Insufficient stock for medication %, skipping this item', v_medication_id;
      ROLLBACK TO SAVEPOINT before_item;
      CONTINUE;
    END IF;

    INSERT INTO prescription_items (prescription_id, medication_id, dosage, quantity, instructions)
    VALUES (v_prescription_id, v_medication_id, v_item->>'dosage', v_quantity, v_item->>'instructions');

    UPDATE medical_inventory
      SET quantity_in_stock = quantity_in_stock - v_quantity
      WHERE inventory_id = v_inventory_id;

    RELEASE SAVEPOINT before_item;
  END LOOP;

  COMMIT;
END;
$$;

-- Computes a billable total for an appointment from its prescriptions
CREATE OR REPLACE FUNCTION fn_calculate_invoice_total(p_appointment_id INT)
RETURNS DECIMAL(10,2)
LANGUAGE plpgsql
AS $$
DECLARE
  v_total DECIMAL(10,2);
BEGIN
  SELECT COALESCE(SUM(pi.quantity * m.unit_price), 0)
  INTO v_total
  FROM prescriptions p
  JOIN prescription_items pi ON pi.prescription_id = p.prescription_id
  JOIN medications m ON m.medication_id = pi.medication_id
  WHERE p.appointment_id = p_appointment_id
    AND p.deleted_at IS NULL;

  RETURN v_total;
END;
$$;

-- Closes a login session cleanly
CREATE OR REPLACE PROCEDURE sp_close_session(p_session_id VARCHAR)
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE user_sessions
  SET logout_at = CURRENT_TIMESTAMP, is_active = FALSE
  WHERE session_id = p_session_id;

  COMMIT;
END;
$$;


-- =====================================================================
-- PART 3 — BUSINESS-LOGIC TRIGGERS
-- (updated_at triggers already exist in hospital_schema.sql — these are
-- additional triggers that enforce domain rules / write audit entries)
-- =====================================================================

-- Logs every appointment status change into audit_log automatically
CREATE OR REPLACE FUNCTION log_appointment_status_change()
RETURNS TRIGGER AS $$
BEGIN
  IF OLD.status IS DISTINCT FROM NEW.status THEN
    INSERT INTO audit_log (user_id, action, table_affected, record_id)
    VALUES (NULL, 'status: ' || OLD.status || ' -> ' || NEW.status, 'appointments', NEW.appointment_id);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_appointment_status
  AFTER UPDATE ON appointments
  FOR EACH ROW EXECUTE FUNCTION log_appointment_status_change();

-- Rejects a booking that conflicts with an existing (non-cancelled) slot
-- for the same doctor — a second layer of protection on top of sp_book_appointment
CREATE OR REPLACE FUNCTION prevent_double_booking()
RETURNS TRIGGER AS $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM appointments
    WHERE doctor_id = NEW.doctor_id
      AND appointment_date = NEW.appointment_date
      AND status <> 'cancelled'
      AND deleted_at IS NULL
      AND appointment_id <> COALESCE(NEW.appointment_id, -1)
  ) THEN
    RAISE EXCEPTION 'Doctor % already booked at %', NEW.doctor_id, NEW.appointment_date;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_double_booking
  BEFORE INSERT OR UPDATE ON appointments
  FOR EACH ROW EXECUTE FUNCTION prevent_double_booking();


-- =====================================================================
-- PART 4 — DCL (GRANT / REVOKE) — database-level RBAC enforcement
-- Mirrors the roles/permissions tables, as defense-in-depth alongside
-- application-level checks.
-- =====================================================================

CREATE ROLE admin_role;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin_role;

CREATE ROLE doctor_role;
GRANT SELECT, INSERT, UPDATE ON
  patients, appointments, medical_records, prescriptions,
  prescription_items, referrals, vital_signs, lab_orders, lab_results
  TO doctor_role;
REVOKE DELETE ON patients, medical_records FROM doctor_role;

CREATE ROLE receptionist_role;
GRANT SELECT, INSERT, UPDATE ON patients, appointments, doctor_schedules, invoices TO receptionist_role;
GRANT SELECT ON medications, medical_inventory TO receptionist_role;
REVOKE DELETE ON ALL TABLES IN SCHEMA public FROM receptionist_role;

CREATE ROLE analyst_role;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analyst_role;
REVOKE INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public FROM analyst_role;

-- =====================================================================
-- EXAMPLE: creating real login accounts and assigning them a role.
-- Replace usernames/passwords with real staff accounts. In PostgreSQL,
-- granting a role to a LOGIN role makes its privileges active
-- automatically on connect (no separate "default role" step needed,
-- unlike MySQL).
-- =====================================================================

-- CREATE ROLE dr_uwase LOGIN PASSWORD 'ChangeMe123!';
-- GRANT doctor_role TO dr_uwase;

-- CREATE ROLE reception1 LOGIN PASSWORD 'ChangeMe123!';
-- GRANT receptionist_role TO reception1;

-- =====================================================================
-- TEMPLATE: creating a brand-new custom role with hand-picked table
-- access. Copy this pattern for any role beyond the four above.
-- =====================================================================

-- CREATE ROLE pharmacist_role;
-- GRANT SELECT, INSERT, UPDATE ON medical_inventory TO pharmacist_role;
-- GRANT SELECT, UPDATE ON prescription_items TO pharmacist_role;
-- GRANT SELECT ON medications TO pharmacist_role;
-- GRANT SELECT ON prescriptions TO pharmacist_role;
-- -- everything else (patients, appointments, users, invoices, ...) stays denied by default

-- =====================================================================
-- End of database objects script.
-- =====================================================================
