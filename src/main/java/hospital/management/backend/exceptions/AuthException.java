package hospital.management.backend.exceptions;

/**
 * Thrown when authentication fails — bad credentials, missing token, etc.
 * Subclass for more specific auth failures: {@link TokenExpiredException},
 * {@link UnauthorizedException}, {@link ForbiddenException}.
 */
public class AuthException extends AppException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}