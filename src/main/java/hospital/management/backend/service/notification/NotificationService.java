package hospital.management.backend.service.notification;

import hospital.management.backend.dto.notification.NotificationDTO;
import java.util.List;

public interface NotificationService {
    /** Persist a notification (NoSQL-backed) and enqueue delivery. Returns notification id. */
    String createNotification(NotificationDTO dto) throws Exception;

    /** Send email channel for the given notification (synchronous send). */
    void sendEmail(NotificationDTO dto) throws Exception;

    /** List recent notifications for a user. */
    List<NotificationDTO> listForUser(String userId, int limit) throws Exception;

    /** Counts a user's unread (read_at IS NULL) notifications — for a bell badge. */
    int countUnreadForUser(String userId) throws Exception;

    /**
     * Marks a notification read. {@code userId} scopes the lookup (a user can
     * only mark read what's actually addressed to them) but {@code read_at} is
     * one column on one row, not a per-recipient state — for a role-broadcast
     * notification with several recipients, one person opening it marks it read
     * for all of them. Acceptable for now: true per-recipient read tracking
     * would need a separate (notification_id, user_id, read_at) table, which
     * every currently-wired notification's usage pattern doesn't yet justify.
     */
    void markAsRead(String notificationId, String userId) throws Exception;

    /** Marks every one of a user's currently-unread notifications as read. */
    void markAllAsRead(String userId) throws Exception;
}
