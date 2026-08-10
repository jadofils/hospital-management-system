package hospital.management.backend.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.MailConfig;
import hospital.management.backend.dto.notification.NotificationDTO;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.service.log.ServiceMongoLogger;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Transport;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal NotificationService implementation that persists notifications
 * as JSON files under the user's home directory (acts as a NoSQL-like store)
 * and sends emails via MailConfig. Replace the file-backed persistence with
 * a real MongoDB client in production — see `src/main/resources/mongo/notifications_init.js`.
 */
public class NotificationServiceImpl implements NotificationService {

    private static final AppLogger logger = AppLogger.getLogger(NotificationServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final UserService userService;
    private final MongoClient mongoClient;
    private final MongoCollection<Document> col;
    private static final JsonWriterSettings JSON_SETTINGS = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    public NotificationServiceImpl(UserService userService) {
        this.userService = userService;
        this.mongoClient = MongoClients.create(hospital.management.backend.config.EnvConfig.getMongoUri());
        MongoDatabase db = mongoClient.getDatabase("hospital");
        this.col = db.getCollection("notifications");
    }

    @Override
    public String createNotification(NotificationDTO dto) throws Exception {
        ServiceMongoLogger.info("notification.service", "Creating notification id=" + dto.getId());

        // persist into MongoDB notifications collection
        String json = MAPPER.writeValueAsString(dto);
        Document doc = Document.parse(json);
        col.insertOne(doc);

        // fire-and-forget email delivery for recipients who opted-in to email
        try { sendEmail(dto); } catch (Exception e) { logger.warn("Email send failed: " + e.getMessage()); }

        logger.info("Notification created: " + dto.getId());
        ServiceMongoLogger.info("notification.service", "Notification persisted id=" + dto.getId());
        return dto.getId();
    }

    @Override
    public void sendEmail(NotificationDTO dto) throws Exception {
        if (dto.getRecipients() == null || dto.getRecipients().isEmpty()) return;

        for (String userId : dto.getRecipients()) {
            try {
                UserDTO user = userService.findById(userId);
                if (user == null || user.getEmail() == null || user.getEmail().isBlank()) continue;

                MimeMessage msg = new MimeMessage(MailConfig.getSession());
                msg.setFrom(new InternetAddress(MailConfig.getFromAddress(), MailConfig.getFromName()));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail(), user.getUsername()));

                // basic templating for appointment.created
                String subject = "Notification: " + dto.getType();
                String html = buildHtml(dto, user);
                msg.setSubject(subject);
                msg.setContent(html, "text/html; charset=utf-8");

                Transport.send(msg);
                logger.info("Email sent to " + user.getEmail());
                ServiceMongoLogger.info("notification.service", "Notification email sent to=" + user.getEmail());
            } catch (Exception e) {
                logger.warn("Failed to send notification email to user " + userId + ": " + e.getMessage());
                ServiceMongoLogger.warn("notification.service", "Notification email failed for userId=" + userId);
            }
        }
    }

    private String buildHtml(NotificationDTO dto, UserDTO recipient) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p>Hello ").append(recipient.getUsername()).append(",</p>");
        sb.append("<p>");
        sb.append("You have a new notification: <strong>").append(dto.getType()).append("</strong>.");
        sb.append("</p>");
        sb.append("<p>Details:</p>");
        sb.append("<ul>");
        if (dto.getPayload() != null) {
            dto.getPayload().forEach((k,v) -> sb.append("<li><strong>").append(k).append(":</strong> ").append(v).append("</li>"));
        }
        sb.append("</ul>");
        sb.append("<p>Time: ").append(dto.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append("</p>");
        sb.append("<p>Regards,<br/>Hospital Management System</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    @Override
    public List<NotificationDTO> listForUser(String userId, int limit) throws Exception {
        List<NotificationDTO> out = new ArrayList<>();
        Document filter = new Document("recipients", userId);
        FindIterable<Document> iter = col.find(filter).sort(new Document("createdAt", -1)).limit(limit);
        for (Document d : iter) {
            try {
                String json = d.toJson(JSON_SETTINGS);
                NotificationDTO dto = MAPPER.readValue(json, NotificationDTO.class);
                out.add(dto);
            } catch (Exception ignored) {}
        }
        return out;
    }
}
