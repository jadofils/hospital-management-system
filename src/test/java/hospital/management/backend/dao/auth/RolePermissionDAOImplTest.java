package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.model.user.Permission;
import hospital.management.backend.model.user.Role;
import hospital.management.backend.model.user.RolePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase) against the
 * `role_permissions` join table. Every test inserts a real role + permission row first
 * (FK NOT NULL on both columns), since nothing persists between tests.
 */
class RolePermissionDAOImplTest extends PostgresIntegrationTestBase {

    private final RolePermissionDAOImpl dao = new RolePermissionDAOImpl();
    private final RoleDAOImpl roleDAO = new RoleDAOImpl();
    private final PermissionDAOImpl permissionDAO = new PermissionDAOImpl();

    private String roleId;
    private String permissionId;

    @BeforeEach
    void seedRoleAndPermission() throws Exception {
        Role role = new Role();
        role.setRoleName("Nurse");
        roleId = roleDAO.save(role).getRoleId();

        Permission permission = new Permission();
        permission.setResource("patients");
        permission.setAction("create");
        permissionId = permissionDAO.save(permission).getPermissionId();
    }

    @Test
    @DisplayName("assign creates a role-permission link discoverable via findByRoleId/findByPermissionId/exists")
    void assign_createsLink() throws Exception {
        dao.assign(roleId, permissionId);

        assertTrue(dao.exists(roleId, permissionId));
        assertEquals(1, dao.findByRoleId(roleId).size());
        assertEquals(1, dao.findByPermissionId(permissionId).size());
    }

    @Test
    @DisplayName("assign via the caller-supplied Connection overload composes into a borrowed connection")
    void assign_viaConnectionOverload() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            dao.assign(roleId, permissionId, conn);
        }

        assertTrue(dao.exists(roleId, permissionId));
    }

    @Test
    @DisplayName("assign fails with a FK violation for a role id that doesn't exist")
    void assign_throwsDatabaseException_whenRoleMissing() {
        assertThrows(DatabaseException.class, () -> dao.assign(UUID.randomUUID().toString(), permissionId));
    }

    @Test
    @DisplayName("assign fails with a FK violation for a permission id that doesn't exist")
    void assign_throwsDatabaseException_whenPermissionMissing() {
        assertThrows(DatabaseException.class, () -> dao.assign(roleId, UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("revoke marks the link deleted so exists()/findByRoleId() no longer surface it")
    void revoke_marksDeleted() throws Exception {
        dao.assign(roleId, permissionId);

        dao.revoke(roleId, permissionId);

        assertFalse(dao.exists(roleId, permissionId));
        assertTrue(dao.findByRoleId(roleId).isEmpty());
    }

    @Test
    @DisplayName("re-assigning after a revoke clears deleted_at via ON CONFLICT DO UPDATE, rather than failing "
            + "on the composite-PK duplicate key")
    void assign_reAssignsAfterRevoke() throws Exception {
        dao.assign(roleId, permissionId);
        dao.revoke(roleId, permissionId);

        assertDoesNotThrow(() -> dao.assign(roleId, permissionId));
        assertTrue(dao.exists(roleId, permissionId));
    }

    @Test
    @DisplayName("findByRoleId returns the assignment with roleId/permissionId populated")
    void findByRoleId_populatesFields() throws Exception {
        dao.assign(roleId, permissionId);

        List<RolePermission> found = dao.findByRoleId(roleId);

        assertEquals(1, found.size());
        assertEquals(roleId, found.get(0).getRoleId());
        assertEquals(permissionId, found.get(0).getPermissionId());
        assertNotNull(found.get(0).getCreatedAt());
    }

    @Test
    @DisplayName("exists returns false when no assignment has ever been made")
    void exists_returnsFalse_whenNeverAssigned() throws Exception {
        assertFalse(dao.exists(roleId, permissionId));
    }
}
