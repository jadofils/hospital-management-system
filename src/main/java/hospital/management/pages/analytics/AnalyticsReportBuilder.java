package hospital.management.pages.analytics;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the professional, multi-page "Analytics Report" PDF: a title page followed by
 * one section per Analytics-page chart (heading, the chart's snapshot image, and a plain
 * summary of its underlying numbers). Mirrors DeveloperDashboardController's report-download
 * flow (FileChooser + AsyncJobRunner), but is a dedicated template rather than a re-used one.
 */
final class AnalyticsReportBuilder {

    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final float MARGIN = 50f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float MAX_IMAGE_HEIGHT = 260f;

    // "Hospital blue" brand color used for header bars — PDPageContentStream's RGB
    // setters take 0-1 float components, not 0-255.
    private static final float BRAND_R = 30 / 255f, BRAND_G = 58 / 255f, BRAND_B = 92 / 255f;

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private AnalyticsReportBuilder() {}

    record ChartSection(String title, BufferedImage image, Map<String, String> summaryRows) {}

    static void build(Path destination, String period, AnalyticsController.AnalyticsSnapshot snapshot,
                       BufferedImage admissionsImg, BufferedImage revenueImg, BufferedImage apptStatusImg,
                       BufferedImage feedbackImg, BufferedImage labStatusImg) throws IOException {

        List<ChartSection> sections = List.of(
                new ChartSection("Monthly Admissions", admissionsImg, toStringMap(snapshot.admissionsByMonth())),
                new ChartSection("Monthly Revenue", revenueImg, toMoneyMap(snapshot.revenueByMonth())),
                new ChartSection("Appointment Status", apptStatusImg, toCapitalizedMap(snapshot.appointmentStatus())),
                new ChartSection("Patient Feedback Ratings", feedbackImg, toRatingMap(snapshot.feedbackRatings())),
                new ChartSection("Lab Order Status Distribution", labStatusImg, toCapitalizedMap(snapshot.labStatus())));

        try (PDDocument document = new PDDocument()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            int totalPages = 1 + sections.size();
            int pageNumber = 1;

            drawTitlePage(document, regular, bold, period, pageNumber, totalPages);
            for (ChartSection section : sections) {
                pageNumber++;
                drawChartPage(document, regular, bold, section, pageNumber, totalPages);
            }

            document.save(destination.toFile());
        }
    }

    private static void drawTitlePage(PDDocument document, PDFont regular, PDFont bold,
                                       String period, int pageNumber, int totalPages) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);

        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            drawHeaderBar(stream, 130f);

            drawText(stream, bold, 20, MARGIN, PAGE_HEIGHT - 60, "Hospital Management System", 1, 1, 1);
            drawText(stream, bold, 28, MARGIN, PAGE_HEIGHT - 95, "Analytics Report", 1, 1, 1);

            float y = PAGE_HEIGHT - 170;
            drawText(stream, regular, 12, MARGIN, y, "Generated: " + LocalDateTime.now().format(TIMESTAMP_FMT), 0, 0, 0);
            y -= 20;
            drawText(stream, regular, 12, MARGIN, y, "Period: " + (period == null ? "N/A" : period), 0, 0, 0);

            y -= 40;
            drawText(stream, bold, 13, MARGIN, y, "Overview", 0, 0, 0);
            y -= 20;
            String description = "This report summarizes hospital analytics across admissions, revenue, "
                    + "appointment status, patient feedback, and lab order turnaround for the selected period.";
            y = drawWrappedText(stream, regular, 11, MARGIN, y, CONTENT_WIDTH, description);

            y -= 25;
            drawText(stream, bold, 13, MARGIN, y, "Contents", 0, 0, 0);
            y -= 20;
            String[] contents = {
                    "1. Monthly Admissions", "2. Monthly Revenue", "3. Appointment Status",
                    "4. Patient Feedback Ratings", "5. Lab Order Status Distribution"
            };
            for (String line : contents) {
                drawText(stream, regular, 11, MARGIN + 10, y, line, 0, 0, 0);
                y -= 16;
            }

            drawFooter(stream, regular, pageNumber, totalPages);
        }
    }

    private static void drawChartPage(PDDocument document, PDFont regular, PDFont bold,
                                       ChartSection section, int pageNumber, int totalPages) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);

        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            drawHeaderBar(stream, 44f);
            drawText(stream, bold, 12, MARGIN, PAGE_HEIGHT - 28, "Analytics Report", 1, 1, 1);
            drawTextRightAligned(stream, regular, 10, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 27,
                    "Hospital Management System", 1, 1, 1);

            float y = PAGE_HEIGHT - 44 - 40;
            drawText(stream, bold, 17, MARGIN, y, section.title(), 0, 0, 0);
            y -= 20;

            if (section.image() != null) {
                float[] size = fitWithin(section.image().getWidth(), section.image().getHeight(), CONTENT_WIDTH, MAX_IMAGE_HEIGHT);
                float drawWidth = size[0];
                float drawHeight = size[1];
                float imageX = MARGIN + (CONTENT_WIDTH - drawWidth) / 2f;
                float imageY = y - drawHeight;

                PDImageXObject pdImage = LosslessFactory.createFromImage(document, section.image());
                stream.drawImage(pdImage, imageX, imageY, drawWidth, drawHeight);
                y = imageY - 25;
            }

            drawText(stream, bold, 12, MARGIN, y, "Summary", 0, 0, 0);
            y -= 18;
            for (Map.Entry<String, String> row : section.summaryRows().entrySet()) {
                if (y < MARGIN + 30) break; // avoid drawing past the footer if a chart has many rows
                drawText(stream, regular, 10, MARGIN + 10, y, row.getKey() + ":  " + row.getValue(), 0, 0, 0);
                y -= 15;
            }

            drawFooter(stream, regular, pageNumber, totalPages);
        }
    }

    private static void drawHeaderBar(PDPageContentStream stream, float height) throws IOException {
        stream.setNonStrokingColor(BRAND_R, BRAND_G, BRAND_B);
        stream.addRect(0, PAGE_HEIGHT - height, PAGE_WIDTH, height);
        stream.fill();
    }

    private static void drawFooter(PDPageContentStream stream, PDFont regular, int pageNumber, int totalPages) throws IOException {
        drawText(stream, regular, 9, MARGIN, 30, "Hospital Management System — Confidential", 0.4f, 0.4f, 0.4f);
        drawTextRightAligned(stream, regular, 9, PAGE_WIDTH - MARGIN, 30,
                "Page " + pageNumber + " of " + totalPages, 0.4f, 0.4f, 0.4f);
    }

    private static void drawText(PDPageContentStream stream, PDFont font, float size, float x, float y,
                                  String text, float r, float g, float b) throws IOException {
        stream.setNonStrokingColor(r, g, b);
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private static void drawTextRightAligned(PDPageContentStream stream, PDFont font, float size, float rightX, float y,
                                               String text, float r, float g, float b) throws IOException {
        float width = font.getStringWidth(text) / 1000f * size;
        drawText(stream, font, size, rightX - width, y, text, r, g, b);
    }

    /** Wraps a long paragraph across multiple lines within maxWidth, drawing each line as its
     *  own text block; returns the y-coordinate after the last line so callers can continue below it. */
    private static float drawWrappedText(PDPageContentStream stream, PDFont font, float size,
                                          float x, float y, float maxWidth, String text) throws IOException {
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        float cursorY = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * size;
            if (width > maxWidth && !line.isEmpty()) {
                drawText(stream, font, size, x, cursorY, line.toString(), 0, 0, 0);
                cursorY -= size + 4;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            drawText(stream, font, size, x, cursorY, line.toString(), 0, 0, 0);
            cursorY -= size + 4;
        }
        return cursorY;
    }

    /** Scales (width, height) to fit within (maxWidth, maxHeight), preserving aspect ratio. */
    private static float[] fitWithin(double width, double height, float maxWidth, float maxHeight) {
        if (width <= 0 || height <= 0) return new float[] {maxWidth, maxHeight};
        float scale = (float) Math.min(maxWidth / width, maxHeight / height);
        return new float[] {(float) (width * scale), (float) (height * scale)};
    }

    private static Map<String, String> toStringMap(Map<String, Long> values) {
        Map<String, String> out = new LinkedHashMap<>();
        values.forEach((k, v) -> out.put(k, String.valueOf(v)));
        return out;
    }

    private static Map<String, String> toMoneyMap(Map<String, BigDecimal> values) {
        Map<String, String> out = new LinkedHashMap<>();
        values.forEach((k, v) -> out.put(k, "$" + String.format("%,.2f", v == null ? BigDecimal.ZERO : v)));
        return out;
    }

    private static Map<String, String> toCapitalizedMap(Map<String, Long> values) {
        Map<String, String> out = new LinkedHashMap<>();
        values.forEach((k, v) -> out.put(capitalize(k), String.valueOf(v)));
        return out;
    }

    private static Map<String, String> toRatingMap(Map<Integer, Long> values) {
        Map<String, String> out = new LinkedHashMap<>();
        values.forEach((k, v) -> out.put(k == null || k == 0 ? "n/a" : k + " star(s)", String.valueOf(v)));
        return out;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
