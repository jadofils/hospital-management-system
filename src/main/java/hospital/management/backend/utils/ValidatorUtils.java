package hospital.management.backend.utils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
    private static final Pattern PASSWORD_STRONG =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$");
    private static final List<String> VALID_GENDERS =
        Arrays.asList("Male", "Female", "Other", "Prefer not to say");
    private static final int MIN_AGE_YEARS = 0;
    private static final int MAX_AGE_YEARS = 150;

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

    /**
     * Throws {@link IllegalArgumentException} if {@code phone} fails the format check.
     */
    public static void requireValidPhone(String phone, String fieldName) {
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException(
                fieldName + " is not a valid phone number. Expected format: +250 788 000 000.");
        }
    }

    // ── Password ──────────────────────────────────────────────────────────────

    /**
     * Returns true if the password meets minimum strength requirements:
     * at least 8 chars, one uppercase, one lowercase, one digit, one symbol.
     */
    public static boolean isPasswordStrong(String password) {
        return password != null && PASSWORD_STRONG.matcher(password).matches();
    }

    /**
     * Throws {@link IllegalArgumentException} if the password is too weak.
     */
    public static void requireStrongPassword(String password, String fieldName) {
        requireMinLength(password, 8, fieldName);
        if (!isPasswordStrong(password)) {
            throw new IllegalArgumentException(
                fieldName + " must contain uppercase, lowercase, digit, and special character.");
        }
    }

    // ── Date of birth ─────────────────────────────────────────────────────────

    /**
     * Returns true if {@code dob} is a plausible date of birth:
     * in the past and within MAX_AGE_YEARS years ago.
     */
    public static boolean isValidDateOfBirth(LocalDate dob) {
        if (dob == null) return false;
        LocalDate today = LocalDate.now();
        if (!dob.isBefore(today)) return false;
        return !dob.isBefore(today.minusYears(MAX_AGE_YEARS));
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code dob} is not a plausible date of birth.
     */
    public static void requireValidDateOfBirth(LocalDate dob, String fieldName) {
        if (dob == null) throw new IllegalArgumentException(fieldName + " is required.");
        if (!dob.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " must be a past date.");
        }
        if (dob.isBefore(LocalDate.now().minusYears(MAX_AGE_YEARS))) {
            throw new IllegalArgumentException(fieldName + " is not a plausible date of birth.");
        }
    }

    // ── Date range ────────────────────────────────────────────────────────────

    /**
     * Throws {@link IllegalArgumentException} if {@code from} is after {@code to}.
     * Either value may be null (range is open-ended when null).
     */
    public static void requireDateRange(LocalDate from, LocalDate to, String fromField, String toField) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                fromField + " must not be after " + toField + ".");
        }
    }

    // ── Gender ────────────────────────────────────────────────────────────────

    /**
     * Returns true if {@code gender} is one of the accepted values (case-sensitive).
     * Accepted: Male, Female, Other, Prefer not to say.
     */
    public static boolean isValidGender(String gender) {
        return gender != null && VALID_GENDERS.contains(gender);
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code gender} is not a valid value.
     */
    public static void requireValidGender(String gender, String fieldName) {
        if (!isValidGender(gender)) {
            throw new IllegalArgumentException(
                fieldName + " must be one of: " + String.join(", ", VALID_GENDERS) + ".");
        }
    }

    // ── Feedback rating ───────────────────────────────────────────────────────

    /**
     * Returns true if {@code rating} is between 1 and 5 (inclusive).
     */
    public static boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    /**
     * Throws {@link IllegalArgumentException} if {@code rating} is not in [1, 5].
     */
    public static void requireValidRating(int rating, String fieldName) {
        if (!isValidRating(rating)) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 5.");
        }
    }

    // ── Name / text ───────────────────────────────────────────────────────────

    /**
     * Validates a human name: non-blank, 1–100 chars, only letters/spaces/hyphens/apostrophes.
     */
    public static void requireValidName(String name, String fieldName) {
        requireNonBlank(name, fieldName);
        requireMaxLength(name, 100, fieldName);
        if (!name.matches("[\\p{L}\\s'\\-]+")) {
            throw new IllegalArgumentException(
                fieldName + " may only contain letters, spaces, hyphens, and apostrophes.");
        }
    }

    /**
     * Validates a non-blank string with a max length.
     */
    public static String requireNonBlankMaxLength(String value, int max, String fieldName) {
        String trimmed = requireNonBlank(value, fieldName);
        requireMaxLength(trimmed, max, fieldName);
        return trimmed;
    }
}