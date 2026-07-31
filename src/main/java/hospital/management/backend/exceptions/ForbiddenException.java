package hospital.management.backend.exceptions;

/**
 * Thrown when an authenticated user attempts an action their role does not permit.
 * Distinct from {@link UnauthorizedException} — the user is known, but access is denied.
 */
public class ForbiddenException extends AuthException {

    public ForbiddenException(String action) {
        super("Access denied: your role does not have permission to perform '" + action + "'.");
    }

    public ForbiddenException(String action, String role) {
        super("Access denied: role '" + role + "' cannot perform '" + action + "'.");
    }
}