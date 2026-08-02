package hospital.management.backend.dao.auth;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.model.user.User;
import hospital.management.backend.model.user.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase) against the
 * `user_sessions` table. Every test inserts a real user row first (FK user_id is
 * NOT NULL with ON DELETE CASCADE), since nothing persists between tests.
 */
class UserSessionDAOImplTest extends PostgresIntegrationTestBase {

    private final UserSessionDAOImpl dao = new UserSessionDAOImpl();
    private final UserDAOImpl userDAO = new UserDAOImpl();

    private String userId;

    @BeforeEach
    void seedUser() throws Exception {
        User user = new User();
        user.setUsername("jane.doe");
        user.setPasswordHash("hashed-password");
        user.setEmail("jane.doe@example.com");
        user.setIsActive(true);
        userId = userDAO.save(user).getUserId();
    }

    private UserSession sampleSession() {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        session.setIpAddress("127.0.0.1");
        session.setUserAgent("JUnit");
        session.setIsActive(true);
        return session;
    }

    @Test
    @DisplayName("save assigns a generated id and populates login_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        UserSession saved = dao.save(sampleSession());

        assertNotNull(saved.getSessionId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getSessionId()));
        assertNotNull(saved.getLoginAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save via the caller-supplied Connection overload composes into a borrowed connection")
    void save_viaConnectionOverload() throws Exception {
        UserSession session = sampleSession();
        try (Connection conn = DBConnection.getConnection()) {
            dao.save(session, conn);
        }

        assertNotNull(session.getSessionId());
        assertTrue(dao.findById(session.getSessionId()).isPresent());
    }

    @Test
    @DisplayName("save fails with a FK violation for a user id that doesn't exist")
    void save_throwsDatabaseException_whenUserMissing() {
        UserSession session = sampleSession();
        session.setUserId(UUID.randomUUID().toString());

        assertThrows(DatabaseException.class, () -> dao.save(session));
    }

    @Test
    @DisplayName("findById returns the saved session with every field intact")
    void findById_returnsSavedSession() throws Exception {
        UserSession saved = dao.save(sampleSession());

        Optional<UserSession> found = dao.findById(saved.getSessionId());

        assertTrue(found.isPresent());
        assertEquals(userId, found.get().getUserId());
        assertEquals("127.0.0.1", found.get().getIpAddress());
        assertEquals("JUnit", found.get().getUserAgent());
        assertTrue(found.get().isIsActive());
        assertNull(found.get().getLogoutAt());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findActiveByUserId returns only sessions still marked active, most-recent login first")
    void findActiveByUserId_returnsOnlyActiveSessions() throws Exception {
        UserSession first = dao.save(sampleSession());
        UserSession second = dao.save(sampleSession());
        dao.deactivate(first.getSessionId());

        List<UserSession> active = dao.findActiveByUserId(userId);

        assertEquals(1, active.size());
        assertEquals(second.getSessionId(), active.get(0).getSessionId());
    }

    @Test
    @DisplayName("deactivate flips is_active to false and stamps logout_at")
    void deactivate_setsInactiveAndLogoutAt() throws Exception {
        UserSession saved = dao.save(sampleSession());

        dao.deactivate(saved.getSessionId());

        Optional<UserSession> reloaded = dao.findById(saved.getSessionId());
        assertFalse(reloaded.get().isIsActive());
        assertNotNull(reloaded.get().getLogoutAt());
    }

    @Test
    @DisplayName("deactivate via the caller-supplied Connection overload composes into a borrowed connection")
    void deactivate_viaConnectionOverload() throws Exception {
        UserSession saved = dao.save(sampleSession());

        try (Connection conn = DBConnection.getConnection()) {
            dao.deactivate(saved.getSessionId(), conn);
        }

        assertFalse(dao.findById(saved.getSessionId()).get().isIsActive());
    }

    @Test
    @DisplayName("deactivateAll closes every active session for the user, leaving other users' sessions untouched")
    void deactivateAll_deactivatesEveryActiveSessionForUser() throws Exception {
        dao.save(sampleSession());
        dao.save(sampleSession());

        User other = new User();
        other.setUsername("john.smith");
        other.setPasswordHash("hashed-password");
        other.setEmail("john.smith@example.com");
        other.setIsActive(true);
        String otherUserId = userDAO.save(other).getUserId();
        UserSession otherSession = sampleSession();
        otherSession.setUserId(otherUserId);
        UserSession otherSaved = dao.save(otherSession);

        dao.deactivateAll(userId);

        assertTrue(dao.findActiveByUserId(userId).isEmpty());
        assertTrue(dao.findById(otherSaved.getSessionId()).get().isIsActive());
    }

    @Test
    @DisplayName("deactivateAll via the caller-supplied Connection overload composes into a borrowed connection")
    void deactivateAll_viaConnectionOverload() throws Exception {
        dao.save(sampleSession());

        try (Connection conn = DBConnection.getConnection()) {
            dao.deactivateAll(userId, conn);
        }

        assertTrue(dao.findActiveByUserId(userId).isEmpty());
    }
}
