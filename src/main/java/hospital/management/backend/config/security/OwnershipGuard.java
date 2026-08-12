package hospital.management.backend.config.security;

import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.exceptions.UnauthorizedException;
import hospital.management.backend.service.auth.interfaces.RoleService;

/**
 * Record-level authorization checks that go beyond {@link PermissionGate}'s generic
 * resource:action CRUD permissions.
 *
 * A user granted a generic "users:update" or "roles:update" permission (e.g. a custom
 * "HR Manager" role) is meant to manage user/role records — not to grant themselves or
 * anyone else the Admin role, which is a privilege-escalation path a route-level CRUD
 * check alone can't close. Role/permission assignment is therefore always admin-gated,
 * regardless of which generic permissions the acting user's own role carries.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {}

    /** Call before assigning/revoking a role on a user, or a permission on a role. */
    public static void requireAdminForRoleAssignment() {
        if (!PermissionGate.isCurrentUserAdmin()) {
            throw new UnauthorizedException("Only administrators can assign or revoke roles and permissions.");
        }
    }

    /**
     * Call before deleting a role, or before mutating a role's own permission set — never
     * before assigning/revoking a role ON A USER, which is a different action already
     * guarded by {@link #requireAdminForRoleAssignment()}.
     *
     * Nobody — not even an admin — may delete or edit the exact role they themselves
     * currently hold. This forces a different admin (or the same admin after switching to
     * a different role) to make that specific change, so nobody can accidentally lock
     * themselves out or gut their own access.
     */
    public static void requireNotOwnRole(RoleService roleService, String roleId) throws Exception {
        if (roleId == null || !SessionManager.isLoggedIn()) return;
        String currentUserId = SessionManager.getCurrentUserId();

        boolean holdsRole = roleService.findRolesForUser(currentUserId).stream()
                .map(RoleDTO::getRoleId)
                .anyMatch(roleId::equals);
        if (holdsRole) {
            throw new UnauthorizedException(
                    "You cannot delete or modify a role you currently hold. Ask another administrator to make this change.");
        }
    }
}
