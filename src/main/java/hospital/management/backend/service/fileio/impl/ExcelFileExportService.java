package hospital.management.backend.service.fileio.impl;

import hospital.management.backend.service.fileio.FileExportService;
import hospital.management.backend.service.fileio.FileProcessingException;
import hospital.management.backend.service.log.ServiceMongoLogger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ExcelFileExportService implements FileExportService {

    @Override
    public void exportTo(Path path, List<Map<String, Object>> rows) throws Exception {
        if (rows == null || rows.isEmpty()) {
            ServiceMongoLogger.warn("fileio.excel.export", "Skipped Excel export due to empty rows: " + path);
            return;
        }
        ServiceMongoLogger.info("fileio.excel.export", "Starting Excel export: " + path + " rows=" + rows.size());
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("export");
            Map<String, Object> first = rows.get(0);
            String[] header = first.keySet().toArray(new String[0]);
            int rownum = 0;
            Row hr = sheet.createRow(rownum++);
            for (int i = 0; i < header.length; i++) hr.createCell(i).setCellValue(header[i]);
            for (Map<String, Object> r : rows) {
                Row row = sheet.createRow(rownum++);
                for (int i = 0; i < header.length; i++) {
                    Cell c = row.createCell(i);
                    Object v = r.get(header[i]);
                    c.setCellValue(v == null ? "" : String.valueOf(v));
                }
            }
            try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                workbook.write(out);
            }
            ServiceMongoLogger.info("fileio.excel.export", "Excel export completed: " + path + " rows=" + rows.size());
        } catch (Exception e) {
            ServiceMongoLogger.error("fileio.excel.export", "Excel export failed: " + path, e);
            throw new FileProcessingException("Failed to export Excel: " + e.getMessage(), e);
        }
    }
}
