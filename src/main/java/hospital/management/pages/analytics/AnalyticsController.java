package hospital.management.pages.analytics;

import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.finance.InvoiceDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicalInventoryDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicationDAOImpl;
import hospital.management.backend.dao.lab.LabOrderDAOImpl;
import hospital.management.backend.dao.lab.LabResultDAOImpl;
import hospital.management.backend.dao.patient.PatientFeedbackDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.finance.InvoiceSummaryDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.finance.InvoiceServiceImpl;
import hospital.management.backend.service.lab.LabServiceImpl;
import hospital.management.backend.service.patient.FeedbackServiceImpl;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.pages.BasePageController;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalyticsController extends BasePageController {

    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final InvoiceServiceImpl invoiceService = new InvoiceServiceImpl(new InvoiceDAOImpl(), new PatientDAOImpl());
    private final FeedbackServiceImpl feedbackService = new FeedbackServiceImpl(new PatientFeedbackDAOImpl());
    private final LabServiceImpl labService = new LabServiceImpl(new LabOrderDAOImpl(), new LabResultDAOImpl());

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    @FXML private ComboBox<String> periodFilter;
    @FXML private Button exportBtn;

    @FXML private BarChart<String, Number>  admissionsChart;
    @FXML private CategoryAxis admXAxis;
    @FXML private NumberAxis   admYAxis;

    @FXML private BarChart<String, Number>  revenueChart;
    @FXML private CategoryAxis revXAxis;
    @FXML private NumberAxis   revYAxis;

    @FXML private PieChart apptStatusChart;

    @FXML private BarChart<String, Number>  feedbackChart;
    @FXML private CategoryAxis fbXAxis;
    @FXML private NumberAxis   fbYAxis;

    @FXML private BarChart<String, Number>  labStatusChart;
    @FXML private CategoryAxis labXAxis;
    @FXML private NumberAxis   labYAxis;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.ANALYTICS);

        periodFilter.getItems().addAll("Last 30 days", "Last 3 months", "Last 6 months", "Last 12 months");
        periodFilter.setValue("Last 12 months");

        setupCharts();
        reloadData();

        periodFilter.setOnAction(e -> reloadData());
        exportBtn.setOnAction(e -> toast("Export not yet implemented.", NotificationType.INFO));
    }

    private void setupCharts() {
        admissionsChart.setLegendVisible(false);
        admissionsChart.setTitle("");

        revenueChart.setLegendVisible(false);
        revenueChart.setTitle("");

        feedbackChart.setLegendVisible(false);
        feedbackChart.setTitle("");

        labStatusChart.setLegendVisible(false);
        labStatusChart.setTitle("");

        apptStatusChart.setLegendVisible(true);
        apptStatusChart.setLabelsVisible(true);
    }

    private void reloadData() {
        String selectedPeriod = periodFilter.getValue();
        AsyncJobRunner.submit(() -> buildSnapshot(selectedPeriod), this::applySnapshot, ex -> {
            toastError("Failed to load analytics: " + ex.getMessage());
        });
    }

    private AnalyticsSnapshot buildSnapshot(String periodLabel) throws Exception {
        TimeWindow window = resolveWindow(periodLabel);

        List<AppointmentSummaryDTO> appointments = appointmentService.findAll(CursorPagination.firstPage(1000)).getItems()
                .stream()
                .filter(a -> a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(window.start))
                .toList();

        List<InvoiceSummaryDTO> invoices = invoiceService.findAll(CursorPagination.firstPage(1000)).getItems()
                .stream()
                .filter(i -> i.getIssuedAt() != null && !i.getIssuedAt().toLocalDate().isBefore(window.start.toLocalDate()))
                .toList();

        List<PatientFeedbackDTO> feedback = feedbackService.findAll().stream()
                .filter(f -> f.getDateSubmitted() != null && !f.getDateSubmitted().isBefore(window.start.toLocalDate()))
                .toList();

        Map<String, Long> admissionsByMonth = bucketByMonth(appointments.stream()
                .map(AppointmentSummaryDTO::getAppointmentDate)
                .toList(), window);

        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (InvoiceSummaryDTO invoice : invoices) {
            String key = YearMonth.from(invoice.getIssuedAt()).format(MONTH_FMT);
            revenueByMonth.merge(key, invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount(), BigDecimal::add);
        }

        Map<String, Long> appointmentStatus = appointments.stream()
                .collect(Collectors.groupingBy(a -> safeLabel(a.getStatus()), LinkedHashMap::new, Collectors.counting()));

        Map<Integer, Long> feedbackRatings = feedback.stream()
                .collect(Collectors.groupingBy(f -> f.getRating() == null ? 0 : f.getRating(), LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> labStatus = new LinkedHashMap<>();
        for (AppointmentSummaryDTO appointment : appointments) {
            for (LabOrderDTO order : labService.findOrdersByAppointment(appointment.getAppointmentId())) {
                if (order.getOrderedAt() != null && order.getOrderedAt().isBefore(window.start)) continue;
                labStatus.merge(safeLabel(order.getStatus()), 1L, Long::sum);
            }
        }

        return new AnalyticsSnapshot(admissionsByMonth, revenueByMonth, appointmentStatus, feedbackRatings, labStatus);
    }

    private void applySnapshot(AnalyticsSnapshot snapshot) {
        admissionsChart.getData().setAll(toMonthlySeries("Admissions", snapshot.admissionsByMonth()));
        revenueChart.getData().setAll(toMonthlyMoneySeries("Revenue", snapshot.revenueByMonth()));
        apptStatusChart.getData().setAll(snapshot.appointmentStatus().entrySet().stream()
                .map(e -> new PieChart.Data(capitalize(e.getKey()), e.getValue()))
                .toList());
        feedbackChart.getData().setAll(toIntegerSeries("Feedback Ratings", snapshot.feedbackRatings()));
        labStatusChart.getData().setAll(toMonthlySeries("Lab Orders", snapshot.labStatus()));
    }

    private XYChart.Series<String, Number> toMonthlySeries(String name, Map<String, Long> values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        values.forEach((month, count) -> series.getData().add(new XYChart.Data<>(month, count)));
        return series;
    }

    private XYChart.Series<String, Number> toMonthlyMoneySeries(String name, Map<String, BigDecimal> values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        values.forEach((month, amount) -> series.getData().add(new XYChart.Data<>(month, amount == null ? 0 : amount)));
        return series;
    }

    private XYChart.Series<String, Number> toIntegerSeries(String name, Map<Integer, Long> values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        values.forEach((rating, count) -> series.getData().add(new XYChart.Data<>(rating == 0 ? "n/a" : String.valueOf(rating), count)));
        return series;
    }

    private Map<String, Long> bucketByMonth(List<LocalDateTime> dates, TimeWindow window) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.from(window.start);
        YearMonth end = YearMonth.from(window.end);
        while (!cursor.isAfter(end)) {
            buckets.put(cursor.format(MONTH_FMT), 0L);
            cursor = cursor.plusMonths(1);
        }
        for (LocalDateTime date : dates) {
            String key = YearMonth.from(date).format(MONTH_FMT);
            buckets.merge(key, 1L, Long::sum);
        }
        return buckets;
    }

    private TimeWindow resolveWindow(String label) {
        LocalDate end = LocalDate.now();
        LocalDate start = switch (label) {
            case "Last 30 days" -> end.minusDays(30);
            case "Last 3 months" -> end.minusMonths(3);
            case "Last 6 months" -> end.minusMonths(6);
            default -> end.minusMonths(12);
        };
        return new TimeWindow(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    private String safeLabel(String value) {
        return value == null || value.isBlank() ? "unknown" : value.toLowerCase();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private record TimeWindow(LocalDateTime start, LocalDateTime end) {}

    private record AnalyticsSnapshot(
            Map<String, Long> admissionsByMonth,
            Map<String, BigDecimal> revenueByMonth,
            Map<String, Long> appointmentStatus,
            Map<Integer, Long> feedbackRatings,
            Map<String, Long> labStatus) {}
}