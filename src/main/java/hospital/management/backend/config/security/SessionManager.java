package hospital.management.backend.config.security;

import hospital.management.backend.exceptions.TokenExpiredException;
import hospital.management.backend.exceptions.UnauthorizedException;

/**
 * In-memory session state for the currently logged-in user.
 *
 * In a single-user JavaFX desktop app, exactly one person uses the app at a time.
 * SessionManager holds their identity so every controller can call
 * SessionManager.getCurrentUserId() instead of passing userId as a parameter
 * through every method call.
 *
 * Usage:
 *   SessionManager.login(token);               // called by AuthPageController on success
 *   SessionManager.getCurrentUserId();         // called by any controller that needs the user
 *   SessionManager.requireLoggedIn();          // guard at the top of sensitive operations
 *   SessionManager.logout();                   // called by the logout button / session expiry
 */
public final class SessionManager {

    private static String currentUserId;
    private static String currentUsername;
    private static String currentRole;
    private static String currentToken;

    private SessionManager() {}

    // ── Login / Logout ────────────────────────────────────────────────────────

    /**
     * Establishes a session from a validated JWE token.
     * All identity fields are extracted from the token — no separate parameters needed.
     *
     * @param token compact JWE string returned by JwtConfig.generateToken()
     * @throws TokenExpiredException if the token is already expired at login time
     */
    public static void login(String token) {
        currentUserId   = JwtConfig.getUserId(token);
        currentUsername = JwtConfig.getUsername(token);
        currentRole     = JwtConfig.getRole(token);
        currentToken    = token;
    }

    /** Clears all session state. Call on logout button or when a token expires mid-session. */
    public static void logout() {
        currentUserId   = null;
        currentUsername = null;
        currentRole     = null;
        currentToken    = null;
    }

    // ── Session state ─────────────────────────────────────────────────────────

    /**
     * Returns true if a token is stored AND it has not yet expired.
     * Use this for conditional UI (show logout button, hide login form).
     */
    public static boolean isLoggedIn() {
        if (currentToken == null) return false;
        try {
            return !JwtConfig.isExpired(currentToken);
        } catch (TokenExpiredException e) {
            logout();
            return false;
        }
    }

    // ── Guarded accessors ─────────────────────────────────────────────────────

    /**
     * Returns the current user's UUID.
     * @throws UnauthorizedException if no session is active
     */
    public static String getCurrentUserId() {
        requireLoggedIn();
        return currentUserId;
    }

    /**
     * Returns the current user's username or email.
     * @throws UnauthorizedException if no session is active
     */
    public static String getCurrentUsername() {
        requireLoggedIn();
        return currentUsername;
    }

    /**
     * Returns the current user's RBAC role name (e.g. "admin", "doctor").
     * @throws UnauthorizedException if no session is active
     */
    public static String getCurrentRole() {
        requireLoggedIn();
        return currentRole;
    }

    /**
     * Returns the raw JWE token — pass to APIs or store in user_sessions table.
     * @throws UnauthorizedException if no session is active
     */
    public static String getCurrentToken() {
        requireLoggedIn();
        return currentToken;
    }

    // ── Guard ─────────────────────────────────────────────────────────────────

    /**
     * Throws {@link UnauthorizedException} if no session is active.
     * Call this at the top of any controller method that requires authentication.
     */
    public static void requireLoggedIn() {
        if (!isLoggedIn()) throw new UnauthorizedException();
    }
}