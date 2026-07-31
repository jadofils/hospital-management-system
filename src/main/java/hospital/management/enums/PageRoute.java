package hospital.management.enums;

/**
 * Every navigable page in the application.
 * key      — matches the string passed to SidebarController.setActiveItem()
 * label    — human-readable page title
 * fxmlPath — classpath-relative path passed to FXMLLoader.getResource()
 */
public enum PageRoute {
    DASHBOARD(
        "dashboard",
        "Dashboard",
        "/hospital/management/frontend/pages/dashboard.fxml"
    ),
    PATIENTS(
        "patients",
        "Patients",
        "/hospital/management/frontend/pages/patients-page.fxml"
    ),
    APPOINTMENTS(
        "appointments",
        "Appointments",
        "/hospital/management/frontend/pages/appointments-page.fxml"
    ),
    BILLING(
        "billing",
        "Billing",
        "/hospital/management/frontend/pages/billing-page.fxml"
    ),
    AUTH(
        "auth",
        "Login",
        "/hospital/management/frontend/pages/auth-pages.fxml"
    );

    private final String key;
    private final String label;
    private final String fxmlPath;

    PageRoute(String key, String label, String fxmlPath) {
        this.key      = key;
        this.label    = label;
        this.fxmlPath = fxmlPath;
    }

    public String getKey()      { return key; }
    public String getLabel()    { return label; }
    public String getFxmlPath() { return fxmlPath; }

    public static PageRoute fromKey(String key) {
        for (PageRoute r : values()) {
            if (r.key.equalsIgnoreCase(key)) return r;
        }
        throw new IllegalArgumentException("Unknown PageRoute key: " + key);
    }

    @Override public String toString() { return key; }
}