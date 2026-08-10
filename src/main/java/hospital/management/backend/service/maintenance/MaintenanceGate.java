package hospital.management.backend.service.maintenance;

import java.util.Locale;

/**
 * Decides whether a just-logged-in user should see the system-status page
 * instead of the normal Dashboard. Named to echo {@code PermissionGate}'s
 * existing role in this codebase — a synchronous, stateless check consulted
 * at exactly one point (right after login), not a scheduled daemon.
 *
 * Admins always bypass — otherwise a global "enabled" maintenance switch
 * would lock out the very person who needs to turn it back off.
 */
public final class MaintenanceGate {

    private MaintenanceGate() {}

    public static boolean isBlocked(String userId, String role) {
        if (isAdmin(role)) return false;
        MaintenanceMode mode = MaintenanceModeStore.load();
        return mode.isEnabled() || mode.isUserBlocked(userId);
    }

    private static boolean isAdmin(String role) {
        if (role == null) return false;
        String normalized = role.trim().toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ');
        return normalized.equals("admin");
    }
}
