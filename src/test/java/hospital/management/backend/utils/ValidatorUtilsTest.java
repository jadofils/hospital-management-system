package hospital.management.backend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorUtilsTest {

    // ── requireNonBlank ──────────────────────────────────────────────────

    @Test
    @DisplayName("requireNonBlank trims and returns a valid value")
    void requireNonBlank_returnsTrimmedValue() {
        assertEquals("hello", ValidatorUtils.requireNonBlank("  hello  ", "field"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "   "})
    @DisplayName("requireNonBlank rejects null, empty, and blank values")
    void requireNonBlank_rejectsBlank(String value) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidatorUtils.requireNonBlank(value, "username"));
        assertTrue(ex.getMessage().contains("username"));
    }

    // ── requireMinLength / requireMaxLength ──────────────────────────────

    @Test
    @DisplayName("requireMinLength accepts a value meeting the minimum")
    void requireMinLength_accepts() {
        assertDoesNotThrow(() -> ValidatorUtils.requireMinLength("password", 8, "password"));
    }

    @Test
    @DisplayName("requireMinLength rejects a value shorter than the minimum")
    void requireMinLength_rejectsShort() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidatorUtils.requireMinLength("short", 8, "password"));
    }

    @Test
    @DisplayName("requireMinLength rejects null as shorter than any positive minimum")
    void requireMinLength_rejectsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidatorUtils.requireMinLength(null, 8, "password"));
    }

    @Test
    @DisplayName("requireMaxLength accepts a null value (nothing to exceed)")
    void requireMaxLength_acceptsNull() {
        assertDoesNotThrow(() -> ValidatorUtils.requireMaxLength(null, 10, "notes"));
    }

    @Test
    @DisplayName("requireMaxLength rejects a value longer than the maximum")
    void requireMaxLength_rejectsLong() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidatorUtils.requireMaxLength("this is way too long", 5, "notes"));
    }

    // ── requireRange ──────────────────────────────────────────────────────

    @Test
    @DisplayName("requireRange accepts a value within bounds, inclusive")
    void requireRange_acceptsInclusiveBounds() {
        assertDoesNotThrow(() -> ValidatorUtils.requireRange(4, 4, 31, "BCRYPT_ROUNDS"));
        assertDoesNotThrow(() -> ValidatorUtils.requireRange(31, 4, 31, "BCRYPT_ROUNDS"));
    }

    @ParameterizedTest
    @CsvSource({"3", "32"})
    @DisplayName("requireRange rejects values outside bounds")
    void requireRange_rejectsOutOfBounds(int value) {
        assertThrows(IllegalStateException.class,
                () -> ValidatorUtils.requireRange(value, 4, 31, "BCRYPT_ROUNDS"));
    }

    // ── Email ─────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "first.last@sub.domain.co", "a@b.io"})
    @DisplayName("isValidEmail accepts well-formed addresses")
    void isValidEmail_acceptsValid(String email) {
        assertTrue(ValidatorUtils.isValidEmail(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-an-email", "missing@domain", "@nodomain.com", "spaces in@email.com"})
    @DisplayName("isValidEmail rejects malformed addresses")
    void isValidEmail_rejectsInvalid(String email) {
        assertFalse(ValidatorUtils.isValidEmail(email));
    }

    @Test
    @DisplayName("requireValidEmail throws with a message naming the field on invalid input")
    void requireValidEmail_throwsOnInvalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidatorUtils.requireValidEmail("not-an-email", "email"));
        assertTrue(ex.getMessage().contains("email"));
    }

    // ── UUID ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isValidUuid accepts a canonical UUID")
    void isValidUuid_acceptsCanonical() {
        assertTrue(ValidatorUtils.isValidUuid("550e8400-e29b-41d4-a716-446655440000"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-uuid", "550e8400e29b41d4a716446655440000", "550e8400-e29b-41d4-a716"})
    @DisplayName("isValidUuid rejects malformed values")
    void isValidUuid_rejectsInvalid(String uuid) {
        assertFalse(ValidatorUtils.isValidUuid(uuid));
    }

    @Test
    @DisplayName("requireValidUuid throws on malformed input")
    void requireValidUuid_throwsOnInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidatorUtils.requireValidUuid("bad-uuid", "patientId"));
    }

    // ── Phone ─────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"+1 555 867 5309", "(555) 867-5309", "555-867-5309", "+254712345678"})
    @DisplayName("isValidPhone accepts plausible phone formats")
    void isValidPhone_acceptsValid(String phone) {
        assertTrue(ValidatorUtils.isValidPhone(phone));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "123"})
    @DisplayName("isValidPhone rejects non-phone-like input")
    void isValidPhone_rejectsInvalid(String phone) {
        assertFalse(ValidatorUtils.isValidPhone(phone));
    }
}
