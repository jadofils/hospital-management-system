package hospital.management.backend.mongo.store;

import com.mongodb.client.MongoDatabase;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.dto.notification.NotificationDTO;
import hospital.management.backend.mongo.config.MongoConfig;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fire-and-forget MongoDB mirror for notifications.
 *
 * PostgreSQL remains the authoritative store. This class writes a denormalised
 * copy to MongoDB for read-heavy access patterns and to serve as benchmark data.
 * All methods swallow exceptions silently — MongoDB unavailability must never
 * break the notification flow.
 *
 * Collection: notifications
 */
public class MongoNotificationStore {

    private static final AppLogger logger = AppLogger.getLogger(MongoNotificationStore.class);
    static final String COLLECTION = "notifications";

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Mirrors a notification document to MongoDB.
     * Non-blocking: callers must not depend on this completing or succeeding.
     */
    public void mirror(NotificationDTO dto) {
        if (dto == null) return;
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return;

            Document doc = new Document()
                .append("notification_id", dto.getId())
                .append("type",            dto.getType())
                .append("actor_user_id",   dto.getActorUserId())
                .append("recipients",      dto.getRecipients() != null
                    ? dto.getRecipients() : new ArrayList<>())
                .append("payload",         dto.getPayload() != null
                    ? new Document(dto.getPayload()) : null)
                .append("channels",        dto.getChannels() != null
                    ? dto.getChannels() : new ArrayList<>())
                .append("created_at",      new Date());

            db.getCollection(COLLECTION).insertOne(doc);
        } catch (Exception e) {
            logger.warn("MongoNotificationStore.mirror failed: " + e.getMessage());
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns the most recent {@code limit} notifications for a recipient user ID,
     * sorted newest-first.
     * Returns an empty list if MongoDB is unavailable.
     */
    public List<Document> findForUser(String userId, int limit) {
        List<Document> out = new ArrayList<>();
        if (userId == null || userId.isBlank()) return out;
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return out;
            db.getCollection(COLLECTION)
              .find(new Document("recipients", userId))
              .sort(new Document("created_at", -1))
              .limit(limit)
              .forEach(out::add);
        } catch (Exception e) {
            logger.warn("MongoNotificationStore.findForUser failed: " + e.getMessage());
        }
        return out;
    }
}