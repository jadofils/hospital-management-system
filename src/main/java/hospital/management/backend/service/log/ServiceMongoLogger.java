package hospital.management.backend.service.log;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.model.user.SystemLog;

import java.time.LocalDateTime;

/**
 * Lightweight helper to persist operational service logs directly to Mongo.
 */
public final class ServiceMongoLogger {

    private static final AppLogger logger = AppLogger.getLogger(ServiceMongoLogger.class);
    private static final MongoLogStore STORE = new MongoLogStore();

    private ServiceMongoLogger() {}

    public static void info(String source, String message) {
        write("INFO", source, message, null);
    }

    public static void warn(String source, String message) {
        write("WARNING", source, message, null);
    }

    public static void error(String source, String message, Throwable t) {
        write("ERROR", source, message, t);
    }

    private static void write(String level, String source, String message, Throwable t) {
        try {
            String userId = null;
            try { userId = SessionManager.getCurrentUserId(); } catch (Exception ignored) {}

            SystemLog log = new SystemLog();
            log.setLogLevel(level);
            log.setSource(source == null ? "service" : source);
            String full = message == null ? "" : message;
            if (t != null && t.getMessage() != null && !t.getMessage().isBlank()) {
                full = full + " | " + t.getMessage();
            }
            log.setMessage(full);
            log.setUserId(userId);
            log.setCreatedAt(LocalDateTime.now());
            STORE.saveSystem(log);
            logger.info("MongoLog saved level=" + level + " source=" + log.getSource());
        } catch (Exception e) {
            logger.warn("ServiceMongoLogger failed: " + e.getMessage());
            System.err.println("ServiceMongoLogger failed: " + e.getMessage());
        }
    }
}
