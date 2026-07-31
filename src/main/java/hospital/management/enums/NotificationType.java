package hospital.management.enums;

/**
 * Toast / notification severity levels.
 * cssClass is appended to "toast-" to form the CSS modifier class,
 * e.g. NotificationType.SUCCESS → "toast-success".
 */
public enum NotificationType {
    INFO("info",       "Info"),
    SUCCESS("success", "Success"),
    WARNING("warning", "Warning"),
    ERROR("error",     "Error");

    private final String cssClass;
    private final String label;

    NotificationType(String cssClass, String label) {
        this.cssClass = cssClass;
        this.label    = label;
    }

    public String getCssClass() { return cssClass; }
    public String getLabel()    { return label; }

    /** Returns the full CSS modifier class ready to add to a node's style class list. */
    public String toastCssClass() { return "toast-" + cssClass; }

    @Override public String toString() { return cssClass; }
}