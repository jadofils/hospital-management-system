-- ACID hardening: close two check-then-write races.
-- If either CREATE UNIQUE INDEX fails with a duplicate-key error, existing data
-- already violates the constraint being added — resolve those duplicates first
-- (e.g. soft-delete/merge the extra rows) before re-running this migration.

-- Prevents double-booking the same doctor at the exact same date/time.
CREATE UNIQUE INDEX IF NOT EXISTS uq_appointments_doctor_slot_active
  ON "appointments"("doctor_id", "appointment_date") WHERE "deleted_at" IS NULL;

-- Prevents two concurrent invoice-generation calls for the same appointment
-- from both inserting an invoice.
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoices_appointment_active
  ON "invoices"("appointment_id") WHERE "deleted_at" IS NULL;
