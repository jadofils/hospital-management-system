package hospital.management.backend.config.security;

import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.exceptions.UnauthorizedException;
import hospital.management.backend.service.auth.interfaces.RoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionManager/JwtConfig are plain static utilities (no DB, no mocking needed) — each
 * test logs in a real token via SessionManager.login(...) and logs out afterward so no
 * session state leaks into other tests in the same JVM.
 */
@ExtendWith(MockitoExtension.class)
class OwnershipGuardTest {

    @Mock private RoleService roleService;

    @AfterEach
    void logout() {
        SessionManager.logout();
    }

    @Test
    @DisplayName("requireAdminForRoleAssignment passes silently when the session holds the Admin role")
    void requireAdminForRoleAssignment_passes_whenAdmin() {
        String token = JwtConfig.generateToken(UUID.randomUUID().toString(), "admin.user", "Admin");
        SessionManager.login(token);

        assertDoesNotThrow(OwnershipGuard::requireAdminForRoleAssignment);
    }

    @Test
    @DisplayName("requireAdminForRoleAssignment passes when Admin is one of several roles held, not just the primary one")
    void requireAdminForRoleAssignment_passes_whenAdminIsSecondaryRole() {
        String token = JwtConfig.generateToken(UUID.randomUUID().toString(), "multi.role.user",
                "Nurse", List.of("Nurse", "Admin"));
        SessionManager.login(token);

        assertDoesNotThrow(OwnershipGuard::requireAdminForRoleAssignment);
    }

    @Test
    @DisplayName("requireAdminForRoleAssignment throws UnauthorizedException for a non-admin role")
    void requireAdminForRoleAssignment_throws_whenNotAdmin() {
        String token = JwtConfig.generateToken(UUID.randomUUID().toString(), "nurse.user", "Nurse");
        SessionManager.login(token);

        assertThrows(UnauthorizedException.class, OwnershipGuard::requireAdminForRoleAssignment);
    }

    @Test
    @DisplayName("requireAdminForRoleAssignment throws UnauthorizedException when no session is active")
    void requireAdminForRoleAssignment_throws_whenNotLoggedIn() {
        assertThrows(UnauthorizedException.class, OwnershipGuard::requireAdminForRoleAssignment);
    }

    @Test
    @DisplayName("requireNotOwnRole throws UnauthorizedException when the current user holds the role being touched")
    void requireNotOwnRole_throws_whenCurrentUserHoldsRole() throws Exception {
        String userId = UUID.randomUUID().toString();
        String roleId = UUID.randomUUID().toString();
        String token = JwtConfig.generateToken(userId, "admin.user", "Admin");
        SessionManager.login(token);
        org.mockito.Mockito.when(roleService.findRolesForUser(userId))
                .thenReturn(List.of(new RoleDTO(roleId, "Admin", null)));

        assertThrows(UnauthorizedException.class, () -> OwnershipGuard.requireNotOwnRole(roleService, roleId));
    }

    @Test
    @DisplayName("requireNotOwnRole passes when the current user holds a different role")
    void requireNotOwnRole_passes_whenDifferentAdminHoldsAnotherRole() throws Exception {
        String userId = UUID.randomUUID().toString();
        String heldRoleId = UUID.randomUUID().toString();
        String otherRoleId = UUID.randomUUID().toString();
        String token = JwtConfig.generateToken(userId, "admin.user", "Admin");
        SessionManager.login(token);
        org.mockito.Mockito.when(roleService.findRolesForUser(userId))
                .thenReturn(List.of(new RoleDTO(heldRoleId, "Admin", null)));

        assertDoesNotThrow(() -> OwnershipGuard.requireNotOwnRole(roleService, otherRoleId));
    }

    @Test
    @DisplayName("requireNotOwnRole is a no-op when no session is active")
    void requireNotOwnRole_noOp_whenNotLoggedIn() {
        assertDoesNotThrow(() -> OwnershipGuard.requireNotOwnRole(roleService, UUID.randomUUID().toString()));
    }
}
