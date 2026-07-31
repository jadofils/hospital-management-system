package hospital.management.backend.config.security;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.EnvConfig;
import hospital.management.backend.exceptions.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWE (encrypted JWT) generation and validation for user session tokens.
 * Payload is AES-256-GCM encrypted — contents are opaque to any party without the key.
 *
 * Required .env keys:
 *   ENCRYPTION_KEY    — master secret; JWT key derived as SHA-256("jwt:" + key)
 *   JWT_EXPIRY_HOURS  — token lifetime in hours (default 8)
 *
 * Token claims:
 *   sub      — user_id (UUID string)
 *   username — login username or email
 *   role     — RBAC role name
 *   iat      — issued-at timestamp
 *   exp      — expiry timestamp
 *
 * Usage:
 *   String token    = JwtConfig.generateToken(userId, username, role);
 *   String userId   = JwtConfig.getUserId(token);
 *   String username = JwtConfig.getUsername(token);
 *   String role     = JwtConfig.getRole(token);
 *   boolean expired = JwtConfig.isExpired(token);
 */
public final class JwtConfig {

    private static final AppLogger logger = AppLogger.getLogger(JwtConfig.class);
    private static final SecretKey KEY;
    private static final long      EXPIRY_MS;

    static {
        KEY       = EncryptionConfig.getJwtKey();
        EXPIRY_MS = (long) EnvConfig.getJwtExpiryHours() * 60 * 60 * 1000;
        logger.info("JWT configured — expiry: " + EnvConfig.getJwtExpiryHours() + "h, algorithm: A256GCM (JWE)");
    }

    private JwtConfig() {}

    /**
     * Builds an encrypted JWE token embedding user identity and role.
     * The payload is AES-256-GCM encrypted — contents cannot be read without the key.
     *
     * @param userId   the user_id UUID string from the `users` table
     * @param username the login username or email address
     * @param role     the user's RBAC role name (e.g. "admin", "doctor")
     * @return compact JWE string safe to store in user_sessions or send to client
     */
    public static String generateToken(String userId, String username, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + EXPIRY_MS);
        return Jwts.builder()
                   .subject(userId)
                   .claim("username", username)
                   .claim("role", role)
                   .issuedAt(now)
                   .expiration(expiry)
                   .encryptWith(KEY, Jwts.KEY.DIRECT, Jwts.ENC.A256GCM)
                   .compact();
    }

    /**
     * Extracts the user_id from a token.
     * Throws {@link io.jsonwebtoken.JwtException} if the token is invalid or expired.
     */
    public static String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extracts the username/email claim embedded at token generation time. */
    public static String getUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    /** Extracts the RBAC role claim embedded at token generation time. */
    public static String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /** Returns true if the token's expiry time is in the past. */
    public static boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (TokenExpiredException e) {
            return true;
        }
    }

    /** Returns the expiry Date embedded in the token without throwing on expiry. */
    public static Date getExpiry(String token) {
        return parseClaims(token).getExpiration();
    }

    private static Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                       .decryptWith(KEY)
                       .build()
                       .parseEncryptedClaims(token)
                       .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (io.jsonwebtoken.JwtException e) {
            throw new hospital.management.backend.exceptions.AuthException(
                "Invalid session token: " + e.getMessage(), e);
        }
    }
}