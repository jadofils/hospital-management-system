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
}
