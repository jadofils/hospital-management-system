package hospital.management.backend.config.security;

import hospital.management.backend.exceptions.ForbiddenException;
import hospital.management.backend.model.enums.RoleName;

/**
 * Role-based access control (RBAC) guard for the current session.
 *
 * Instead of writing `if (role.equals("admin"))` in every controller,
 * call AccessControl.require(...) once. If the check fails it throws
 * ForbiddenException with a clear message — no string literals scattered
 * across the UI layer.
 *
 * Usage:
 *   AccessControl.require(RoleName.ADMIN);                      // single role
 *   AccessControl.require(RoleName.ADMIN, RoleName.DOCTOR);     // any of these
 *   AccessControl.hasRole(RoleName.PHARMACIST);                 // boolean check
 */
public final class AccessControl {

    private AccessControl() {}

    // ── Role checks ───────────────────────────────────────────────────────────

    /**
     * Throws {@link ForbiddenException} if the current session's role is not
     * in the list of allowed roles.
     *
     * @param allowed one or more roles permitted to perform the action
     * @throws hospital.management.backend.exceptions.UnauthorizedException if not logged in
     * @throws ForbiddenException if logged in but role is not in the allowed list
     */
    public static void require(RoleName... allowed) {
        String role = SessionManager.getCurrentRole();
        for (RoleName r : allowed) {
            if (r.getDbValue().equalsIgnoreCase(role)) return;
        }
        String needed = rolesToString(allowed);
        throw new ForbiddenException(needed, role);
    }

    /**
     * Returns true if the current session's role matches any of the given roles.
     * Does NOT throw — use this for conditional UI (show/hide buttons, disable fields).
     *
     * @throws hospital.management.backend.exceptions.UnauthorizedException if not logged in
     */
    public static boolean hasRole(RoleName... roles) {
        String current = SessionManager.getCurrentRole();
        for (RoleName r : roles) {
            if (r.getDbValue().equalsIgnoreCase(current)) return true;
        }
        return false;
    }

    /**
     * Returns true if the current session belongs to an admin.
     * Convenience wrapper for the most common check.
     */
    public static boolean isAdmin() {
        return hasRole(RoleName.ADMIN);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String rolesToString(RoleName[] roles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roles.length; i++) {
            if (i > 0) sb.append(" or ");
            sb.append(roles[i].getDbValue());
        }
        return sb.toString();
    }
}