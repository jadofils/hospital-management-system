package hospital.management.backend.utils.listeners;

/** All domain events that can be published through the EventBus. */
public enum AppEventType {

    // ── Auth ──────────────────────────────────────────────────────────────────
    USER_LOGGED_IN,
    USER_LOGGED_OUT,
    SESSION_EXPIRED,

    // ── Patients ──────────────────────────────────────────────────────────────
    PATIENT_CREATED,
    PATIENT_UPDATED,
    PATIENT_DELETED,

    // ── Doctors ───────────────────────────────────────────────────────────────
    DOCTOR_CREATED,
    DOCTOR_UPDATED,
    DOCTOR_DELETED,

    // ── Appointments ──────────────────────────────────────────────────────────
    APPOINTMENT_BOOKED,
    APPOINTMENT_UPDATED,
    APPOINTMENT_CANCELLED,

    // ── Billing ───────────────────────────────────────────────────────────────
    INVOICE_CREATED,
    INVOICE_PAID,

    // ── Lab ───────────────────────────────────────────────────────────────────
    LAB_ORDER_CREATED,
    LAB_RESULT_READY,

    // ── System ────────────────────────────────────────────────────────────────
    DATA_CLEANING_STARTED,
    DATA_CLEANING_PROGRESS,
    DATA_CLEANING_COMPLETED,
    DATA_CLEANING_FAILED,
}