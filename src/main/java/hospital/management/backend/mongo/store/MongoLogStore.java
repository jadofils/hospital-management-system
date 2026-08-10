package hospital.management.backend.mongo.store;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.mongo.config.MongoConfig;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.model.user.SystemLog;
import org.bson.Document;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Secondary (non-transactional) log store backed by MongoDB.
 *
 * PostgreSQL remains the source of truth for all logs used by the UI and
 * retention policies. This class mirrors those writes into MongoDB so the
 * NoSQL store holds an independent queryable copy — satisfying the project
 * requirement to use MongoDB for medical logs.
 *
 * All methods are fire-and-forget: a null database or any exception is
 * logged as a warning and silently swallowed. MongoDB unavailability must
 * never surface to the caller.
 */
public final class MongoLogStore {

    private static final AppLogger logger = AppLogger.getLogger(MongoLogStore.class);

    static final String AUDIT_COLLECTION  = "audit_logs";
    static final String SYSTEM_COLLECTION = "system_logs";

    private MongoLogStore() {}

    // ── Write ─────────────────────────────────────────────────────────────────

    public static void writeAudit(AuditLog log) {
        if (log == null) return;
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return;
            Document doc = new Document()
                .append("log_id",         log.getLogId())
                .append("user_id",        log.getUserId())
                .append("action",         log.getAction())
                .append("table_affected", log.getTableAffected())
                .append("record_id",      log.getRecordId())
                .append("created_at",     toDate(log));
            db.getCollection(AUDIT_COLLECTION).insertOne(doc);
        } catch (Exception e) {
            logger.warn("MongoLogStore.writeAudit failed: " + e.getMessage());
        }
    }

    public static void writeSystem(SystemLog log) {
        if (log == null) return;
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return;
            Document doc = new Document()
                .append("log_id",     log.getLogId())
                .append("user_id",    log.getUserId())
                .append("level",      log.getLogLevel())
                .append("source",     log.getSource())
                .append("message",    log.getMessage())
                .append("created_at", toDate(log));
            db.getCollection(SYSTEM_COLLECTION).insertOne(doc);
        } catch (Exception e) {
            logger.warn("MongoLogStore.writeSystem failed: " + e.getMessage());
        }
    }

    // ── Read (used by MongoBenchmarkService) ──────────────────────────────────

    public static List<Document> findSystemLogsByLevel(String level, int limit) {
        List<Document> results = new ArrayList<>();
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return results;
            db.getCollection(SYSTEM_COLLECTION)
              .find(Filters.eq("level", level))
              .limit(limit)
              .into(results);
        } catch (Exception e) {
            logger.warn("MongoLogStore.findSystemLogsByLevel failed: " + e.getMessage());
        }
        return results;
    }

    public static List<Document> findAuditLogsByTable(String tableAffected, int limit) {
        List<Document> results = new ArrayList<>();
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return results;
            db.getCollection(AUDIT_COLLECTION)
              .find(Filters.eq("table_affected", tableAffected))
              .limit(limit)
              .into(results);
        } catch (Exception e) {
            logger.warn("MongoLogStore.findAuditLogsByTable failed: " + e.getMessage());
        }
        return results;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Date toDate(AuditLog log) {
        return log.getCreatedAt() != null
            ? Date.from(log.getCreatedAt().toInstant(ZoneOffset.UTC))
            : new Date();
    }

    private static Date toDate(SystemLog log) {
        return log.getCreatedAt() != null
            ? Date.from(log.getCreatedAt().toInstant(ZoneOffset.UTC))
            : new Date();
    }
}