package hospital.management.backend.service.log;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.EnvConfig;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.model.user.SystemLog;
import hospital.management.backend.utils.listeners.AppEventType;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Writes audit/system/service-event logs to MongoDB benchmark collections.
 * Failures are intentionally swallowed so business flows never break.
 */
public final class DualLogBridge {

    private static final AppLogger logger = AppLogger.getLogger(DualLogBridge.class);
    private static final String MONGO_DB_NAME = "hospital";
    private static final String MONGO_AUDIT_COLLECTION = "audit_log_benchmark";
    private static final String MONGO_SYSTEM_COLLECTION = "system_log_benchmark";

    private static volatile MongoClient mongoClient;

    private DualLogBridge() {}

    public static void writeAudit(AuditLog audit) {
        if (audit == null) return;
        try {
            MongoCollection<Document> col = mongoDb().getCollection(MONGO_AUDIT_COLLECTION);
            Document doc = new Document()
                .append("logId", audit.getLogId())
                .append("userId", audit.getUserId())
                .append("action", audit.getAction())
                .append("tableAffected", audit.getTableAffected())
                .append("recordId", audit.getRecordId())
                .append("createdAt", toDate(audit.getCreatedAt()))
                .append("source", "audit_service");
            col.insertOne(doc);
            logger.info("Mongo audit log saved logId=" + audit.getLogId());
        } catch (Exception e) {
            logger.warn("Mongo audit mirror failed: " + e.getMessage());
        }
    }

    public static void writeSystem(SystemLog systemLog) {
        if (systemLog == null) return;

        // Mirror to MongoDB for benchmark/reporting path.
        try {
            MongoCollection<Document> col = mongoDb().getCollection(MONGO_SYSTEM_COLLECTION);
            Document doc = new Document()
                .append("logId", systemLog.getLogId())
                .append("userId", systemLog.getUserId())
                .append("logLevel", systemLog.getLogLevel())
                .append("source", systemLog.getSource())
                .append("message", systemLog.getMessage())
                .append("createdAt", toDate(systemLog.getCreatedAt()))
                .append("mirror", "postgres+mongo");
            col.insertOne(doc);
            logger.info("Mongo system log saved source=" + systemLog.getSource() + " level=" + systemLog.getLogLevel());
        } catch (Exception e) {
            logger.warn("Mongo system log mirror failed: " + e.getMessage());
        }
    }

    public static void recordServiceEvent(AppEventType eventType, Object payload, String userId) {
        if (eventType == null) return;

        String payloadText = payload == null ? null : String.valueOf(payload);

        SystemLog systemLog = new SystemLog();
        systemLog.setLogLevel("INFO");
        systemLog.setSource("service-event");
        systemLog.setMessage(eventType.name() + (payloadText == null ? "" : " payload=" + payloadText));
        systemLog.setUserId(userId);
        systemLog.setCreatedAt(LocalDateTime.now());

        // Persist service event into Mongo benchmark collection.
        writeSystem(systemLog);
    }

    private static MongoDatabase mongoDb() {
        MongoClient client = mongoClient;
        if (client == null) {
            synchronized (DualLogBridge.class) {
                client = mongoClient;
                if (client == null) {
                    client = mongoClient = MongoClients.create(EnvConfig.getMongoUri());
                }
            }
        }
        return client.getDatabase(MONGO_DB_NAME);
    }

    private static Date toDate(LocalDateTime value) {
        LocalDateTime time = value == null ? LocalDateTime.now() : value;
        return Date.from(time.toInstant(ZoneOffset.UTC));
    }
}
