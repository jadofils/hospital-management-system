package hospital.management.backend.utils;

import java.util.regex.Pattern;

/**
 * Sanitization helpers for log output and user-supplied input.
 * Call these before writing any value to a log or before storing
 * user input that will be embedded in queries or messages.
 */
public final class SanitizeUtils {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("([a-zA-Z0-9._%+\\-]{1,3})[a-zA-Z0-9._%+\\-]*@([a-zA-Z0-9.\\-]{1,3})[a-zA-Z0-9.\\-]*\\.([a-zA-Z]{2,})");
    private static final Pattern UUID_PATTERN =
        Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(\\+?\\d{1,3}[\\s\\-]?)?\\(?\\d{3}\\)?[\\s\\-]?\\d{3}[\\s\\-]?\\d{4}");
    private static final Pattern CONTROL_CHAR_PATTERN =
        Pattern.compile("[\\p{Cntrl}&&[^\\t\\n\\r]]");

    private SanitizeUtils() {}

    // ── Log masking ───────────────────────────────────────────────────────────

    /**
     * Masks PII in a log message so sensitive values are never written to log files.
     * Handles emails, UUIDs, and phone numbers found anywhere in the string.
     *
     * @param message raw log message, may contain PII
     * @return safe message with sensitive values masked; null if input is null
     */
    public static String maskForLog(String message) {
        if (message == null) return null;
        String out = EMAIL_PATTERN.matcher(message)
            .replaceAll(m -> m.group(1) + "***@" + m.group(2) + "***." + m.group(3));
        out = UUID_PATTERN.matcher(out)
            .replaceAll(m -> m.group().substring(0, 8) + "-***");
        out = PHONE_PATTERN.matcher(out)
            .replaceAll(m -> {
                String digits = m.group().replaceAll("\\D", "");
                String last4  = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
                return "***-***-" + last4;
            });
        return out;
    }

    /**
     * Masks a standalone email address. Returns {@code "***"} for null/blank.
     * Example: {@code "john.doe@example.com"} → {@code "joh***@exa***.com"}
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) return "***";
        return EMAIL_PATTERN.matcher(email)
            .replaceAll(m -> m.group(1) + "***@" + m.group(2) + "***." + m.group(3));
    }

    /**
     * Masks a standalone UUID, keeping only the first 8 characters visible.
     * Example: {@code "550e8400-e29b-41d4-a716-446655440000"} → {@code "550e8400-***"}
     */
    public static String maskUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) return "***";
        return uuid.length() >= 8 ? uuid.substring(0, 8) + "-***" : "***";
    }

    /**
     * Masks a standalone phone number, keeping only the last 4 digits.
     * Example: {@code "+1 (555) 867-5309"} → {@code "***-***-5309"}
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "***";
        String digits = phone.replaceAll("\\D", "");
        String last4  = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
        return "***-***-" + last4;
    }

    // ── Input sanitization ────────────────────────────────────────────────────

    /**
     * Strips ASCII control characters (except tab, newline, carriage-return) from user input.
     * Use before storing or displaying any free-text field from an external source.
     *
     * @param input raw user input
     * @return sanitized string, or null if input is null
     */
    public static String stripControlChars(String input) {
        if (input == null) return null;
        return CONTROL_CHAR_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Trims whitespace and strips control characters in one call.
     * Convenience wrapper for the common case of cleaning a form field before validation.
     */
    public static String clean(String input) {
        if (input == null) return null;
        return stripControlChars(input.strip());
    }
}