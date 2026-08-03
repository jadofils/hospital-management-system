package hospital.management.backend.config.security;

import hospital.management.backend.dao.auth.PermissionDAOImpl;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.RolePermissionDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dto.auth.PermissionDTO;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.service.auth.RoleServiceImpl;
import hospital.management.backend.service.auth.interfaces.RoleService;
import hospital.management.enums.PageRoute;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Central route access gate.
 *
 * A page is visible/navigable when the logged-in user has at least one
 * CRUD permission on the route resource mapping.
 *
 * This keeps page access dynamic for custom roles created at runtime,
 * including roles not listed in the RoleName enum.
 *
 * CRUD actions considered: create, read, update, delete (and edit alias).
 */
public final class PermissionGate {

    private static final RoleService ROLE_SERVICE = new RoleServiceImpl(
        new RoleDAOImpl(),
        new UserRoleDAOImpl(),
        new RolePermissionDAOImpl(),
        new PermissionDAOImpl()
    );

    private static final Set<String> CRUD_ACTIONS = Set.of("create", "read", "update", "delete", "edit");

    private static final Map<PageRoute, Set<String>> ROUTE_RESOURCES = new EnumMap<>(PageRoute.class);

    private static String cachedUserId;
    private static Set<String> cachedPermissionKeys = Set.of();

    static {
        ROUTE_RESOURCES.put(PageRoute.PATIENTS, Set.of("patients"));
        ROUTE_RESOURCES.put(PageRoute.PATIENT_DETAIL, Set.of("patients", "patient_allergies", "vital_signs"));
        ROUTE_RESOURCES.put(PageRoute.APPOINTMENTS, Set.of("appointments"));
        ROUTE_RESOURCES.put(PageRoute.DOCTORS, Set.of("doctors"));
        ROUTE_RESOURCES.put(PageRoute.DEPARTMENTS, Set.of("departments"));
        ROUTE_RESOURCES.put(PageRoute.MY_SCHEDULE, Set.of("doctor_schedules"));
        ROUTE_RESOURCES.put(PageRoute.REFERRALS, Set.of("referrals"));
        ROUTE_RESOURCES.put(PageRoute.MEDICAL_RECORDS, Set.of("medical_records"));
        ROUTE_RESOURCES.put(PageRoute.LAB_ORDERS, Set.of("lab_orders", "lab_results"));
        ROUTE_RESOURCES.put(PageRoute.PRESCRIPTIONS, Set.of("prescriptions", "prescription_items"));
        ROUTE_RESOURCES.put(PageRoute.PHARMACY, Set.of("medical_inventory", "medications"));
        ROUTE_RESOURCES.put(PageRoute.BILLING, Set.of("invoices"));
        ROUTE_RESOURCES.put(PageRoute.ANALYTICS, Set.of("patients", "appointments", "invoices", "system_logs", "audit_log"));
        ROUTE_RESOURCES.put(PageRoute.FEEDBACK, Set.of("patient_feedback"));
        ROUTE_RESOURCES.put(PageRoute.USERS, Set.of("users"));
        ROUTE_RESOURCES.put(PageRoute.ROLES, Set.of("roles", "permissions", "user_roles", "role_permissions"));
        ROUTE_RESOURCES.put(PageRoute.SYSTEM_LOGS, Set.of("system_logs"));
        ROUTE_RESOURCES.put(PageRoute.AUDIT_LOGS, Set.of("audit_log"));
        ROUTE_RESOURCES.put(PageRoute.RETENTION, Set.of("user_sessions", "system_logs", "audit_log"));
    }

    private PermissionGate() {}

    public static String currentRole() {
        String role = SessionManager.getCurrentRole();
        return role == null ? null : role.trim();
    }

    public static boolean isAllowed(PageRoute route) {
        if (route == PageRoute.HOME || route == PageRoute.PROFILE) return true;
        if (isAdmin()) return true;

        if (route == PageRoute.DASHBOARD) {
            return isAllowed(PageRoute.PATIENTS)
                || isAllowed(PageRoute.APPOINTMENTS)
                || isAllowed(PageRoute.BILLING)
                || isAllowed(PageRoute.ANALYTICS)
                || isAllowed(PageRoute.PHARMACY)
                || isAllowed(PageRoute.USERS);
        }

        return hasAnyCrudPermission(route);
    }

    public static boolean canRead(PageRoute route) {
        return hasPermission(route, "read");
    }

    public static boolean canCreate(PageRoute route) {
        return hasPermission(route, "create");
    }

    public static boolean canUpdate(PageRoute route) {
        return hasPermission(route, "update");
    }

    public static boolean canDelete(PageRoute route) {
        return hasPermission(route, "delete");
    }

    public static boolean hasPermission(PageRoute route, String action) {
        if (route == PageRoute.HOME || route == PageRoute.PROFILE) return true;
        if (isAdmin()) return true;
        String normalizedAction = normalizeAction(action);
        if (normalizedAction.isBlank()) return false;

        Set<String> resources = ROUTE_RESOURCES.get(route);
        if (resources == null || resources.isEmpty()) return true;

        Set<String> granted = currentPermissionKeys();
        for (String resource : resources) {
            String normalizedResource = resource.toLowerCase(Locale.ROOT);
            if (granted.contains(normalizedResource + ":" + normalizedAction)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void invalidateCache() {
        cachedUserId = null;
        cachedPermissionKeys = Set.of();
    }

    private static boolean hasAnyCrudPermission(PageRoute route) {
        Set<String> resources = ROUTE_RESOURCES.get(route);
        if (resources == null || resources.isEmpty()) return true;

        Set<String> granted = currentPermissionKeys();
        for (String resource : resources) {
            String normalizedResource = resource.toLowerCase(Locale.ROOT);
            for (String action : CRUD_ACTIONS) {
                String normalizedAction = normalizeAction(action);
                if (granted.contains(normalizedResource + ":" + normalizedAction)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static synchronized Set<String> currentPermissionKeys() {
        try {
            String userId = SessionManager.getCurrentUserId();
            if (userId != null && userId.equals(cachedUserId)) {
                return cachedPermissionKeys;
            }

            Set<String> keys = new HashSet<>();
            List<RoleDTO> roles = ROLE_SERVICE.findRolesForUser(userId);
            for (RoleDTO role : roles) {
                List<PermissionDTO> permissions = ROLE_SERVICE.findPermissionsForRole(role.getRoleId());
                for (PermissionDTO permission : permissions) {
                    String resource = safe(permission.getResource());
                    String action = normalizeAction(permission.getAction());
                    if (!resource.isBlank() && !action.isBlank()) {
                        keys.add(resource + ":" + action);
                    }
                }
            }

            cachedUserId = userId;
            cachedPermissionKeys = Set.copyOf(keys);
            return cachedPermissionKeys;
        } catch (Exception e) {
            return Set.of();
        }
    }

    private static String normalizeAction(String action) {
        String value = safe(action);
        return "edit".equals(value) ? "update" : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isAdmin() {
        String role = currentRole();
        return role != null && "admin".equalsIgnoreCase(role);
    }
}
