package hospital.management.backend.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.MailConfig;
import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dto.notification.NotificationDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.mongo.store.MongoNotificationStore;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.service.log.ServiceMongoLogger;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationServiceImpl implements NotificationService {

    private static final AppLogger logger = AppLogger.getLogger(NotificationServiceImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    private final UserService userService;
    private final MongoNotificationStore mongoStore = new MongoNotificationStore();

    public NotificationServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public String createNotification(NotificationDTO dto) throws Exception {
        ServiceMongoLogger.info("notification.service", "Creating notification id=" + dto.getId());

        String recipientsJson = dto.getRecipients() != null ? MAPPER.writeValueAsString(dto.getRecipients()) : "[]";
        String payloadJson    = dto.getPayload()  != null ? MAPPER.writeValueAsString(dto.getPayload())  : null;
        String channelsJson   = dto.getChannels() != null ? MAPPER.writeValueAsString(dto.getChannels()) : null;
        String statusJson     = dto.getStatus()   != null ? MAPPER.writeValueAsString(dto.getStatus())   : null;

        String sql = "INSERT INTO notifications " +
            "(notification_id, type, actor_user_id, recipients, payload, channels, status, priority) " +
            "VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?) " +
            "ON CONFLICT DO NOTHING";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(dto.getId()));
            ps.setString(2, dto.getType());
            if (dto.getActorUserId() != null && !dto.getActorUserId().isBlank()) {
                ps.setObject(3, UUID.fromString(dto.getActorUserId()));
            } else {
                ps.setNull(3, Types.OTHER);
            }
            ps.setString(4, recipientsJson);
            ps.setString(5, payloadJson);
            ps.setString(6, channelsJson);
            ps.setString(7, statusJson);
            ps.setString(8, "normal");
            ps.executeUpdate();
        }

        if (dto.getChannels() != null && dto.getChannels().contains("email")) {
            try { sendEmail(dto); } catch (Exception e) {
                logger.warn("Email send failed: " + e.getMessage());
            }
        }

        logger.info("Notification created: " + dto.getId());
        ServiceMongoLogger.info("notification.service", "Notification persisted id=" + dto.getId());
        mongoStore.mirror(dto);
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
                msg.setSubject("Notification: " + dto.getType());
                msg.setContent(buildHtml(dto, user), "text/html; charset=utf-8");
                Transport.send(msg);
                logger.info("Email sent to " + user.getEmail());
                ServiceMongoLogger.info("notification.service", "Notification email sent to=" + user.getEmail());
            } catch (Exception e) {
                logger.warn("Failed to send notification email to user " + userId + ": " + e.getMessage());
                ServiceMongoLogger.warn("notification.service", "Notification email failed for userId=" + userId);
            }
        }
    }

    @Override
    public List<NotificationDTO> listForUser(String userId, int limit) throws Exception {
        String sql = "SELECT notification_id, type, actor_user_id, recipients, payload, channels, " +
            "status, priority, created_at, read_at FROM notifications " +
            "WHERE recipients @> jsonb_build_array(?::text) AND deleted_at IS NULL " +
            "ORDER BY created_at DESC LIMIT ?";

        List<NotificationDTO> out = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try { out.add(mapRow(rs)); } catch (Exception ignored) {}
                }
            }
        }
        return out;
    }

    @Override
    public int countUnreadForUser(String userId) throws Exception {
        String sql = "SELECT COUNT(*) FROM notifications " +
            "WHERE recipients @> jsonb_build_array(?::text) AND deleted_at IS NULL AND read_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public void markAsRead(String notificationId, String userId) throws Exception {
        String sql = "UPDATE notifications SET read_at = CURRENT_TIMESTAMP " +
            "WHERE notification_id = ? AND recipients @> jsonb_build_array(?::text) AND read_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(notificationId));
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public void markAllAsRead(String userId) throws Exception {
        String sql = "UPDATE notifications SET read_at = CURRENT_TIMESTAMP " +
            "WHERE recipients @> jsonb_build_array(?::text) AND deleted_at IS NULL AND read_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            int updated = ps.executeUpdate();
            logger.info("Marked " + updated + " notification(s) read for user " + userId);
        }
    }

    private NotificationDTO mapRow(ResultSet rs) throws Exception {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(rs.getObject("notification_id", UUID.class).toString());
        dto.setType(rs.getString("type"));
        UUID actorId = rs.getObject("actor_user_id", UUID.class);
        dto.setActorUserId(actorId != null ? actorId.toString() : null);
        String recipientsJson = rs.getString("recipients");
        if (recipientsJson != null) {
            dto.setRecipients(MAPPER.readValue(recipientsJson,
                MAPPER.getTypeFactory().constructCollectionType(List.class, String.class)));
        }
        String payloadJson = rs.getString("payload");
        if (payloadJson != null) {
            dto.setPayload(MAPPER.readValue(payloadJson,
                MAPPER.getTypeFactory().constructMapType(java.util.Map.class, String.class, Object.class)));
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            dto.setCreatedAt(createdAt.toLocalDateTime().atOffset(java.time.ZoneOffset.UTC));
        }
        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) {
            dto.setReadAt(readAt.toLocalDateTime().atOffset(java.time.ZoneOffset.UTC));
        }
        return dto;
    }

    private String buildHtml(NotificationDTO dto, UserDTO recipient) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p>Hello ").append(recipient.getUsername()).append(",</p>");
        sb.append("<p>You have a new notification: <strong>").append(dto.getType()).append("</strong>.</p>");
        sb.append("<p>Details:</p><ul>");
        if (dto.getPayload() != null) {
            dto.getPayload().forEach((k, v) ->
                sb.append("<li><strong>").append(k).append(":</strong> ").append(v).append("</li>"));
        }
        sb.append("</ul>");
        sb.append("<p>Time: ").append(dto.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)).append("</p>");
        sb.append("<p>Regards,<br/>Hospital Management System</p>");
        sb.append("</body></html>");
        return sb.toString();
    }
}