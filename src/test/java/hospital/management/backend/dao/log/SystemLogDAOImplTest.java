package hospital.management.backend.dao.log;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.model.user.SystemLog;
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
 * runs SystemLogDAOImpl's actual SQL against a real database, proving the RETURNING clause,
 * the log_level CHECK constraint (DEBUG/INFO/WARNING/ERROR), the nullable FK to
 * users(user_id), and the Connection-overloaded save() all behave as the code assumes.
 */
class SystemLogDAOImplTest extends PostgresIntegrationTestBase {

    private final SystemLogDAOImpl dao = new SystemLogDAOImpl();

    private SystemLog sampleLog(String level, String source) {
        SystemLog log = new SystemLog();
        log.setLogLevel(level);
        log.setSource(source);
        log.setMessage("something happened");
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
    @DisplayName("save assigns a generated id and created_at from the DB")
    void save_assignsIdAndTimestamp() throws Exception {
        SystemLog saved = dao.save(sampleLog("INFO", "SchedulerJob"));

        assertNotNull(saved.getLogId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getLogId()));
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("save persists a valid user_id foreign key")
    void save_persistsUserId_whenValid() throws Exception {
        String userId = insertUser();
        SystemLog log = sampleLog("INFO", "SchedulerJob");
        log.setUserId(userId);

        SystemLog saved = dao.save(log);

        PageResult<SystemLog> page = dao.findAll(CursorPagination.firstPage());
        assertEquals(userId, page.getItems().get(0).getUserId());
        assertEquals(saved.getLogId(), page.getItems().get(0).getLogId());
    }

    @Test
    @DisplayName("save rejects a log_level outside DEBUG/INFO/WARNING/ERROR — real CHECK constraint enforcement")
    void save_throws_whenLogLevelInvalid() {
        SystemLog log = sampleLog("VERBOSE", "SchedulerJob");

        Exception ex = assertThrows(Exception.class, () -> dao.save(log));
        assertInstanceOf(DatabaseException.class, ex);
    }

    @Test
    @DisplayName("save rejects a user_id that doesn't exist in users — real FK constraint enforcement")
    void save_throws_whenUserIdDoesNotExist() {
        SystemLog log = sampleLog("INFO", "SchedulerJob");
        log.setUserId(UUID.randomUUID().toString());

        assertThrows(Exception.class, () -> dao.save(log));
    }

    @Test
    @DisplayName("save(log, conn) overload persists using a caller-supplied connection")
    void save_withConnectionOverload_persists() throws Exception {
        SystemLog log = sampleLog("ERROR", "PaymentGateway");

        try (Connection conn = DBConnection.getConnection()) {
            SystemLog saved = dao.save(log, conn);
            assertNotNull(saved.getLogId());
            assertNotNull(saved.getCreatedAt());
        }

        List<SystemLog> all = dao.findBySource("PaymentGateway");
        assertEquals(1, all.size());
    }

    @Test
    @DisplayName("save(log, conn) overload propagates a raw SQLException on a CHECK constraint violation, unwrapped")
    void save_withConnectionOverload_throwsSqlException_onCheckViolation() throws Exception {
        SystemLog log = sampleLog("VERBOSE", "SchedulerJob");

        try (Connection conn = DBConnection.getConnection()) {
            assertThrows(SQLException.class, () -> dao.save(log, conn));
        }
    }

    @Test
    @DisplayName("findAll returns saved entries, most-recently-created first")
    void findAll_returnsEntries() throws Exception {
        dao.save(sampleLog("INFO", "SchedulerJob"));
        dao.save(sampleLog("ERROR", "PaymentGateway"));

        PageResult<SystemLog> page = dao.findAll(CursorPagination.firstPage());

        assertEquals(2, page.getCount());
    }

    @Test
    @DisplayName("findByLevel returns only entries at the given level")
    void findByLevel_filtersByLevel() throws Exception {
        dao.save(sampleLog("ERROR", "PaymentGateway"));
        dao.save(sampleLog("INFO", "SchedulerJob"));

        List<SystemLog> errors = dao.findByLevel("ERROR");

        assertEquals(1, errors.size());
        assertEquals("ERROR", errors.get(0).getLogLevel());
    }

    @Test
    @DisplayName("findBySource returns only entries from the given source")
    void findBySource_filtersBySource() throws Exception {
        dao.save(sampleLog("ERROR", "PaymentGateway"));
        dao.save(sampleLog("INFO", "SchedulerJob"));

        List<SystemLog> fromScheduler = dao.findBySource("SchedulerJob");

        assertEquals(1, fromScheduler.size());
        assertEquals("SchedulerJob", fromScheduler.get(0).getSource());
    }

    @Test
    @DisplayName("deleteOlderThanDays removes only entries older than the cutoff")
    void deleteOlderThanDays_removesOnlyOldEntries() throws Exception {
        insertLogWithAge(40);
        dao.save(sampleLog("INFO", "SchedulerJob")); // fresh, created "now"

        int deleted = dao.deleteOlderThanDays(30);

        assertEquals(1, deleted);
        PageResult<SystemLog> remaining = dao.findAll(CursorPagination.firstPage());
        assertEquals(1, remaining.getCount());
    }

    private void insertLogWithAge(int daysOld) throws SQLException {
        String sql = "INSERT INTO system_logs (log_id, user_id, log_level, source, message, created_at) "
                + "VALUES (gen_random_uuid(), NULL, 'DEBUG', 'OldJob', 'stale entry', "
                + "CURRENT_TIMESTAMP - (? || ' days')::interval)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, daysOld);
            ps.executeUpdate();
        }
    }
}
