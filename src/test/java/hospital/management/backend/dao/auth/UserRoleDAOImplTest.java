package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.model.user.Role;
import hospital.management.backend.model.user.User;
import hospital.management.backend.model.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase) against the
 * `user_roles` join table. Every test inserts a real user + role row first
 * (FK NOT NULL on both columns), since nothing persists between tests.
 */
class UserRoleDAOImplTest extends PostgresIntegrationTestBase {

    private final UserRoleDAOImpl dao = new UserRoleDAOImpl();
    private final UserDAOImpl userDAO = new UserDAOImpl();
    private final RoleDAOImpl roleDAO = new RoleDAOImpl();

    private String userId;
    private String roleId;

    @BeforeEach
    void seedUserAndRole() throws Exception {
        User user = new User();
        user.setUsername("jane.doe");
        user.setPasswordHash("hashed-password");
        user.setEmail("jane.doe@example.com");
        user.setIsActive(true);
        userId = userDAO.save(user).getUserId();

        Role role = new Role();
        role.setRoleName("Nurse");
        roleId = roleDAO.save(role).getRoleId();
    }

    @Test
    @DisplayName("assign creates a user-role link discoverable via findByUserId/findByRoleId/exists")
    void assign_createsLink() throws Exception {
        dao.assign(userId, roleId);

        assertTrue(dao.exists(userId, roleId));
        assertEquals(1, dao.findByUserId(userId).size());
        assertEquals(1, dao.findByRoleId(roleId).size());
    }

    @Test
    @DisplayName("assign via the caller-supplied Connection overload composes into a borrowed connection")
    void assign_viaConnectionOverload() throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            dao.assign(userId, roleId, conn);
        }

        assertTrue(dao.exists(userId, roleId));
    }

    @Test
    @DisplayName("assign fails with a FK violation for a user id that doesn't exist")
    void assign_throwsDatabaseException_whenUserMissing() {
        assertThrows(DatabaseException.class, () -> dao.assign(UUID.randomUUID().toString(), roleId));
    }

    @Test
    @DisplayName("assign fails with a FK violation for a role id that doesn't exist")
    void assign_throwsDatabaseException_whenRoleMissing() {
        assertThrows(DatabaseException.class, () -> dao.assign(userId, UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("revoke marks the link revoked so exists()/findByUserId() no longer surface it")
    void revoke_marksRevoked() throws Exception {
        dao.assign(userId, roleId);

        dao.revoke(userId, roleId);

        assertFalse(dao.exists(userId, roleId));
        assertTrue(dao.findByUserId(userId).isEmpty());
    }

    @Test
    @DisplayName("re-assigning after a revoke clears revoked_at via ON CONFLICT DO UPDATE, rather than failing "
            + "on the composite-PK duplicate key")
    void assign_reAssignsAfterRevoke() throws Exception {
        dao.assign(userId, roleId);
        dao.revoke(userId, roleId);

        assertDoesNotThrow(() -> dao.assign(userId, roleId));
        assertTrue(dao.exists(userId, roleId));
    }

    @Test
    @DisplayName("findByUserId returns the assignment with userId/roleId/assignedAt populated")
    void findByUserId_populatesFields() throws Exception {
        dao.assign(userId, roleId);

        List<UserRole> found = dao.findByUserId(userId);

        assertEquals(1, found.size());
        assertEquals(userId, found.get(0).getUserId());
        assertEquals(roleId, found.get(0).getRoleId());
        assertNotNull(found.get(0).getAssignedAt());
        assertNull(found.get(0).getRevokedAt());
    }

    @Test
    @DisplayName("exists returns false when no assignment has ever been made")
    void exists_returnsFalse_whenNeverAssigned() throws Exception {
        assertFalse(dao.exists(userId, roleId));
    }
}
