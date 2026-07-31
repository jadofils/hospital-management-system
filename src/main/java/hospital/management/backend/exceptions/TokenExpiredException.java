package hospital.management.backend.exceptions;

/** Thrown when a JWT session token has passed its expiry time. */
public class TokenExpiredException extends AuthException {

    public TokenExpiredException() {
        super("Session token has expired. Please log in again.");
    }

    public TokenExpiredException(String message) {
        super(message);
    }
}