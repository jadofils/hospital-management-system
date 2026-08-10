package hospital.management.backend.service.fileio.impl;

import com.opencsv.CSVWriter;
import hospital.management.backend.service.fileio.FileExportService;
import hospital.management.backend.service.fileio.FileProcessingException;
import hospital.management.backend.service.log.ServiceMongoLogger;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CsvFileExportService implements FileExportService {

    @Override
    public void exportTo(Path path, List<Map<String, Object>> rows) throws Exception {
        if (rows == null || rows.isEmpty()) {
            ServiceMongoLogger.warn("fileio.csv.export", "Skipped CSV export due to empty rows: " + path);
            return;
        }
        ServiceMongoLogger.info("fileio.csv.export", "Starting CSV export: " + path + " rows=" + rows.size());
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(bw)) {
            // header
            Map<String, Object> first = rows.get(0);
            String[] header = first.keySet().toArray(new String[0]);
            writer.writeNext(header);
            for (Map<String, Object> r : rows) {
                String[] line = new String[header.length];
                for (int i = 0; i < header.length; i++) line[i] = r.get(header[i]) == null ? "" : String.valueOf(r.get(header[i]));
                writer.writeNext(line);
            }
            ServiceMongoLogger.info("fileio.csv.export", "CSV export completed: " + path + " rows=" + rows.size());
        } catch (Exception e) {
            ServiceMongoLogger.error("fileio.csv.export", "CSV export failed: " + path, e);
            throw new FileProcessingException("Failed to export CSV: " + e.getMessage(), e);
        }
    }
}
