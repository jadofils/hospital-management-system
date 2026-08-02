package hospital.management.backend.config.security;

import hospital.management.backend.model.enums.RoleName;
import hospital.management.enums.PageRoute;

/**
 * Route/navigation-level RBAC gate. Wires the session's current role (already
 * tracked by {@link SessionManager}) against {@link PageRoute}'s allowed-roles
 * metadata, which previously existed but was never actually checked anywhere.
 *
 * Per-button/per-permission (resource:action) gating is a separate, later concern —
 * this only answers "may the current role navigate to this page at all".
 */
public final class PermissionGate {

    private PermissionGate() {}

    public static RoleName currentRole() {
        return RoleName.fromDbValue(SessionManager.getCurrentRole());
    }

    public static boolean isAllowed(PageRoute route) {
        return route.isAllowedFor(currentRole());
    }
}
