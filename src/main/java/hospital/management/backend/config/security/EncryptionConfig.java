package hospital.management.backend.config.security;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.EnvConfig;
import hospital.management.backend.exceptions.ConfigurationException;
import hospital.management.backend.exceptions.EncryptionException;
import hospital.management.backend.utils.ValidatorUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM symmetric encryption for sensitive data at rest and in transit.
 *
 * Required .env key:
 *   ENCRYPTION_KEY — at least 32 characters (master secret)
 *
 * Key derivation:
 *   data key = SHA-256("data:" + ENCRYPTION_KEY)   — used by encrypt/decrypt
 *   jwt  key = SHA-256("jwt:"  + ENCRYPTION_KEY)   — used by JwtConfig for JWE
 *
 * Wire format (encrypt output):
 *   Base64URL( IV[12 bytes] || ciphertext+GCM-tag[16 bytes] )
 *
 * Usage:
 *   String cipher = EncryptionConfig.encrypt("sensitive text");
 *   String plain  = EncryptionConfig.decrypt(cipher);
 *   String safe   = SanitizeUtils.maskForLog("token for user@example.com id=550e8400-...");
 */
public final class EncryptionConfig {

    private static final AppLogger logger  = AppLogger.getLogger(EncryptionConfig.class);
    private static final SecretKey    DATA_KEY;
    private static final SecretKey    JWT_KEY;
    private static final SecureRandom RANDOM  = new SecureRandom();

    private static final int GCM_IV_LEN  = 12;
    private static final int GCM_TAG_LEN = 128;

    static {
        String master = EnvConfig.getEncryptionKey();
        ValidatorUtils.requireNonBlank(master, "ENCRYPTION_KEY");
        ValidatorUtils.requireMinLength(master, 32, "ENCRYPTION_KEY");
        DATA_KEY = deriveKey("data:", master);
        JWT_KEY  = deriveKey("jwt:",  master);
        logger.info("EncryptionConfig initialised — AES-256-GCM, domain-separated keys");
    }

    private EncryptionConfig() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Encrypts a plaintext string with AES-256-GCM using the data key.
     * Each call generates a fresh 12-byte IV, so identical plaintexts produce different ciphertexts.
     *
     * @param plaintext value to encrypt (must not be null)
     * @return Base64URL-encoded string: IV || ciphertext+tag
     */
    public static String encrypt(String plaintext) {
        ValidatorUtils.requireNonBlank(plaintext, "plaintext");
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, DATA_KEY, new GCMParameterSpec(GCM_TAG_LEN, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] output = new byte[GCM_IV_LEN + cipherBytes.length];
            System.arraycopy(iv, 0, output, 0, GCM_IV_LEN);
            System.arraycopy(cipherBytes, 0, output, GCM_IV_LEN, cipherBytes.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(output);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a value produced by {@link #encrypt(String)}.
     *
     * @param encoded Base64URL string from encrypt()
     * @return original plaintext
     * @throws RuntimeException if the ciphertext is tampered or the key is wrong
     */
    public static String decrypt(String encoded) {
        ValidatorUtils.requireNonBlank(encoded, "encoded");
        try {
            byte[] input = Base64.getUrlDecoder().decode(encoded);
            byte[] iv    = new byte[GCM_IV_LEN];
            System.arraycopy(input, 0, iv, 0, GCM_IV_LEN);
            byte[] cipherBytes = new byte[input.length - GCM_IV_LEN];
            System.arraycopy(input, GCM_IV_LEN, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, DATA_KEY, new GCMParameterSpec(GCM_TAG_LEN, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed — data may be tampered", e);
        }
    }

    /**
     * Returns the 256-bit AES key for use by {@link JwtConfig} to encrypt JWT payloads.
     * Never log or expose this value.
     */
    public static SecretKey getJwtKey() {
        return JWT_KEY;
    }

    // ── Key derivation ────────────────────────────────────────────────────────

    private static SecretKey deriveKey(String domain, String master) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(
                (domain + master).getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new ConfigurationException("Key derivation failed for domain '" + domain + "'", e);
        }
    }
}