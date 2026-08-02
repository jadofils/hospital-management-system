package hospital.management.backend.config.security;

import hospital.management.backend.exceptions.EncryptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionConfigTest {

    @Test
    @DisplayName("encrypt() then decrypt() round-trips to the original plaintext")
    void encryptThenDecrypt_roundTrips() {
        String plaintext = "sensitive patient note";
        String cipher = EncryptionConfig.encrypt(plaintext);
        assertEquals(plaintext, EncryptionConfig.decrypt(cipher));
    }

    @Test
    @DisplayName("encrypt() of the same plaintext twice produces different ciphertexts (fresh IV each call)")
    void encrypt_producesDifferentCiphertextEachCall() {
        String plaintext = "repeat me";
        String cipher1 = EncryptionConfig.encrypt(plaintext);
        String cipher2 = EncryptionConfig.encrypt(plaintext);
        assertNotEquals(cipher1, cipher2);
        assertEquals(plaintext, EncryptionConfig.decrypt(cipher1));
        assertEquals(plaintext, EncryptionConfig.decrypt(cipher2));
    }

    @Test
    @DisplayName("encrypt() rejects a null or blank plaintext")
    void encrypt_rejectsBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> EncryptionConfig.encrypt(null));
        assertThrows(IllegalArgumentException.class, () -> EncryptionConfig.encrypt("   "));
    }

    @Test
    @DisplayName("decrypt() rejects a null or blank input")
    void decrypt_rejectsBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> EncryptionConfig.decrypt(null));
        assertThrows(IllegalArgumentException.class, () -> EncryptionConfig.decrypt("   "));
    }

    @Test
    @DisplayName("decrypt() throws EncryptionException (not a raw crypto exception) on tampered/garbage ciphertext")
    void decrypt_throwsEncryptionExceptionOnTamperedInput() {
        assertThrows(EncryptionException.class, () -> EncryptionConfig.decrypt("not-valid-base64-ciphertext!!"));
    }

    @Test
    @DisplayName("decrypt() throws EncryptionException when the ciphertext has been bit-flipped (GCM auth-tag check fails)")
    void decrypt_throwsOnBitFlippedCiphertext() {
        String cipher = EncryptionConfig.encrypt("authentic message");
        // Flip a character in the middle of the Base64URL payload to corrupt it while staying valid Base64URL-ish.
        char[] chars = cipher.toCharArray();
        int mid = chars.length / 2;
        chars[mid] = chars[mid] == 'A' ? 'B' : 'A';
        String tampered = new String(chars);

        assertThrows(EncryptionException.class, () -> EncryptionConfig.decrypt(tampered));
    }

    @Test
    @DisplayName("getJwtKey() returns a non-null 256-bit AES key, stable across calls")
    void getJwtKey_returnsStableKey() {
        assertNotNull(EncryptionConfig.getJwtKey());
        assertEquals(EncryptionConfig.getJwtKey(), EncryptionConfig.getJwtKey());
        assertEquals("AES", EncryptionConfig.getJwtKey().getAlgorithm());
    }
}
