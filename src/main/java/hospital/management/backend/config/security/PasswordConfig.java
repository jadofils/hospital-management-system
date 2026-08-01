package hospital.management.backend.config.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.EnvConfig;
import hospital.management.backend.utils.ValidatorUtils;

/**
 * BCrypt password hashing and verification.
 *
 * Required .env key:
 *   BCRYPT_ROUNDS — cost factor 4–31 (12 recommended for production)
 *
 * The seed file (hospital_rbac_seed_postgresql.sql) uses cost factor 12 and the
 * $2b$ hash variant. at.favre.lib:bcrypt's verifyer() auto-detects and verifies
 * every bcrypt version (2a/2b/2x/2y) — unlike org.mindrot:jbcrypt (previously used
 * here), which is abandoned at 0.4 and only ever recognizes "$2a$", throwing
 * "Invalid salt revision" on the seeded $2b$ hashes.
 *
 * Usage:
 *   String hash   = PasswordConfig.hash("Password@12");
 *   boolean valid = PasswordConfig.verify("Password@12", hash);
 */
public final class PasswordConfig {

    private static final AppLogger logger = AppLogger.getLogger(PasswordConfig.class);
    private static final int       ROUNDS;

    static {
        ROUNDS = EnvConfig.getBcryptRounds();
        ValidatorUtils.requireRange(ROUNDS, 4, 31, "BCRYPT_ROUNDS");
        logger.info("BCrypt configured — cost factor: " + ROUNDS);
    }

    private PasswordConfig() {}

    /**
     * Hashes a plain-text password using BCrypt.
     * Always generates a unique salt internally — never call this twice and compare results.
     *
     * @param plainPassword the raw password from the user
     * @return a 60-character BCrypt hash safe to store in users.password_hash
     */
    public static String hash(String plainPassword) {
        ValidatorUtils.requireNonBlank(plainPassword, "password");
        return BCrypt.withDefaults().hashToString(ROUNDS, plainPassword.toCharArray());
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     * Constant-time comparison — safe against timing attacks. Accepts any
     * bcrypt version prefix ($2a$/$2b$/$2x$/$2y$) present in storedHash.
     *
     * @param plainPassword the raw input from the login form
     * @param storedHash    the value from users.password_hash in the database
     * @return true if the password matches
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), storedHash);
            return result.verified;
        } catch (IllegalArgumentException e) {
            logger.warn("BCrypt verification failed — invalid hash format: " + e.getMessage());
            return false;
        }
    }
}
