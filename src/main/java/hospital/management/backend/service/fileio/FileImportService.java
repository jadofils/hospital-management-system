package hospital.management.backend.service.fileio;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Single-responsibility importer interface. Implementations handle a single format.
 */
public interface FileImportService {
    /**
     * Import file at path and consume parsed records. Implementations must handle IO and parsing errors
     * and translate them into FileProcessingException.
     */
    void importFile(Path path, Consumer<java.util.Map<String, String>> recordConsumer) throws Exception;
}
