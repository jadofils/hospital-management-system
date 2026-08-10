package hospital.management.backend.dto.notification;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Turns a {@link NotificationDTO}'s type + payload into a human sentence.
 * Shared by every UI surface that renders a notification list (the right
 * sidebar's Notifications panel, the navbar bell dropdown) so the same
 * notification always reads identically no matter where it's shown.
 */
public final class NotificationTextFormatter {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private NotificationTextFormatter() {}

    /** Builds a human sentence from a notification's type + payload. */
    public static String describe(NotificationDTO dto) {
        Map<String, Object> payload = dto.getPayload();
        String patientName = payload != null && payload.get("patientName") != null
            ? String.valueOf(payload.get("patientName")) : "a patient";
        String type = dto.getType() == null ? "" : dto.getType();

        if (NotificationTopics.APPOINTMENT_CREATED.equals(type)) {
            return "New appointment booked for " + patientName;
        } else if (NotificationTopics.APPOINTMENT_UPDATED.equals(type)) {
            return "Appointment updated for " + patientName;
        } else if (NotificationTopics.APPOINTMENT_CANCELLED.equals(type)) {
            return "Appointment cancelled for " + patientName;
        } else if (NotificationTopics.PRESCRIPTION_CREATED.equals(type)) {
            return "New prescription issued for " + patientName;
        } else if (NotificationTopics.LAB_RESULT_READY.equals(type)) {
            String testName = payload != null && payload.get("testName") != null
                ? String.valueOf(payload.get("testName")) : "Lab";
            return testName + " result ready for " + patientName;
        }
        // Developer Dashboard admin-audit topics carry the full human-readable text
        // directly in "description" — show that verbatim rather than the humanized
        // topic constant.
        if (payload != null && payload.get("description") != null) {
            return String.valueOf(payload.get("description"));
        }
        // Generic fallback for every other topic — humanizes the constant instead of
        // hand-writing a branch per type, and appends patientName when present.
        if (type.isBlank()) return "Notification";
        String humanized = humanizeTopic(type);
        return payload != null && payload.get("patientName") != null
            ? humanized + " — " + patientName
            : humanized;
    }

    /** {@code "medical_record_created"} -> {@code "Medical record created"}. */
    public static String humanizeTopic(String type) {
        String spaced = type.replace('_', ' ');
        return spaced.isEmpty() ? spaced : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /** Formats {@code dto.getCreatedAt()} as e.g. {@code "Aug 6, 14:32"}, or "" if null. */
    public static String formatTimestamp(NotificationDTO dto) {
        return dto.getCreatedAt() != null ? dto.getCreatedAt().format(TIMESTAMP_FMT) : "";
    }
}
