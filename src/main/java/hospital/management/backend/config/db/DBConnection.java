package hospital.management.backend.config.db;

import hospital.management.backend.config.EnvConfig;
import hospital.management.backend.config.AppLogger; // your custom wrapper
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    private static final AppLogger logger = AppLogger.getLogger(DBConnection.class);
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(EnvConfig.getUrl());
            config.setUsername(EnvConfig.getUser());
            config.setPassword(EnvConfig.getPassword());
            config.setMaximumPoolSize(10);   // adjust based on workload
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(20000);

            dataSource = new HikariDataSource(config);
            logger.info("Database connection pool initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to initialize DB pool", e);
            throw new RuntimeException("Failed to initialize DB pool", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        if (connection != null) {
            logger.info("Database connected successfully!");
        } else {
            logger.warn("Database connection returned null.");
        }
        return connection;
    }
}
