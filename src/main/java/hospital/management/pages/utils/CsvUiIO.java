package hospital.management.pages.utils;

import hospital.management.backend.service.fileio.FileExportService;
import hospital.management.backend.service.fileio.FileImportService;
import hospital.management.backend.service.fileio.impl.CsvFileExportService;
import hospital.management.backend.service.fileio.impl.CsvFileImportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CsvUiIO {

    private static final FileExportService EXPORTER = new CsvFileExportService();
    private static final FileImportService IMPORTER = new CsvFileImportService();

    private CsvUiIO() {
    }

    public static boolean exportRows(Window owner, String initialFileName, List<Map<String, Object>> rows) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Data");
        FileChooser.ExtensionFilter csvFilter = new FileChooser.ExtensionFilter("CSV Files", "*.csv");
        FileChooser.ExtensionFilter xlsxFilter = new FileChooser.ExtensionFilter("Excel Files", "*.xlsx");
        FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF Files", "*.pdf");
        chooser.getExtensionFilters().addAll(csvFilter, xlsxFilter, pdfFilter);
        chooser.setSelectedExtensionFilter(csvFilter);
        chooser.setInitialFileName(ensureCsvName(initialFileName));
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return false;
        }

        String format = determineExportFormat(file, chooser.getSelectedExtensionFilter());
        switch (format) {
            case "xlsx" -> exportToExcel(file, rows);
            case "pdf" -> exportToPdf(file, rows);
            default -> EXPORTER.exportTo(ensureExtension(file, ".csv").toPath(), rows);
        }
        return true;
    }

    public static List<Map<String, String>> importRows(Window owner, String title) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return List.of();
        }

        String name = file.getName().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return importExcelRows(file);
        }

        List<Map<String, String>> rows = new ArrayList<>();
        IMPORTER.importFile(file.toPath(), rows::add);
        return rows;
    }

    private static String ensureCsvName(String initialFileName) {
        if (initialFileName == null || initialFileName.isBlank()) {
            return "export.csv";
        }
        return initialFileName.toLowerCase().endsWith(".csv") ? initialFileName : initialFileName + ".csv";
    }

    private static String determineExportFormat(File file, FileChooser.ExtensionFilter filter) {
        String lower = file.getName().toLowerCase();
        if (lower.endsWith(".xlsx")) return "xlsx";
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".csv")) return "csv";
        if (filter != null && filter.getDescription().toLowerCase().contains("excel")) return "xlsx";
        if (filter != null && filter.getDescription().toLowerCase().contains("pdf")) return "pdf";
        return "csv";
    }

    private static File ensureExtension(File file, String extension) {
        String lower = file.getName().toLowerCase();
        if (lower.endsWith(extension)) return file;
        return new File(file.getParentFile(), file.getName() + extension);
    }

    private static void exportToExcel(File file, List<Map<String, Object>> rows) throws Exception {
        if (rows == null || rows.isEmpty()) return;
        File target = ensureExtension(file, ".xlsx");

        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(target)) {
            Sheet sheet = workbook.createSheet("Export");
            List<String> headers = new ArrayList<>(rows.get(0).keySet());

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Map<String, Object> data = rows.get(r);
                for (int c = 0; c < headers.size(); c++) {
                    Object value = data.get(headers.get(c));
                    row.createCell(c).setCellValue(value == null ? "" : String.valueOf(value));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
        }
    }

    private static void exportToPdf(File file, List<Map<String, Object>> rows) throws Exception {
        if (rows == null || rows.isEmpty()) return;
        File target = ensureExtension(file, ".pdf");
        List<String> headers = new ArrayList<>(rows.get(0).keySet());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float y = 730;
            float leading = 14;

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(font, 10);
                stream.setLeading(leading);
                stream.newLineAtOffset(40, y);

                stream.showText(joinAndTrim(headers));
                stream.newLine();
                for (Map<String, Object> row : rows) {
                    List<String> values = new ArrayList<>();
                    for (String header : headers) {
                        Object value = row.get(header);
                        values.add(value == null ? "" : String.valueOf(value));
                    }
                    stream.showText(joinAndTrim(values));
                    stream.newLine();
                }
                stream.endText();
            }
            document.save(target);
        }
    }

    private static String joinAndTrim(List<String> values) {
        String line = String.join(" | ", values);
        return line.length() > 170 ? line.substring(0, 170) : line;
    }

    private static List<Map<String, String>> importExcelRows(File file) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream in = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) return rows;

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return rows;

            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String header = formatter.formatCellValue(headerRow.getCell(c));
                headers.add(header == null ? "" : header.trim());
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> mapped = new LinkedHashMap<>();
                boolean hasValue = false;
                for (int c = 0; c < headers.size(); c++) {
                    String value = formatter.formatCellValue(row.getCell(c));
                    mapped.put(headers.get(c), value);
                    if (!hasValue && value != null && !value.isBlank()) {
                        hasValue = true;
                    }
                }
                if (hasValue) rows.add(mapped);
            }
        }
        return rows;
    }
}
