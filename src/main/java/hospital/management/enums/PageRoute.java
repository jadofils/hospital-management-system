package hospital.management.enums;

import hospital.management.backend.model.enums.RoleName;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Every navigable page in the application.
 *
 * allowedRoles — which roles may see/navigate to this page.
 *               Empty set means no restriction (all roles allowed).
 */
public enum PageRoute {

    // ── Landing / sign-in ─────────────────────────────────────────────────
    HOME("home", "Home",
         "/hospital/management/frontend/pages/home-page.fxml"),

    // ── Common (all authenticated roles) ──────────────────────────────────
    DASHBOARD("dashboard", "Dashboard",
              "/hospital/management/frontend/pages/dashboard.fxml"),

    PROFILE("profile", "My Profile",
            "/hospital/management/frontend/pages/profile-page.fxml"),

    // ── Patient domain ────────────────────────────────────────────────────
    PATIENTS("patients", "Patients",
             "/hospital/management/frontend/pages/patients-page.fxml",
             RoleName.ADMIN, RoleName.DOCTOR, RoleName.RECEPTIONIST),

    PATIENT_DETAIL("patient-detail", "Patient Detail",
                   "/hospital/management/frontend/pages/patient-detail-page.fxml",
                   RoleName.ADMIN, RoleName.DOCTOR, RoleName.RECEPTIONIST),

    // ── Appointment domain ────────────────────────────────────────────────
    APPOINTMENTS("appointments", "Appointments",
                 "/hospital/management/frontend/pages/appointments-page.fxml",
                 RoleName.ADMIN, RoleName.DOCTOR, RoleName.RECEPTIONIST),

    // ── Doctor / Department ───────────────────────────────────────────────
    DOCTORS("doctors", "Doctors",
            "/hospital/management/frontend/pages/doctors-page.fxml",
            RoleName.ADMIN, RoleName.RECEPTIONIST),

    DEPARTMENTS("departments", "Departments",
                "/hospital/management/frontend/pages/departments-page.fxml",
                RoleName.ADMIN),

    MY_SCHEDULE("my-schedule", "My Schedule",
                "/hospital/management/frontend/pages/schedule-page.fxml",
                RoleName.DOCTOR),

    REFERRALS("referrals", "Referrals",
              "/hospital/management/frontend/pages/referrals-page.fxml",
              RoleName.DOCTOR, RoleName.ADMIN),

    // ── Clinical ──────────────────────────────────────────────────────────
    MEDICAL_RECORDS("medical-records", "Medical Records",
                    "/hospital/management/frontend/pages/medical-records-page.fxml",
                    RoleName.DOCTOR, RoleName.ADMIN),

    LAB_ORDERS("lab-orders", "Lab Orders",
               "/hospital/management/frontend/pages/lab-orders-page.fxml",
               RoleName.DOCTOR, RoleName.ADMIN),

    // ── Pharmacy ──────────────────────────────────────────────────────────
    PRESCRIPTIONS("prescriptions", "Prescriptions",
                  "/hospital/management/frontend/pages/prescriptions-page.fxml",
                  RoleName.DOCTOR, RoleName.PHARMACIST, RoleName.ADMIN),

    PHARMACY("pharmacy", "Pharmacy Inventory",
             "/hospital/management/frontend/pages/pharmacy-page.fxml",
             RoleName.PHARMACIST, RoleName.ADMIN),

    // ── Finance ───────────────────────────────────────────────────────────
    BILLING("billing", "Billing",
            "/hospital/management/frontend/pages/billing-page.fxml",
            RoleName.ADMIN, RoleName.RECEPTIONIST),

    // ── Analytics ─────────────────────────────────────────────────────────
    ANALYTICS("analytics", "Analytics",
              "/hospital/management/frontend/pages/analytics-page.fxml",
              RoleName.ADMIN, RoleName.ANALYST),

    FEEDBACK("feedback", "Patient Feedback",
             "/hospital/management/frontend/pages/feedback-page.fxml",
             RoleName.ANALYST, RoleName.ADMIN),

    // ── Admin ─────────────────────────────────────────────────────────────
    USERS("users", "User Management",
          "/hospital/management/frontend/pages/users-page.fxml",
          RoleName.ADMIN),

    ROLES("roles", "Roles & Permissions",
          "/hospital/management/frontend/pages/roles-page.fxml",
          RoleName.ADMIN),

    SYSTEM_LOGS("system-logs", "System Logs",
                "/hospital/management/frontend/pages/system-logs-page.fxml",
                RoleName.ADMIN),

    AUDIT_LOGS("audit-logs", "Audit Logs",
               "/hospital/management/frontend/pages/audit-logs-page.fxml",
               RoleName.ADMIN),

    RETENTION("retention", "Data Retention",
              "/hospital/management/frontend/pages/retention-settings.fxml",
              RoleName.ADMIN),

    DEVELOPER_DASHBOARD("developer-dashboard", "Developer Dashboard",
                        "/hospital/management/frontend/pages/developer-dashboard.fxml",
                        RoleName.ADMIN),

    // ── System status (post-login maintenance gate) ─────────────────────────
    // No role restriction — a blocked non-admin user must be able to reach it.
    SYSTEM_STATUS("system-status", "System Status",
                  "/hospital/management/frontend/pages/system-status-page.fxml");

    // ─────────────────────────────────────────────────────────────────────

    private final String         key;
    private final String         label;
    private final String         fxmlPath;
    private final Set<RoleName>  allowedRoles;

    PageRoute(String key, String label, String fxmlPath, RoleName... roles) {
        this.key          = key;
        this.label        = label;
        this.fxmlPath     = fxmlPath;
        this.allowedRoles = roles.length == 0
                ? EnumSet.noneOf(RoleName.class)
                : EnumSet.copyOf(Arrays.asList(roles));
    }

    public String        getKey()          { return key; }
    public String        getLabel()        { return label; }
    public String        getFxmlPath()     { return fxmlPath; }
    public Set<RoleName> getAllowedRoles()  { return allowedRoles; }

    /** Returns true if this page is visible to the given role (or has no restriction). */
    public boolean isAllowedFor(RoleName role) {
        return allowedRoles.isEmpty() || allowedRoles.contains(role);
    }

    public static PageRoute fromKey(String key) {
        for (PageRoute r : values()) {
            if (r.key.equalsIgnoreCase(key)) return r;
        }
        throw new IllegalArgumentException("Unknown PageRoute key: " + key);
    }

    /**
     * The page a user is most likely to need next, following the data-dependency
     * workflow (e.g. after creating a patient, book their appointment; after an
     * appointment, process its billing). Drives the "Continue to →" guide button
     * shown on the right end of each page. Returns {@code null} for terminal pages
     * that have no sensible next step.
     */
    public PageRoute getNextStep() {
        return switch (this) {
            case DASHBOARD                      -> PATIENTS;
            case PATIENTS, PATIENT_DETAIL       -> APPOINTMENTS;
            case APPOINTMENTS                   -> BILLING;
            case DEPARTMENTS                    -> DOCTORS;
            case DOCTORS, MY_SCHEDULE           -> APPOINTMENTS;
            case REFERRALS                      -> APPOINTMENTS;
            case MEDICAL_RECORDS                -> PRESCRIPTIONS;
            case LAB_ORDERS                     -> MEDICAL_RECORDS;
            case PRESCRIPTIONS                  -> PHARMACY;
            case PHARMACY                       -> PRESCRIPTIONS;
            case BILLING                        -> DASHBOARD;
            case FEEDBACK, ANALYTICS            -> DASHBOARD;
            case USERS                          -> ROLES;
            case ROLES                          -> USERS;
            case SYSTEM_LOGS                    -> AUDIT_LOGS;
            case AUDIT_LOGS                     -> RETENTION;
            case RETENTION                      -> SYSTEM_LOGS;
            case DEVELOPER_DASHBOARD, PROFILE   -> DASHBOARD;
            case HOME, SYSTEM_STATUS            -> null;
        };
    }

    @Override public String toString() { return key; }
}