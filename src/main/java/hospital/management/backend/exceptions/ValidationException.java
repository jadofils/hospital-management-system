package hospital.management.backend.exceptions;

/**
 * Thrown when user-supplied input fails a format, range, or business-rule check.
 * Carries the offending field name so UI controllers can highlight the right control.
 */
public class ValidationException extends AppException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** The name of the field that failed validation (e.g. "email", "dateOfBirth"). */
    public String getField() {
        return field;
    }
}