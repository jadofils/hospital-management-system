package hospital.management.backend.dto.notification;

/**
 * String constants for the {@code notifications.type} free-text column.
 * Shared by the producer ({@code NotificationEventListener}) and any UI code
 * that renders a notification's type into human-readable text, so both sides
 * agree on the vocabulary.
 */
public final class NotificationTopics {

    public static final String APPOINTMENT_CREATED   = "appointment_created";
    public static final String APPOINTMENT_UPDATED    = "appointment_updated";
    public static final String APPOINTMENT_CANCELLED = "appointment_cancelled";
    public static final String PRESCRIPTION_CREATED   = "prescription_created";
    public static final String LAB_RESULT_READY       = "lab_result_ready";

    // ── Single-recipient (doctor) topics ────────────────────────────────────
    public static final String MEDICAL_RECORD_CREATED = "medical_record_created";
    public static final String MEDICAL_RECORD_UPDATED = "medical_record_updated";
    public static final String INVOICE_CREATED         = "invoice_created";
    public static final String INVOICE_UPDATED         = "invoice_updated";
    public static final String INVOICE_PAID            = "invoice_paid";
    public static final String LAB_ORDER_CREATED       = "lab_order_created";
    public static final String LAB_ORDER_UPDATED       = "lab_order_updated";
    public static final String VITAL_SIGN_RECORDED     = "vital_sign_recorded";
    public static final String PATIENT_FEEDBACK_SUBMITTED = "patient_feedback_submitted";
    public static final String REFERRAL_CREATED        = "referral_created";
    public static final String REFERRAL_UPDATED        = "referral_updated";
    public static final String DOCTOR_UPDATED          = "doctor_updated";
    public static final String DOCTOR_DELETED          = "doctor_deleted";
    public static final String DOCTOR_SCHEDULE_UPDATED = "doctor_schedule_updated";

    // ── Role-broadcast topics ────────────────────────────────────────────────
    public static final String PATIENT_CREATED         = "patient_created";
    public static final String PATIENT_UPDATED         = "patient_updated";
    public static final String PATIENT_DELETED         = "patient_deleted";
    public static final String PATIENT_ALLERGY_ADDED   = "patient_allergy_added";
    public static final String PATIENT_ALLERGY_REMOVED = "patient_allergy_removed";
    public static final String MEDICATION_CREATED      = "medication_created";
    public static final String INVENTORY_UPDATED       = "inventory_updated";
    public static final String INVENTORY_LOW_STOCK     = "inventory_low_stock";
    public static final String USER_CREATED            = "user_created";
    public static final String USER_UPDATED            = "user_updated";
    public static final String USER_DELETED            = "user_deleted";
    public static final String ROLE_CREATED            = "role_created";
    public static final String ROLE_UPDATED            = "role_updated";
    public static final String ROLE_DELETED            = "role_deleted";
    public static final String PERMISSION_CREATED      = "permission_created";
    public static final String PERMISSION_DELETED      = "permission_deleted";
    public static final String DEPARTMENT_CREATED      = "department_created";
    public static final String DEPARTMENT_UPDATED      = "department_updated";
    public static final String DEPARTMENT_DELETED      = "department_deleted";

    // ── Developer Dashboard (admin-audit — self-notifying, unlike the role-broadcast
    //     topics above: an admin performing one of these wants their own confirmation) ──
    public static final String DB_OBJECT_CHANGED         = "db_object_changed";
    public static final String MAINTENANCE_ACCESS_CHANGED = "maintenance_access_changed";
    public static final String BACKUP_COMPLETED           = "backup_completed";
    public static final String BACKUP_FAILED              = "backup_failed";

    private NotificationTopics() {}
}
