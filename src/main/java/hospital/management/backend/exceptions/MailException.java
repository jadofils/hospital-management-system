package hospital.management.backend.exceptions;

/**
 * Checked exception for SMTP send failures.
 * Kept checked so callers can decide to retry, queue, or notify the user gracefully.
 */
public class MailException extends Exception {

    public MailException(String message) {
        super(message);
    }

    public MailException(String message, Throwable cause) {
        super(message, cause);
    }
}