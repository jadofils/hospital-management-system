package hospital.management.backend.config.security;

import hospital.management.backend.exceptions.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {

    @Test
    @DisplayName("generateToken() then getUserId/getUsername/getRole round-trip the embedded claims")
    void generateToken_roundTripsClaims() {
        String token = JwtConfig.generateToken("user-123", "jane.doe", "Doctor");

        assertEquals("user-123", JwtConfig.getUserId(token));
        assertEquals("jane.doe", JwtConfig.getUsername(token));
        assertEquals("Doctor", JwtConfig.getRole(token));
    }

    @Test
    @DisplayName("generateToken() with a 3-arg role also round-trips a single-element roles list")
    void generateToken_threeArg_roundTripsSingleRoleList() {
        String token = JwtConfig.generateToken("user-123", "jane.doe", "Doctor");

        assertEquals(List.of("Doctor"), JwtConfig.getRoles(token));
    }

    @Test
    @DisplayName("generateToken() with an explicit roles list round-trips every role, and primary role separately")
    void generateToken_fourArg_roundTripsAllRoles() {
        String token = JwtConfig.generateToken("user-123", "jane.doe", "Doctor", List.of("Doctor", "Admin"));

        assertEquals("Doctor", JwtConfig.getRole(token));
        assertEquals(List.of("Doctor", "Admin"), JwtConfig.getRoles(token));
    }

    @Test
    @DisplayName("A freshly generated token is not expired and its expiry is in the future")
    void freshToken_isNotExpired() {
        String token = JwtConfig.generateToken("user-123", "jane.doe", "Doctor");

        assertFalse(JwtConfig.isExpired(token));
        assertTrue(JwtConfig.getExpiry(token).after(new Date()));
    }

    @Test
    @DisplayName("The token payload is opaque — its compact string never contains the plaintext claims")
    void token_payloadIsEncrypted() {
        String token = JwtConfig.generateToken("user-123", "jane.doe", "SuperSecretRole");
        assertFalse(token.contains("jane.doe"));
        assertFalse(token.contains("SuperSecretRole"));
        assertFalse(token.contains("user-123"));
    }

    @Test
    @DisplayName("A malformed token string throws AuthException rather than a raw JWT library exception")
    void malformedToken_throwsAuthException() {
        assertThrows(AuthException.class, () -> JwtConfig.getUserId("not-a-real-token"));
    }

    @Test
    @DisplayName("A tampered (bit-flipped) token fails decryption with AuthException")
    void tamperedToken_throwsAuthException() {
        String token = JwtConfig.generateToken("user-123", "jane.doe", "Doctor");
        char[] chars = token.toCharArray();
        int mid = chars.length / 2;
        chars[mid] = chars[mid] == 'A' ? 'B' : 'A';
        String tampered = new String(chars);

        assertThrows(AuthException.class, () -> JwtConfig.getUserId(tampered));
    }
}
