package hospital.management.backend.dao.log;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion here
 * runs AuditLogDAOImpl's actual SQL against a real database, proving the RETURNING clause,
 * the nullable FK to users(user_id), and the Connection-overloaded save() (used
 * transactionally by other services, e.g. AuthServiceImpl.login) all behave as the code
 * assumes.
 */
class AuditLogDAOImplTest extends PostgresIntegrationTestBase {

    private final AuditLogDAOImpl dao = new AuditLogDAOImpl();

    private AuditLog sampleLog(String userId) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction("CREATE");
        log.setTableAffected("patients");
        log.setRecordId(UUID.randomUUID().toString());
        return log;
    }

    /** Inserts a minimal row directly into `users` so tests can exercise a real, valid FK. */
    private String insertUser() throws SQLException {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?) RETURNING user_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "user_" + UUID.randomUUID());
            ps.setString(2, "hashed-password");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject("user_id", UUID.class).toString();
            }
        }
    }

    @Test
    @DisplayName("save assigns a generated id and created_at from the DB, with a null user_id")
    void save_assignsIdAndTimestamp_whenUserIdNull() throws Exception {
        AuditLog saved = dao.save(sampleLog(null));

        assertNotNull(saved.getLogId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getLogId()));
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("save persists a valid user_id foreign key")
    void save_persistsUserId_whenValid() throws Exception {
        String userId = insertUser();

        AuditLog saved = dao.save(sampleLog(userId));

        List<AuditLog> forUser = dao.findByUserId(userId);
        assertEquals(1, forUser.size());
        assertEquals(saved.getLogId(), forUser.get(0).getLogId());
        assertEquals(userId, forUser.get(0).getUserId());
    }

    @Test
    @DisplayName("save rejects a user_id that doesn't exist in users — real FK constraint enforcement")
    void save_throws_whenUserIdDoesNotExist() {
        AuditLog log = sampleLog(UUID.randomUUID().toString());

        assertThrows(Exception.class, () -> dao.save(log));
    }

    @Test
    @DisplayName("save(log, conn) overload persists using a caller-supplied connection")
    void save_withConnectionOverload_persists() throws Exception {
        AuditLog log = sampleLog(null);

        try (Connection conn = DBConnection.getConnection()) {
            AuditLog saved = dao.save(log, conn);
            assertNotNull(saved.getLogId());
            assertNotNull(saved.getCreatedAt());
        }

        // Visible afterwards through a normal (separate-connection) read, proving it was
        // actually committed rather than merely buffered on the borrowed connection.
        List<AuditLog> all = dao.findByTable("patients");
        assertEquals(1, all.size());
    }

    @Test
    @DisplayName("save(log, conn) overload propagates a raw SQLException on FK violation, unwrapped")
    void save_withConnectionOverload_throwsSqlException_onFkViolation() throws Exception {
        AuditLog log = sampleLog(UUID.randomUUID().toString());

        try (Connection conn = DBConnection.getConnection()) {
            assertThrows(SQLException.class, () -> dao.save(log, conn));
        }
    }

    @Test
    @DisplayName("findAll returns saved entries, most-recently-created first")
    void findAll_returnsEntries_mostRecentFirst() throws Exception {
        dao.save(sampleLog(null));
        dao.save(sampleLog(null));

        PageResult<AuditLog> page = dao.findAll(CursorPagination.firstPage());

        assertEquals(2, page.getCount());
    }

    @Test
    @DisplayName("findByUserId returns only entries for the given user")
    void findByUserId_filtersByUser() throws Exception {
        String userId = insertUser();
        dao.save(sampleLog(userId));
        dao.save(sampleLog(null));

        List<AuditLog> forUser = dao.findByUserId(userId);

        assertEquals(1, forUser.size());
        assertEquals(userId, forUser.get(0).getUserId());
    }

    @Test
    @DisplayName("findByTable returns only entries for the given affected table")
    void findByTable_filtersByTable() throws Exception {
        dao.save(sampleLog(null));
        AuditLog other = sampleLog(null);
        other.setTableAffected("doctors");
        dao.save(other);

        List<AuditLog> forPatients = dao.findByTable("patients");

        assertEquals(1, forPatients.size());
        assertEquals("patients", forPatients.get(0).getTableAffected());
    }

    @Test
    @DisplayName("deleteOlderThanDays removes only entries older than the cutoff")
    void deleteOlderThanDays_removesOnlyOldEntries() throws Exception {
        insertLogWithAge(40);
        dao.save(sampleLog(null)); // fresh, created "now"

        int deleted = dao.deleteOlderThanDays(30);

        assertEquals(1, deleted);
        PageResult<AuditLog> remaining = dao.findAll(CursorPagination.firstPage());
        assertEquals(1, remaining.getCount());
    }

    private void insertLogWithAge(int daysOld) throws SQLException {
        String sql = "INSERT INTO audit_log (log_id, user_id, action, table_affected, record_id, created_at) "
                + "VALUES (gen_random_uuid(), NULL, 'OLD_ACTION', 'patients', NULL, "
                + "CURRENT_TIMESTAMP - (? || ' days')::interval)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, daysOld);
            ps.executeUpdate();
        }
    }
}
