package hospital.management.backend.config.db;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.exceptions.ConfigurationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * HikariCP connection pool lifecycle.
 * All tuning values come from {@link DBConfig} — this class only manages
 * the pool itself and hands out connections.
 *
 * Usage:
 *   try (Connection conn = DBConnection.getConnection()) {
 *       // use conn — auto-returned to pool on close
 *   }
 */
public final class DBConnection {

    private static final AppLogger        logger     = AppLogger.getLogger(DBConnection.class);
    private static final HikariDataSource DATA_SOURCE;

    static {
        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setPoolName(DBConfig.POOL_NAME);
            cfg.setJdbcUrl(DBConfig.getUrl());
            cfg.setUsername(DBConfig.getUsername());
            cfg.setPassword(DBConfig.getPassword());
            cfg.setMaximumPoolSize(DBConfig.MAX_POOL_SIZE);
            cfg.setMinimumIdle(DBConfig.MIN_IDLE);
            cfg.setConnectionTimeout(DBConfig.CONNECTION_TIMEOUT_MS);
            cfg.setIdleTimeout(DBConfig.IDLE_TIMEOUT_MS);
            cfg.setMaxLifetime(DBConfig.MAX_LIFETIME_MS);

            DATA_SOURCE = new HikariDataSource(cfg);
            logger.info("DB pool '" + DBConfig.POOL_NAME + "' ready — max " + DBConfig.MAX_POOL_SIZE + " connections.");
        } catch (Exception e) {
            throw new ConfigurationException("Failed to initialise DB connection pool: " + e.getMessage(), e);
        }
    }

    private DBConnection() {}

    /**
     * Borrows a connection from the pool.
     * Always use in a try-with-resources block so it is returned automatically.
     *
     * @throws SQLException if the pool is exhausted or the DB is unreachable
     */
    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    /**
     * Returns true if the pool has been initialised and the DB is reachable.
     * Useful for a startup health-check without borrowing a real connection.
     */
    public static boolean isHealthy() {
        try (Connection c = DATA_SOURCE.getConnection()) {
            return c.isValid(2);
        } catch (SQLException e) {
            logger.warn("DB health check failed: " + e.getMessage());
            return false;
        }
    }
}