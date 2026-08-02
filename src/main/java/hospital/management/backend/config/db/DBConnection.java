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

    private static final AppLogger logger = AppLogger.getLogger(DBConnection.class);

    /**
     * Lazily initialised so integration tests can point this pool at a
     * Testcontainers-managed Postgres instance (via {@link #configureForTests})
     * before the first real connection is ever borrowed. The production app
     * still gets its config from {@link DBConfig}/.env, unaffected — the
     * override only takes effect if a test explicitly calls it first.
     */
    private static volatile HikariDataSource dataSource;

    private DBConnection() {}

    private static HikariDataSource dataSource() {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            synchronized (DBConnection.class) {
                ds = dataSource;
                if (ds == null) {
                    ds = dataSource = buildDataSource(DBConfig.getUrl(), DBConfig.getUsername(), DBConfig.getPassword());
                }
            }
        }
        return ds;
    }

    private static HikariDataSource buildDataSource(String url, String username, String password) {
        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setPoolName(DBConfig.POOL_NAME);
            cfg.setJdbcUrl(url);
            cfg.setUsername(username);
            cfg.setPassword(password);
            cfg.setMaximumPoolSize(DBConfig.MAX_POOL_SIZE);
            cfg.setMinimumIdle(DBConfig.MIN_IDLE);
            cfg.setConnectionTimeout(DBConfig.CONNECTION_TIMEOUT_MS);
            cfg.setIdleTimeout(DBConfig.IDLE_TIMEOUT_MS);
            cfg.setMaxLifetime(DBConfig.MAX_LIFETIME_MS);

            HikariDataSource ds = new HikariDataSource(cfg);
            logger.info("DB pool '" + DBConfig.POOL_NAME + "' ready — max " + DBConfig.MAX_POOL_SIZE + " connections.");
            return ds;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to initialise DB connection pool: " + e.getMessage(), e);
        }
    }

    /**
     * Test-only hook: points this pool at a different database (e.g. a
     * Testcontainers Postgres instance) instead of .env's configured one.
     * Must be called before any DAO borrows a connection in this JVM —
     * once the pool is built lazily on first use, it is not rebuilt.
     */
    public static synchronized void configureForTests(String url, String username, String password) {
        if (dataSource != null) {
            dataSource.close();
        }
        dataSource = buildDataSource(url, username, password);
    }

    /**
     * Borrows a connection from the pool.
     * Always use in a try-with-resources block so it is returned automatically.
     *
     * @throws SQLException if the pool is exhausted or the DB is unreachable
     */
    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    /**
     * Returns true if the pool has been initialised and the DB is reachable.
     * Useful for a startup health-check without borrowing a real connection.
     */
    public static boolean isHealthy() {
        try (Connection c = dataSource().getConnection()) {
            return c.isValid(2);
        } catch (SQLException e) {
            logger.warn("DB health check failed: " + e.getMessage());
            return false;
        }
    }
}