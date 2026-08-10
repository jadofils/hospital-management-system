package hospital.management.backend.daemon;

/**
 * FULL backs up every PostgreSQL table plus every MongoDB collection.
 * PARTIAL backs up only patient-critical data: patients, medical_records,
 * prescriptions, lab_orders, invoices (Postgres) + patient_notes,
 * notifications (Mongo) — faster and smaller, meant for frequent automated
 * runs.
 */
public enum BackupType { FULL, PARTIAL }
