package hospital.management.backend.config.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    @Test
    @DisplayName("hash() then verify() succeeds for the original password")
    void hashThenVerify_succeedsForOriginalPassword() {
        String hash = PasswordConfig.hash("Sup3rSecret!");
        assertTrue(PasswordConfig.verify("Sup3rSecret!", hash));
    }

    @Test
    @DisplayName("verify() fails for a wrong password against a real hash")
    void verify_failsForWrongPassword() {
        String hash = PasswordConfig.hash("Sup3rSecret!");
        assertFalse(PasswordConfig.verify("wrong-password", hash));
    }

    @Test
    @DisplayName("hash() produces a different hash each time (unique salt) even for the same input")
    void hash_producesUniqueSaltPerCall() {
        String hash1 = PasswordConfig.hash("Sup3rSecret!");
        String hash2 = PasswordConfig.hash("Sup3rSecret!");
        assertNotEquals(hash1, hash2);
        // Both must still verify correctly despite differing.
        assertTrue(PasswordConfig.verify("Sup3rSecret!", hash1));
        assertTrue(PasswordConfig.verify("Sup3rSecret!", hash2));
    }

    @Test
    @DisplayName("hash() rejects a null or blank password")
    void hash_rejectsBlankPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordConfig.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordConfig.hash("   "));
    }

    @Test
    @DisplayName("verify() returns false (not an exception) for null password or hash")
    void verify_falseForNullInputs() {
        assertFalse(PasswordConfig.verify(null, "somehash"));
        assertFalse(PasswordConfig.verify("password", null));
        assertFalse(PasswordConfig.verify(null, null));
    }

    @Test
    @DisplayName("verify() returns false rather than throwing for a malformed stored hash")
    void verify_falseForMalformedHash() {
        assertFalse(PasswordConfig.verify("Sup3rSecret!", "not-a-real-bcrypt-hash"));
    }

    @Test
    @DisplayName("verify() accepts a real $2b$ hash — regression guard for the jbcrypt-vs-favre-lib "
            + "incompatibility (jbcrypt only recognized $2a$ and threw \"Invalid salt revision\" on "
            + "the $2b$ hashes the seeded RBAC data actually uses)")
    void verify_accepts2bVariant() {
        String real2bHash = BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(10, "Sup3rSecret!".toCharArray());
        assertTrue(real2bHash.startsWith("$2b$"), "test setup sanity check — expected a $2b$ hash but got: " + real2bHash);

        assertTrue(PasswordConfig.verify("Sup3rSecret!", real2bHash));
        assertFalse(PasswordConfig.verify("wrong-password", real2bHash));
    }
}
