package hospital.management.backend.service.fileio.impl;

import com.opencsv.CSVReader;
import hospital.management.backend.service.fileio.FileImportService;
import hospital.management.backend.service.fileio.FileProcessingException;
import hospital.management.backend.service.log.ServiceMongoLogger;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Consumer;

public class CsvFileImportService implements FileImportService {

    @Override
    public void importFile(Path path, Consumer<java.util.Map<String, String>> recordConsumer) throws Exception {
        ServiceMongoLogger.info("fileio.csv.import", "Starting CSV import: " + path);
        int processed = 0;
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVReader reader = new CSVReader(br)) {
            String[] header = reader.readNext();
            if (header == null) return;
            String[] line;
            while ((line = reader.readNext()) != null) {
                java.util.Map<String, String> map = new HashMap<>();
                for (int i = 0; i < Math.min(header.length, line.length); i++) map.put(header[i], line[i]);
                recordConsumer.accept(map);
                processed++;
            }
            ServiceMongoLogger.info("fileio.csv.import", "CSV import completed: " + path + " records=" + processed);
        } catch (Exception e) {
            ServiceMongoLogger.error("fileio.csv.import", "CSV import failed: " + path, e);
            throw new FileProcessingException("Failed to import CSV: " + e.getMessage(), e);
        }
    }
}
