package hospital.management.backend.service.log;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.config.security.SessionManager;

/**
 * Small helper to record audit + system logs from service implementations.
 * Silent on failure to avoid breaking business logic.
 */
public final class ServiceAudit {

    private static final AppLogger logger = AppLogger.getLogger(ServiceAudit.class);

    private ServiceAudit() {}

    public static void record(String table, String action, String recordId) {
        try {
            String userId = null;
            try { userId = SessionManager.getCurrentUserId(); } catch (Exception ignored) {}

            AuditServiceImpl audit = new AuditServiceImpl(new hospital.management.backend.dao.log.AuditLogDAOImpl());
            audit.record(userId, action, table, recordId);

            SystemLogServiceImpl system = new SystemLogServiceImpl(new hospital.management.backend.dao.log.SystemLogDAOImpl());
            system.log("INFO", "service-action", action + " on " + table + " record=" + recordId, userId);

            logger.info("Service action logged: " + action + " on " + table + " record=" + recordId);
            // publish a generic event so UI can react if interested
            EventBus.publish(AppEventType.AUDIT_LOG_RECORDED, recordId);
        } catch (Exception e) {
            // swallow — logging should not stop service flows
            System.err.println("ServiceAudit failure: " + e.getMessage());
        }
    }
}
