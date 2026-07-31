package hospital.management.backend.exceptions;

/**
 * Checked exception for SQL and connection-pool failures.
 * Kept checked so service/DAO callers are forced to decide whether to
 * wrap in a transaction rollback, retry, or surface to the user.
 */
public class DatabaseException extends Exception {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}