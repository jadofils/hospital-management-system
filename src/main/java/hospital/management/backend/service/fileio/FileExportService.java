package hospital.management.backend.service.fileio;

import java.nio.file.Path;
import java.util.List;

/**
 * Single-responsibility exporter interface. Implementations produce a file in a specific format.
 */
public interface FileExportService {
    /**
     * Export provided rows to given path. Each row is a map from column->value.
     */
    void exportTo(Path path, List<java.util.Map<String, Object>> rows) throws Exception;
}
