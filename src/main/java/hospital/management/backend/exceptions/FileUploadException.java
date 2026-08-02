package hospital.management.backend.exceptions;

/**
 * Thrown when a file upload is rejected — size limit exceeded, unsupported type,
 * or the Cloudinary API call fails.
 */
public class FileUploadException extends AppException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}