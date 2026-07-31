package hospital.management.backend.daemon;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Purges old records from the append-only log tables (system_logs, audit_log).
 *
 * Log tables are never soft-deleted — rows are hard-deleted here once they are
 * older than the configured retention period. This is the only place in the system
 * that issues a hard DELETE on log tables.
 *
 * Both tables are cleaned in a single connection to avoid two round-trips.
 */
public class DbLogCleaner implements CleanupTask {

    private static final AppLogger logger = AppLogger.getLogger(DbLogCleaner.class);

    @Override
    public String getName() { return "db-logs"; }

    @Override
    public String run(RetentionPolicy policy) throws Exception {
        int days = policy.getDbLogRetentionDays();

        String deleteSysLogs   = "DELETE FROM system_logs WHERE created_at < NOW() - INTERVAL '" + days + " days'";
        String deleteAuditLogs = "DELETE FROM audit_log   WHERE created_at < NOW() - INTERVAL '" + days + " days'";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            int sysCount, auditCount;

            try (PreparedStatement ps = conn.prepareStatement(deleteSysLogs)) {
                sysCount = ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(deleteAuditLogs)) {
                auditCount = ps.executeUpdate();
            }

            conn.commit();
            String summary = "Deleted " + sysCount + " system log(s) and "
                + auditCount + " audit log(s) older than " + days + " days.";
            logger.info("[" + getName() + "] " + summary);
            return summary;

        } catch (Exception e) {
            logger.error("[" + getName() + "] DB log cleanup failed — rolled back.", e);
            throw e;
        }
    }
}