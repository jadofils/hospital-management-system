package hospital.management.backend.exceptions;

/**
 * Thrown when AES-GCM encryption or decryption fails.
 * A decryption failure usually means the data was tampered with or the key changed.
 */
public class EncryptionException extends AppException {

    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}