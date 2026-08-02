package hospital.management.backend.service.auth;

import hospital.management.backend.dao.auth.interfaces.PermissionDAO;
import hospital.management.backend.dao.auth.interfaces.RoleDAO;
import hospital.management.backend.dao.auth.interfaces.RolePermissionDAO;
import hospital.management.backend.dao.auth.interfaces.UserRoleDAO;
import hospital.management.backend.dto.auth.CreateRoleDTO;
import hospital.management.backend.dto.auth.PermissionDTO;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.model.user.Permission;
import hospital.management.backend.model.user.Role;
import hospital.management.backend.model.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the flow the Users page relies on: creating a brand-new custom role
 * (create), assigning it to a user (assignToUser), and attaching permissions
 * to it (assignPermission/findPermissionsForRole) — the exact path that must
 * work for a freshly-created role to be immediately assignable in the UI.
 * Ids are fresh random UUIDs to avoid the static L1 CacheService leaking
 * cached DTOs across tests (see PatientServiceImplTest for the same rationale).
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock private RoleDAO roleDAO;
    @Mock private UserRoleDAO userRoleDAO;
    @Mock private RolePermissionDAO rolePermissionDAO;
    @Mock private PermissionDAO permissionDAO;

    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleServiceImpl(roleDAO, userRoleDAO, rolePermissionDAO, permissionDAO);
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create throws IllegalArgumentException when roleName is blank")
    void create_throwsIllegalArgumentException_whenRoleNameBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.create(new CreateRoleDTO("  ")));
        verifyNoInteractions(roleDAO);
    }

    @Test
    @DisplayName("create throws ValidationException when a role with the same name already exists")
    void create_throwsValidationException_whenRoleNameTaken() throws Exception {
        when(roleDAO.findByName("Nurse")).thenReturn(Optional.of(new Role("role-1", "Nurse", null, null, null)));

        assertThrows(ValidationException.class, () -> service.create(new CreateRoleDTO("Nurse")));
        verify(roleDAO, never()).save(any());
    }

    @Test
    @DisplayName("create saves a brand-new custom role when the name is unique")
    void create_savesRole_whenNameUnique() throws Exception {
        when(roleDAO.findByName("Nurse")).thenReturn(Optional.empty());
        when(roleDAO.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleDTO result = service.create(new CreateRoleDTO("Nurse"));

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleDAO).save(captor.capture());
        assertEquals("Nurse", captor.getValue().getRoleName());
        assertEquals("Nurse", result.getRoleName());
    }

    // ── findById / findAll ────────────────────────────────────────────────

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the DAO finds nothing")
    void findById_throwsResourceNotFoundException_whenNotFound() throws Exception {
        String id = UUID.randomUUID().toString();
        when(roleDAO.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    @Test
    @DisplayName("findAll maps every DAO role to a DTO, including a freshly-created custom role")
    void findAll_mapsEveryRole() throws Exception {
        when(roleDAO.findAll()).thenReturn(List.of(
                new Role("role-1", "Admin", null, null, null),
                new Role("role-2", "Nurse", null, null, null)));

        List<RoleDTO> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("Nurse", result.get(1).getRoleName());
    }

    // ── assignToUser ──────────────────────────────────────────────────────

    @Test
    @DisplayName("assignToUser throws ResourceNotFoundException when the role doesn't exist")
    void assignToUser_throwsResourceNotFoundException_whenRoleMissing() throws Exception {
        String roleId = UUID.randomUUID().toString();
        when(roleDAO.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.assignToUser("user-1", roleId));
        verify(userRoleDAO, never()).assign(anyString(), anyString());
    }

    @Test
    @DisplayName("assignToUser delegates to the DAO when the role exists")
    void assignToUser_assigns_whenRoleExists() throws Exception {
        String roleId = UUID.randomUUID().toString();
        when(roleDAO.findById(roleId)).thenReturn(Optional.of(new Role(roleId, "Nurse", null, null, null)));

        service.assignToUser("user-1", roleId);

        verify(userRoleDAO).assign("user-1", roleId);
    }

    // ── revokeFromUser ────────────────────────────────────────────────────

    @Test
    @DisplayName("revokeFromUser delegates straight to the DAO")
    void revokeFromUser_delegatesToDao() throws Exception {
        service.revokeFromUser("user-1", "role-1");

        verify(userRoleDAO).revoke("user-1", "role-1");
    }

    // ── findRolesForUser ──────────────────────────────────────────────────

    @Test
    @DisplayName("findRolesForUser silently skips assignments whose role no longer exists")
    void findRolesForUser_skipsMissingRoles() throws Exception {
        when(userRoleDAO.findByUserId("user-1")).thenReturn(List.of(
                new UserRole("user-1", "role-1", null, null, null),
                new UserRole("user-1", "role-missing", null, null, null)));
        when(roleDAO.findById("role-1")).thenReturn(Optional.of(new Role("role-1", "Doctor", null, null, null)));
        when(roleDAO.findById("role-missing")).thenReturn(Optional.empty());

        List<RoleDTO> result = service.findRolesForUser("user-1");

        assertEquals(1, result.size());
        assertEquals("Doctor", result.get(0).getRoleName());
    }

    // ── assignPermission / findPermissionsForRole ────────────────────────

    @Test
    @DisplayName("assignPermission throws ResourceNotFoundException when the permission doesn't exist")
    void assignPermission_throwsResourceNotFoundException_whenPermissionMissing() throws Exception {
        when(permissionDAO.findById("perm-missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.assignPermission("role-1", "perm-missing"));
        verify(rolePermissionDAO, never()).assign(anyString(), anyString());
    }

    @Test
    @DisplayName("assignPermission delegates to the DAO when the permission exists")
    void assignPermission_assigns_whenPermissionExists() throws Exception {
        when(permissionDAO.findById("perm-1"))
                .thenReturn(Optional.of(new Permission("perm-1", "patients", "create", null, null, null)));

        service.assignPermission("role-1", "perm-1");

        verify(rolePermissionDAO).assign("role-1", "perm-1");
    }

    @Test
    @DisplayName("findPermissionsForRole silently skips assignments whose permission no longer exists")
    void findPermissionsForRole_skipsMissingPermissions() throws Exception {
        String roleId = UUID.randomUUID().toString();
        when(rolePermissionDAO.findByRoleId(roleId)).thenReturn(List.of());

        List<PermissionDTO> result = service.findPermissionsForRole(roleId);

        assertTrue(result.isEmpty());
        verifyNoInteractions(permissionDAO);
    }
}
