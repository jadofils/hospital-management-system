package hospital.management.backend.exceptions;

/**
 * Base unchecked exception for all application-level failures.
 * Subclass this instead of throwing raw RuntimeException so callers
 * can catch the entire app family with a single catch clause.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}