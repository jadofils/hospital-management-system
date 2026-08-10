package hospital.management.backend.service.fileio.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hospital.management.backend.service.fileio.FileImportService;
import hospital.management.backend.service.fileio.FileProcessingException;
import hospital.management.backend.service.log.ServiceMongoLogger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JsonFileImportService implements FileImportService {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void importFile(Path path, Consumer<java.util.Map<String, String>> recordConsumer) throws Exception {
        ServiceMongoLogger.info("fileio.json.import", "Starting JSON import: " + path);
        int processed = 0;
        try {
            byte[] bytes = Files.readAllBytes(path);
            List<Map<String, Object>> rows = mapper.readValue(bytes, new TypeReference<List<Map<String, Object>>>(){});
            for (Map<String, Object> r : rows) {
                java.util.Map<String, String> flat = new java.util.HashMap<>();
                r.forEach((k,v) -> flat.put(k, v == null ? null : String.valueOf(v)));
                recordConsumer.accept(flat);
                processed++;
            }
            ServiceMongoLogger.info("fileio.json.import", "JSON import completed: " + path + " records=" + processed);
        } catch (Exception e) {
            ServiceMongoLogger.error("fileio.json.import", "JSON import failed: " + path, e);
            throw new FileProcessingException("Failed to import JSON: " + e.getMessage(), e);
        }
    }
}
