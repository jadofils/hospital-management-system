package hospital.management.backend.daemon;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Marks users as inactive when they have not logged in within the configured period.
 *
 * A user's last login is determined by the most recent row in user_sessions for that user.
 * Users who are already inactive (is_active = false) or soft-deleted are skipped.
 *
 * SQL logic:
 *   UPDATE users SET is_active = false, updated_at = NOW()
 *   WHERE is_active = true
 *     AND deleted_at IS NULL
 *     AND user_id NOT IN (
 *         SELECT DISTINCT user_id FROM user_sessions
 *         WHERE created_at >= NOW() - INTERVAL 'N days'
 *     )
 */
public class UserInactivityCleaner implements CleanupTask {

    private static final AppLogger logger = AppLogger.getLogger(UserInactivityCleaner.class);

    @Override
    public String getName() { return "user-inactivity"; }

    @Override
    public String run(RetentionPolicy policy) throws Exception {
        int days = policy.getInactiveUserDays();

        String sql = """
            UPDATE users
               SET is_active  = false,
                   updated_at = NOW()
             WHERE is_active  = true
               AND deleted_at IS NULL
               AND user_id NOT IN (
                   SELECT DISTINCT user_id
                     FROM user_sessions
                    WHERE created_at >= NOW() - INTERVAL '%d days'
               )
            """.formatted(days);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int affected = ps.executeUpdate();
            String summary = "Deactivated " + affected + " user(s) inactive for >" + days + " days.";
            logger.info("[" + getName() + "] " + summary);
            return summary;
        }
    }

    /**
     * Returns a count of users who will be deactivated on the next run.
     * The admin UI calls this to preview the impact before saving a new policy.
     */
    public int previewCount(RetentionPolicy policy) {
        int days = policy.getInactiveUserDays();
        String sql = """
            SELECT COUNT(*) FROM users
             WHERE is_active  = true
               AND deleted_at IS NULL
               AND user_id NOT IN (
                   SELECT DISTINCT user_id FROM user_sessions
                    WHERE created_at >= NOW() - INTERVAL '%d days'
               )
            """.formatted(days);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            logger.warn("Preview count failed: " + e.getMessage());
            return -1;
        }
    }
}