package hospital.management.pages.utils;

import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvUiIOTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("ensureCsvName appends .csv and provides a default export name")
    void ensureCsvName_appendsExtensionOrDefault() throws Exception {
        assertEquals("report.csv", invokeString("ensureCsvName", "report"));
        assertEquals("already.csv", invokeString("ensureCsvName", "already.csv"));
        assertEquals("export.csv", invokeString("ensureCsvName", "   "));
    }

    @Test
    @DisplayName("determineExportFormat prefers file extension and falls back to selected filter")
    void determineExportFormat_detectsExpectedFormat() throws Exception {
        Method method = method("determineExportFormat", File.class, FileChooser.ExtensionFilter.class);

        String byPdfExt = (String) method.invoke(null,
                new File(tempDir.toFile(), "analytics.pdf"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        assertEquals("pdf", byPdfExt);

        String byExcelFilter = (String) method.invoke(null,
                new File(tempDir.toFile(), "analytics"),
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        assertEquals("xlsx", byExcelFilter);

        String byDefault = (String) method.invoke(null,
                new File(tempDir.toFile(), "analytics"),
                null);
        assertEquals("csv", byDefault);
    }

    @Test
    @DisplayName("importExcelRows reads sheet headers and values and skips empty data rows")
    @SuppressWarnings("unchecked")
    void importExcelRows_parsesWorkbook() throws Exception {
        File excel = tempDir.resolve("patients.xlsx").toFile();
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(excel)) {
            var sheet = workbook.createSheet("Export");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("patient_id");
            header.createCell(1).setCellValue("name");

            var data = sheet.createRow(1);
            data.createCell(0).setCellValue("p-1");
            data.createCell(1).setCellValue("Alice");

            sheet.createRow(2); // empty row should be skipped
            workbook.write(out);
        }

        Method method = method("importExcelRows", File.class);
        List<Map<String, String>> rows = (List<Map<String, String>>) method.invoke(null, excel);

        assertEquals(1, rows.size());
        assertEquals("p-1", rows.get(0).get("patient_id"));
        assertEquals("Alice", rows.get(0).get("name"));
    }

    @Test
    @DisplayName("exportToExcel writes xlsx content with header and row values")
    void exportToExcel_createsWorkbookFile() throws Exception {
        File output = tempDir.resolve("export").toFile();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("invoice_id", "inv-1");
        row.put("status", "paid");

        Method method = method("exportToExcel", File.class, List.class);
        method.invoke(null, output, List.of(row));

        File written = tempDir.resolve("export.xlsx").toFile();
        assertTrue(written.exists());

        try (FileInputStream in = new FileInputStream(written); Workbook workbook = new XSSFWorkbook(in)) {
            var sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            assertEquals("invoice_id", formatter.formatCellValue(sheet.getRow(0).getCell(0)));
            assertEquals("status", formatter.formatCellValue(sheet.getRow(0).getCell(1)));
            assertEquals("inv-1", formatter.formatCellValue(sheet.getRow(1).getCell(0)));
            assertEquals("paid", formatter.formatCellValue(sheet.getRow(1).getCell(1)));
        }
    }

    private static String invokeString(String methodName, String value) throws Exception {
        Method method = method(methodName, String.class);
        return (String) method.invoke(null, value);
    }

    private static Method method(String name, Class<?>... args) throws Exception {
        Method method = CsvUiIO.class.getDeclaredMethod(name, args);
        method.setAccessible(true);
        return method;
    }
}
