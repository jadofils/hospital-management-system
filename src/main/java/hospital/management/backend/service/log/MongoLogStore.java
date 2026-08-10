package hospital.management.backend.service.log;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import hospital.management.backend.config.EnvConfig;
import hospital.management.backend.model.user.AuditLog;
import hospital.management.backend.model.user.SystemLog;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;
import org.bson.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB-backed store for audit/system logs used by frontend log pages.
 */
public class MongoLogStore {

    private static final String DB_NAME = "hospital";
    private static final String AUDIT_COLLECTION = "audit_log_benchmark";
    private static final String SYSTEM_COLLECTION = "system_log_benchmark";

    private static volatile MongoClient mongoClient;

    public AuditLog saveAudit(AuditLog log) {
        if (log.getLogId() == null || log.getLogId().isBlank()) {
            log.setLogId(UUID.randomUUID().toString());
        }
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(LocalDateTime.now());
        }

        Document doc = new Document()
            .append("logId", log.getLogId())
            .append("userId", log.getUserId())
            .append("action", log.getAction())
            .append("tableAffected", log.getTableAffected())
            .append("recordId", log.getRecordId())
            .append("createdAt", toDate(log.getCreatedAt()));

        auditCollection().insertOne(doc);
        return log;
    }

    public SystemLog saveSystem(SystemLog log) {
        if (log.getLogId() == null || log.getLogId().isBlank()) {
            log.setLogId(UUID.randomUUID().toString());
        }
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(LocalDateTime.now());
        }

        Document doc = new Document()
            .append("logId", log.getLogId())
            .append("userId", log.getUserId())
            .append("logLevel", log.getLogLevel())
            .append("source", log.getSource())
            .append("message", log.getMessage())
            .append("createdAt", toDate(log.getCreatedAt()));

        systemCollection().insertOne(doc);
        return log;
    }

    public PageResult<AuditLog> findAllAudit(PageRequest request) {
        List<AuditLog> rows = new ArrayList<>();
        FindIterable<Document> iter = auditCollection()
            .find(cursorFilter(request))
            .sort(new Document("createdAt", request.getDirection() == PageRequest.SortDirection.DESC ? -1 : 1))
            .limit(request.getPageSize() + 1);
        for (Document d : iter) rows.add(mapAudit(d));
        return CursorPagination.toResult(rows, request, AuditLog::getCreatedAt);
    }

    public List<AuditLog> findAuditByUser(String userId) {
        List<AuditLog> rows = new ArrayList<>();
        for (Document d : auditCollection().find(Filters.eq("userId", userId)).sort(new Document("createdAt", -1))) {
            rows.add(mapAudit(d));
        }
        return rows;
    }

    public PageResult<SystemLog> findAllSystem(PageRequest request) {
        List<SystemLog> rows = new ArrayList<>();
        FindIterable<Document> iter = systemCollection()
            .find(cursorFilter(request))
            .sort(new Document("createdAt", request.getDirection() == PageRequest.SortDirection.DESC ? -1 : 1))
            .limit(request.getPageSize() + 1);
        for (Document d : iter) rows.add(mapSystem(d));
        return CursorPagination.toResult(rows, request, SystemLog::getCreatedAt);
    }

    public List<SystemLog> findSystemByLevel(String level) {
        List<SystemLog> rows = new ArrayList<>();
        for (Document d : systemCollection().find(Filters.eq("logLevel", level)).sort(new Document("createdAt", -1))) {
            rows.add(mapSystem(d));
        }
        return rows;
    }

    public int purgeSystemOlderThanDays(int days) {
        if (days <= 0) {
            return (int) systemCollection().deleteMany(new Document()).getDeletedCount();
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return (int) systemCollection().deleteMany(Filters.lt("createdAt", toDate(cutoff))).getDeletedCount();
    }

    private Document cursorFilter(PageRequest request) {
        if (request.isFirstPage()) return new Document();
        LocalDateTime ts = CursorPagination.decodeCursor(request.getCursor());
        Date d = toDate(ts);
        return request.getDirection() == PageRequest.SortDirection.DESC
            ? new Document("createdAt", new Document("$lt", d))
            : new Document("createdAt", new Document("$gt", d));
    }

    private AuditLog mapAudit(Document d) {
        AuditLog log = new AuditLog();
        log.setLogId(d.getString("logId"));
        log.setUserId(d.getString("userId"));
        log.setAction(d.getString("action"));
        log.setTableAffected(d.getString("tableAffected"));
        log.setRecordId(d.getString("recordId"));
        log.setCreatedAt(toLocalDateTime(d.getDate("createdAt")));
        return log;
    }

    private SystemLog mapSystem(Document d) {
        SystemLog log = new SystemLog();
        log.setLogId(d.getString("logId"));
        log.setUserId(d.getString("userId"));
        log.setLogLevel(d.getString("logLevel"));
        log.setSource(d.getString("source"));
        log.setMessage(d.getString("message"));
        log.setCreatedAt(toLocalDateTime(d.getDate("createdAt")));
        return log;
    }

    private MongoCollection<Document> auditCollection() {
        return mongoDb().getCollection(AUDIT_COLLECTION);
    }

    private MongoCollection<Document> systemCollection() {
        return mongoDb().getCollection(SYSTEM_COLLECTION);
    }

    private MongoDatabase mongoDb() {
        MongoClient client = mongoClient;
        if (client == null) {
            synchronized (MongoLogStore.class) {
                client = mongoClient;
                if (client == null) {
                    client = mongoClient = MongoClients.create(EnvConfig.getMongoUri());
                }
            }
        }
        return client.getDatabase(DB_NAME);
    }

    private Date toDate(LocalDateTime value) {
        LocalDateTime safe = value == null ? LocalDateTime.now() : value;
        return Date.from(safe.toInstant(ZoneOffset.UTC));
    }

    private LocalDateTime toLocalDateTime(Date value) {
        if (value == null) return LocalDateTime.now();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(value.getTime()), ZoneOffset.UTC);
    }
}
