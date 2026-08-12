package hospital.management.pages.components.finance;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.service.lookup.EntityLookupService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class InvoiceTableController extends PaginatedTableController<InvoiceDTO> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String STATUS_PAID = "paid";

    private final EntityLookupService lookupService = new EntityLookupService();

    @FXML private TableColumn<InvoiceDTO, String>     invoiceIdColumn;
    @FXML private TableColumn<InvoiceDTO, String>     patientIdColumn;
    @FXML private TableColumn<InvoiceDTO, String>     appointmentIdColumn;
    @FXML private TableColumn<InvoiceDTO, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<InvoiceDTO, String>     paymentStatusColumn;
    @FXML private TableColumn<InvoiceDTO, Void>       changeStatusColumn;
    @FXML private TableColumn<InvoiceDTO, String>     issuedAtColumn;
    @FXML private TableColumn<InvoiceDTO, Void>       actionsColumn;

    private Consumer<InvoiceDTO> onChangeStatus;

    public void setOnChangeStatus(Consumer<InvoiceDTO> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
    protected void configureColumns() {
        invoiceIdColumn.setVisible(false);
        patientIdColumn.setText("Patient");
        patientIdColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(resolveLabel(() -> lookupService.patientLabel(cell.getValue().getPatientId()))));
        appointmentIdColumn.setText("Appointment");
        appointmentIdColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(appointmentDisplay(cell.getValue().getAppointmentId())));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        wireTextActionColumn(changeStatusColumn, "Mark Paid", "Mark invoice as paid",
                invoice -> !STATUS_PAID.equalsIgnoreCase(invoice.getPaymentStatus()),
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        issuedAtColumn.setCellValueFactory(cell -> {
            LocalDateTime issuedAt = cell.getValue().getIssuedAt();
            return new SimpleStringProperty(issuedAt != null ? issuedAt.format(DATE_FORMAT) : "");
        });
        addSortOption("Patient", patientIdColumn);
        addSortOption("Appointment", appointmentIdColumn);
        addSortOption("Total", totalAmountColumn);
        addSortOption("Payment Status", paymentStatusColumn);
        addSortOption("Issued At", issuedAtColumn);
        wireActionsColumn(actionsColumn);
    }

    private String appointmentDisplay(String appointmentId) {
        try {
            var opt = lookupService.findById(EntityLookupService.APPOINTMENT, appointmentId);
            if (opt.isEmpty()) return "—";
            AppointmentDTO appointment = (AppointmentDTO) opt.get();
            String date = appointment.getAppointmentDate() != null
                    ? appointment.getAppointmentDate().toLocalDate().toString() : "";
            return date + " with " + lookupService.doctorLabel(appointment.getDoctorId());
        } catch (Exception ex) {
            return "—";
        }
    }

    @Override
    protected boolean matches(InvoiceDTO invoice, String lowerQuery) {
        String amountStr = invoice.getTotalAmount() != null ? invoice.getTotalAmount().toPlainString() : "";
        String dateStr   = invoice.getIssuedAt() != null ? invoice.getIssuedAt().format(DATE_FORMAT) : "";
        return safe(invoice.getInvoiceId()).contains(lowerQuery)
            || safe(invoice.getPatientId()).contains(lowerQuery)
            || safe(invoice.getAppointmentId()).contains(lowerQuery)
            || safe(invoice.getPaymentStatus()).contains(lowerQuery)
            || amountStr.contains(lowerQuery)
            || dateStr.contains(lowerQuery);
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(); }
}
