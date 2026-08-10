package hospital.management.backend.service.log;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.dao.log.SystemLogDAOImpl;
import hospital.management.backend.dao.log.interfaces.SystemLogDAO;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.model.user.SystemLog;
import hospital.management.backend.utils.listeners.AppEventType;

import java.time.LocalDateTime;

/**
 * Routes audit/system/service-event logs to PostgreSQL.
 * Failures are intentionally swallowed so business flows never break.
 */
public final class DualLogBridge {

    private static final AppLogger logger = AppLogger.getLogger(DualLogBridge.class);
    private static final SystemLogDAO SYSTEM_DAO = new SystemLogDAOImpl();

    private DualLogBridge() {}

    /** No-op: audit logs are written directly to PostgreSQL by the service/DAO layer. */
    public static void writeAudit(AuditLog audit) {}

    /** No-op: system logs are written directly to PostgreSQL by ServiceMongoLogger. */
    public static void writeSystem(SystemLog systemLog) {}

    public static void recordServiceEvent(AppEventType eventType, Object payload, String userId) {
        if (eventType == null) return;
        try {
            String payloadText = payload == null ? "" : " payload=" + payload;
            SystemLog log = new SystemLog();
            log.setLogLevel("INFO");
            log.setSource("service-event");
            log.setMessage(eventType.name() + payloadText);
            log.setUserId(userId);
            log.setCreatedAt(LocalDateTime.now());
            SYSTEM_DAO.save(log);
        } catch (Exception e) {
            logger.warn("DualLogBridge.recordServiceEvent failed: " + e.getMessage());
        }
    }
}