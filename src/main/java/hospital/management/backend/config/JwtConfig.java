package hospital.management.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT generation and validation for user session tokens.
 *
 * Required .env keys:
 *   JWT_SECRET        — at least 32 characters; change before production
 *   JWT_EXPIRY_HOURS  — token lifetime in hours (default 8)
 *
 * Token payload:
 *   sub  — user_id (UUID string)
 *   iat  — issued-at timestamp
 *   exp  — expiry timestamp
 *
 * Usage:
 *   String token  = JwtConfig.generateToken(userId);
 *   String userId = JwtConfig.getUserId(token);   // throws if invalid/expired
 *   boolean ok    = JwtConfig.isExpired(token);
 */
public final class JwtConfig {

    private static final AppLogger  logger = AppLogger.getLogger(JwtConfig.class);
    private static final SecretKey  KEY;
    private static final long       EXPIRY_MS;

    static {
        String secret = EnvConfig.getJwtSecret();
        if (secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least 32 characters. Update your .env file.");
        }
        KEY       = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        EXPIRY_MS = (long) EnvConfig.getJwtExpiryHours() * 60 * 60 * 1000;
        logger.info("JWT configured — expiry: " + EnvConfig.getJwtExpiryHours() + "h, algorithm: HS256");
    }

    private JwtConfig() {}

    /**
     * Builds a signed HS256 JWT embedding the user's UUID as the subject.
     *
     * @param userId the user_id UUID string from the `users` table
     * @return compact JWT string to store in user_sessions.session_id or send to client
     */
    public static String generateToken(String userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + EXPIRY_MS);
        return Jwts.builder()
                   .subject(userId)
                   .issuedAt(now)
                   .expiration(expiry)
                   .signWith(KEY)
                   .compact();
    }

    /**
     * Extracts the user_id from a token.
     * Throws {@link io.jsonwebtoken.JwtException} if the token is invalid or expired.
     */
    public static String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns true if the token's expiry time is in the past. */
    public static boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        }
    }

    /** Returns the expiry Date embedded in the token without throwing on expiry. */
    public static Date getExpiry(String token) {
        return parseClaims(token).getExpiration();
    }

    private static Claims parseClaims(String token) {
        return Jwts.parser()
                   .verifyWith(KEY)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }
}