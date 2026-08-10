package hospital.management.backend.service.log;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.log.SystemLogDAOImpl;
import hospital.management.backend.dao.log.interfaces.SystemLogDAO;
import hospital.management.backend.model.user.SystemLog;

import java.time.LocalDateTime;

public final class ServiceMongoLogger {

    private static final AppLogger logger = AppLogger.getLogger(ServiceMongoLogger.class);
    private static final SystemLogDAO DAO = new SystemLogDAOImpl();

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
            DAO.save(log);
        } catch (Exception e) {
            logger.warn("ServiceLogger failed: " + e.getMessage());
        }
    }
}