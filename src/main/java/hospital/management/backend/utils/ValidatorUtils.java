package hospital.management.backend.utils;

import java.util.regex.Pattern;

/**
 * Reusable input validation guards.
 * All methods throw {@link IllegalArgumentException} or {@link IllegalStateException}
 * on failure so callers get a clear message without needing to repeat the check inline.
 */
public final class ValidatorUtils {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern UUID_PATTERN =
        Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^\\+?[\\d\\s\\-().]{7,20}$");

    private ValidatorUtils() {}

    // ── Presence ──────────────────────────────────────────────────────────────

    /**
     * Throws {@link IllegalArgumentException} if {@code value} is null or blank.
     *
     * @param value     the value to check
     * @param fieldName used in the error message (e.g. "password", "userId")
     * @return the value, trimmed, so callers can chain: {@code String v = requireNonBlank(raw, "name");}
     */
    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.strip();
    }

    // ── Length ────────────────────────────────────────────────────────────────

    /**
     * Throws {@link IllegalArgumentException} if {@code value} is shorter than {@code min} characters.
     * Null/blank values are caught first — combine with {@link #requireNonBlank} if needed.
     */
    public static void requireMinLength(String value, int min, String fieldName) {
        if (value == null || value.length() < min) {
            throw new IllegalArgumentException(
                fieldName + " must be at least " + min + " characters.");
        }
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code value} exceeds {@code max} characters.
     */
    public static void requireMaxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(
                fieldName + " must not exceed " + max + " characters.");
        }
    }

    // ── Numeric range ─────────────────────────────────────────────────────────

    /**
     * Throws {@link IllegalStateException} if {@code value} is outside [{@code min}, {@code max}].
     * Uses {@link IllegalStateException} because this is typically a configuration problem,
     * not a user-input problem.
     */
    public static void requireRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalStateException(
                fieldName + " must be between " + min + " and " + max + " (got " + value + ").");
        }
    }

    // ── Format ────────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code email} matches a basic RFC-5321 pattern.
     * Does not perform DNS/MX lookup — use for fast format checks only.
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Returns true if {@code uuid} matches the canonical 8-4-4-4-12 UUID format.
     */
    public static boolean isValidUuid(String uuid) {
        return uuid != null && UUID_PATTERN.matcher(uuid).matches();
    }

    /**
     * Returns true if {@code phone} looks like a plausible international phone number
     * (7–20 digits/spaces/dashes/parens, optional leading +).
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    // ── Convenience throws ────────────────────────────────────────────────────

    /**
     * Throws {@link IllegalArgumentException} if {@code email} fails the format check.
     */
    public static void requireValidEmail(String email, String fieldName) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(fieldName + " is not a valid email address.");
        }
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code uuid} fails the format check.
     */
    public static void requireValidUuid(String uuid, String fieldName) {
        if (!isValidUuid(uuid)) {
            throw new IllegalArgumentException(fieldName + " is not a valid UUID.");
        }
    }
}