package hospital.management.backend.utils.listeners;

/** All domain events that can be published through the EventBus. */
public enum AppEventType {

    // ── Auth ──────────────────────────────────────────────────────────────────
    USER_LOGGED_IN,
    USER_LOGGED_OUT,
    SESSION_EXPIRED,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    ROLE_CREATED,
    ROLE_UPDATED,
    ROLE_DELETED,
    PERMISSION_CREATED,
    PERMISSION_DELETED,

    // ── Patients ──────────────────────────────────────────────────────────────
    PATIENT_CREATED,
    PATIENT_UPDATED,
    PATIENT_DELETED,
    VITAL_SIGN_RECORDED,
    PATIENT_ALLERGY_ADDED,
    PATIENT_ALLERGY_REMOVED,
    PATIENT_FEEDBACK_SUBMITTED,

    // ── Doctors ───────────────────────────────────────────────────────────────
    DOCTOR_CREATED,
    DOCTOR_UPDATED,
    DOCTOR_DELETED,
    DEPARTMENT_CREATED,
    DEPARTMENT_UPDATED,
    DEPARTMENT_DELETED,
    DOCTOR_SCHEDULE_UPDATED,
    REFERRAL_CREATED,
    REFERRAL_UPDATED,

    // ── Appointments ──────────────────────────────────────────────────────────
    APPOINTMENT_BOOKED,
    APPOINTMENT_UPDATED,
    APPOINTMENT_CANCELLED,
    MEDICAL_RECORD_CREATED,
    MEDICAL_RECORD_UPDATED,

    // ── Billing ───────────────────────────────────────────────────────────────
    INVOICE_CREATED,
    INVOICE_UPDATED,
    INVOICE_PAID,

    // ── Lab ───────────────────────────────────────────────────────────────────
    LAB_ORDER_CREATED,
    LAB_ORDER_UPDATED,
    LAB_RESULT_READY,
    LAB_RESULT_UPDATED,

    // ── Pharmacy ──────────────────────────────────────────────────────────────
    MEDICATION_CREATED,
    MEDICATION_UPDATED,
    INVENTORY_UPDATED,
    INVENTORY_LOW_STOCK,
    PRESCRIPTION_CREATED,
    PRESCRIPTION_UPDATED,

    // ── System ────────────────────────────────────────────────────────────────
    DATA_CLEANING_STARTED,
    DATA_CLEANING_PROGRESS,
    DATA_CLEANING_COMPLETED,
    DATA_CLEANING_FAILED,
    AUDIT_LOG_RECORDED,
    SYSTEM_LOG_RECORDED,
    BACKUP_STARTED,
    BACKUP_PROGRESS,
    BACKUP_COMPLETED,
    BACKUP_FAILED,

    // ── Developer Dashboard (admin-audit) ───────────────────────────────────
    DB_OBJECT_CHANGED,
    MAINTENANCE_ACCESS_CHANGED,
}