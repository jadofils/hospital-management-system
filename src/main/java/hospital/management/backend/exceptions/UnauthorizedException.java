package hospital.management.backend.exceptions;

/** Thrown when a request is made without a valid authenticated session. */
public class UnauthorizedException extends AuthException {

    public UnauthorizedException() {
        super("Authentication required. Please log in.");
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}