package hospital.management.backend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SanitizeUtilsTest {

    // ── maskForLog ────────────────────────────────────────────────────────

    @Test
    @DisplayName("maskForLog returns null for null input")
    void maskForLog_null() {
        assertNull(SanitizeUtils.maskForLog(null));
    }

    @Test
    @DisplayName("maskForLog masks an email embedded in a larger message")
    void maskForLog_masksEmail() {
        String result = SanitizeUtils.maskForLog("login failed for john.doe@example.com");
        assertFalse(result.contains("john.doe@example.com"));
        assertTrue(result.contains("***"));
    }

    @Test
    @DisplayName("maskForLog masks a UUID embedded in a larger message")
    void maskForLog_masksUuid() {
        String result = SanitizeUtils.maskForLog("user id=550e8400-e29b-41d4-a716-446655440000 logged in");
        assertFalse(result.contains("550e8400-e29b-41d4-a716-446655440000"));
        assertTrue(result.contains("550e8400-***"));
    }

    @Test
    @DisplayName("maskForLog leaves a message with no PII unchanged")
    void maskForLog_noPii() {
        assertEquals("nothing sensitive here", SanitizeUtils.maskForLog("nothing sensitive here"));
    }

    // ── maskEmail ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("maskEmail masks the local part and domain, keeping the TLD")
    void maskEmail_masksParts() {
        String masked = SanitizeUtils.maskEmail("john.doe@example.com");
        assertFalse(masked.contains("john.doe"));
        assertTrue(masked.endsWith(".com"));
    }

    @Test
    @DisplayName("maskEmail returns *** for null or blank input")
    void maskEmail_nullOrBlank() {
        assertEquals("***", SanitizeUtils.maskEmail(null));
        assertEquals("***", SanitizeUtils.maskEmail(""));
        assertEquals("***", SanitizeUtils.maskEmail("   "));
    }

    // ── maskUuid ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("maskUuid keeps only the first 8 characters")
    void maskUuid_keepsPrefix() {
        assertEquals("550e8400-***", SanitizeUtils.maskUuid("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    @DisplayName("maskUuid returns *** for null, blank, or too-short input")
    void maskUuid_shortOrNull() {
        assertEquals("***", SanitizeUtils.maskUuid(null));
        assertEquals("***", SanitizeUtils.maskUuid(""));
        assertEquals("***", SanitizeUtils.maskUuid("short"));
    }

    // ── maskPhone ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("maskPhone keeps only the last 4 digits")
    void maskPhone_keepsLast4() {
        assertEquals("***-***-5309", SanitizeUtils.maskPhone("+1 (555) 867-5309"));
    }

    @Test
    @DisplayName("maskPhone returns *** for null or blank input")
    void maskPhone_nullOrBlank() {
        assertEquals("***", SanitizeUtils.maskPhone(null));
        assertEquals("***", SanitizeUtils.maskPhone(""));
    }

    // ── stripControlChars / clean ─────────────────────────────────────────

    @Test
    @DisplayName("stripControlChars removes a real control character but keeps tab/newline/CR")
    void stripControlChars_removesControlCharsOnly() {
        String input = "helloworld\ttab\nnewline\rcr";
        String result = SanitizeUtils.stripControlChars(input);
        assertEquals("helloworld\ttab\nnewline\rcr", result);
    }

    @Test
    @DisplayName("stripControlChars returns null for null input")
    void stripControlChars_null() {
        assertNull(SanitizeUtils.stripControlChars(null));
    }

    @Test
    @DisplayName("clean trims whitespace and strips control characters together")
    void clean_trimsAndStrips() {
        assertEquals("hello", SanitizeUtils.clean("  hello   "));
    }

    @Test
    @DisplayName("clean returns null for null input")
    void clean_null() {
        assertNull(SanitizeUtils.clean(null));
    }
}
