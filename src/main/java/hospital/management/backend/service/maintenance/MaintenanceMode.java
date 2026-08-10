package hospital.management.backend.service.maintenance;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Admin-configurable maintenance settings. Two independent controls, not a
 * single mode enum:
 * <ul>
 *   <li>{@code enabled} — a blanket switch that blocks EVERY non-Admin user.</li>
 *   <li>{@code blockedUserIds} — a standing per-user revoke list, checked
 *       regardless of {@code enabled}, so specific accounts can stay revoked
 *       even while the system is otherwise up (and vice versa).</li>
 * </ul>
 * Distinct from {@code User.isActive} (a permanent, login-time hard block) —
 * this is a softer, temporary, post-login gate. Persisted and loaded by
 * {@link MaintenanceModeStore}.
 */
public final class MaintenanceMode {

    public static final boolean DEFAULT_ENABLED = false;
    public static final SystemStatusPage DEFAULT_STATUS_PAGE = SystemStatusPage.MAINTENANCE;
    public static final String DEFAULT_MESSAGE = "It's not you, it's me.";

    private boolean enabled = DEFAULT_ENABLED;
    private Set<String> blockedUserIds = new LinkedHashSet<>();
    private SystemStatusPage statusPage = DEFAULT_STATUS_PAGE;
    private String message = DEFAULT_MESSAGE;

    public MaintenanceMode() {}

    public boolean isEnabled()                 { return enabled; }
    public Set<String> getBlockedUserIds()     { return blockedUserIds; }
    public SystemStatusPage getStatusPage()    { return statusPage; }
    public String getMessage()                 { return message; }

    public void setEnabled(boolean enabled)                    { this.enabled = enabled; }
    public void setBlockedUserIds(Set<String> blockedUserIds) {
        this.blockedUserIds = blockedUserIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(blockedUserIds);
    }
    public void setStatusPage(SystemStatusPage statusPage) {
        this.statusPage = statusPage == null ? DEFAULT_STATUS_PAGE : statusPage;
    }
    public void setMessage(String message) {
        this.message = (message == null || message.isBlank()) ? DEFAULT_MESSAGE : message;
    }

    public boolean isUserBlocked(String userId) {
        return userId != null && blockedUserIds.contains(userId);
    }

    @Override
    public String toString() {
        return "MaintenanceMode{enabled=" + enabled + ", blockedUsers=" + blockedUserIds.size()
            + ", statusPage=" + statusPage + "}";
    }
}
