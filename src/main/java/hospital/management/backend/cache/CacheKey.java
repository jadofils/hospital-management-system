package hospital.management.backend.cache;

/**
 * Centralised cache key factory.
 *
 * Convention:  {domain}:{id}            — single entity
 *              {domain}:list:{page}:{size} — paginated result set
 *              {domain}:*               — glob pattern for bulk eviction
 *
 * Using constants here means a typo in a key string is caught at compile time,
 * and renaming a key is a single-file change.
 */
public final class CacheKey {

    // ── Auth ──────────────────────────────────────────────────────────────
    public static String user(String userId)               { return "user:" + userId; }
    public static String userByUsername(String username)   { return "user:username:" + username; }
    public static String userList(int page, int size)      { return "user:list:" + page + ":" + size; }
    public static String session(String sessionId)         { return "session:" + sessionId; }
    public static String role(String roleId)               { return "role:" + roleId; }
    public static String roleList()                        { return "role:list"; }
    public static String rolePermissions(String roleId)    { return "role:permissions:" + roleId; }
    public static String userRoles(String userId)          { return "user:roles:" + userId; }
    public static String permission(String permissionId)   { return "permission:" + permissionId; }
    public static String permissionList()                  { return "permission:list"; }

    // ── Department ────────────────────────────────────────────────────────
    public static String department(String id)             { return "department:" + id; }
    public static String departmentList()                  { return "department:list"; }
    public static String doctor(String id)                 { return "doctor:" + id; }
    public static String doctorList(int page, int size)    { return "doctor:list:" + page + ":" + size; }
    public static String doctorsByDept(String deptId)      { return "doctor:dept:" + deptId; }
    public static String doctorSchedule(String doctorId)   { return "schedule:" + doctorId; }
    public static String referral(String id)               { return "referral:" + id; }
    public static String referralsByAppt(String apptId)    { return "referral:appt:" + apptId; }

    // ── Patient ───────────────────────────────────────────────────────────
    public static String patient(String id)                { return "patient:" + id; }
    public static String patientList(int page, int size)   { return "patient:list:" + page + ":" + size; }
    public static String patientSearch(String q, int page) { return "patient:search:" + q.hashCode() + ":" + page; }
    public static String vitals(String apptId)             { return "vital:appt:" + apptId; }
    public static String vitalsByPatient(String patientId) { return "vital:patient:" + patientId; }
    public static String allergies(String patientId)       { return "allergy:" + patientId; }
    public static String feedback(String patientId)        { return "feedback:" + patientId; }
    public static String feedbackList()                    { return "feedback:list"; }

    // ── Clinical ──────────────────────────────────────────────────────────
    public static String appointment(String id)            { return "appointment:" + id; }
    public static String appointmentList(int page, int size){ return "appointment:list:" + page + ":" + size; }
    public static String appointmentsByPatient(String pid) { return "appointment:patient:" + pid; }
    public static String appointmentsByDoctor(String did)  { return "appointment:doctor:" + did; }
    public static String medicalRecord(String id)          { return "record:" + id; }
    public static String recordByAppt(String apptId)       { return "record:appt:" + apptId; }

    // ── Lab ───────────────────────────────────────────────────────────────
    public static String labOrder(String id)               { return "laborder:" + id; }
    public static String labOrdersByAppt(String apptId)    { return "laborder:appt:" + apptId; }
    public static String labResult(String orderId)         { return "labresult:" + orderId; }

    // ── Pharmacy ──────────────────────────────────────────────────────────
    public static String medication(String id)             { return "medication:" + id; }
    public static String medicationList()                  { return "medication:list"; }
    public static String inventory(String medicationId)    { return "inventory:" + medicationId; }
    public static String lowStock()                        { return "inventory:lowstock"; }
    public static String prescription(String id)           { return "prescription:" + id; }
    public static String prescriptionByAppt(String apptId) { return "prescription:appt:" + apptId; }
    public static String prescriptionsByPatient(String pid){ return "prescription:patient:" + pid; }

    // ── Finance ───────────────────────────────────────────────────────────
    public static String invoice(String id)                { return "invoice:" + id; }
    public static String invoiceList(int page, int size)   { return "invoice:list:" + page + ":" + size; }
    public static String invoicesByPatient(String pid)     { return "invoice:patient:" + pid; }

    // ── Eviction patterns (pass to CacheService.evictByPattern) ──────────
    public static final String ALL_USERS        = "user:*";
    public static final String ALL_SESSIONS     = "session:*";
    public static final String ALL_ROLES        = "role:*";
    public static final String ALL_PERMISSIONS  = "permission:*";
    public static final String ALL_DEPARTMENTS  = "department:*";
    public static final String ALL_DOCTORS      = "doctor:*";
    public static final String ALL_PATIENTS     = "patient:*";
    public static final String ALL_FEEDBACK     = "feedback:*";
    public static final String ALL_APPOINTMENTS = "appointment:*";
    public static final String ALL_RECORDS      = "record:*";
    public static final String ALL_LAB          = "lab*";
    public static final String ALL_PHARMACY     = "medication:* inventory:* prescription:*";
    public static final String ALL_INVOICES     = "invoice:*";

    private CacheKey() {}
}