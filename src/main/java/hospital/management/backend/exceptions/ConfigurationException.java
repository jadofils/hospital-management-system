package hospital.management.backend.exceptions;

/**
 * Thrown during application startup when a required .env key is missing,
 * out of range, or causes a dependency (Cloudinary, SMTP, DB) to fail initialization.
 */
public class ConfigurationException extends AppException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}