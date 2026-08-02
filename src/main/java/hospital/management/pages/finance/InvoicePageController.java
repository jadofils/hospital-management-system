package hospital.management.pages.finance;

import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.finance.InvoiceTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvoicePageController extends BasePageController implements QuickAddCapable {

    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());

    @FXML private InvoiceTableController invoiceTableController;

    @FXML private Label totalRevenueLabel;
    @FXML private Label paidLabel;
    @FXML private Label pendingLabel;

    @FXML private Button newInvoiceBtn;
    @FXML private Button exportCsvBtn;
    @FXML private Button printReportBtn;

    private final List<Invoice> invoices = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.BILLING);

        totalRevenueLabel.setText("$0.00");
        paidLabel.setText("$0.00");
        pendingLabel.setText("$0.00");

        newInvoiceBtn.setOnAction(e -> openInvoiceDialog(null));
        exportCsvBtn.setOnAction(e -> toast("Export not yet implemented.", NotificationType.INFO));
        printReportBtn.setOnAction(e -> toast("Print not yet implemented.", NotificationType.INFO));

        invoiceTableController.setRowActions(this::openInvoiceDialog, this::confirmDeleteInvoice);

        refreshTable();
    }

    private void refreshTable() {
        invoiceTableController.setItems(invoices);
    }

    private void confirmDeleteInvoice(Invoice invoice) {
        confirm("Delete Invoice",
                "Are you sure you want to delete invoice " + invoice.getInvoiceId() + "? This cannot be undone.",
                () -> {
                    invoices.remove(invoice);
                    refreshTable();
                    toastSuccess("Invoice deleted.");
                });
    }

    @Override
    public void openAddDialog() {
        openInvoiceDialog(null);
    }

    /** Opens the shared form dialog in Add mode (invoice == null) or Update mode. */
    private void openInvoiceDialog(Invoice invoice) {
        boolean addMode = invoice == null;

        EntityIdComboBox patientId     = new EntityIdComboBox();
        EntityIdComboBox appointmentId = new EntityIdComboBox();
        TextField totalAmount   = new TextField();
        ComboBox<String> paymentStatus = new ComboBox<>();

        totalAmount.getStyleClass().add("form-input");
        List.of(patientId, appointmentId).forEach(f -> f.getStyleClass().add("form-combo"));
        paymentStatus.getStyleClass().add("form-combo");
        paymentStatus.getItems().addAll("Pending", "Paid", "Overdue", "Cancelled");

        try {
            patientId.setOptions(patientService.findAll(CursorPagination.firstPage(1000)).getItems().stream()
                    .map(p -> new EntityIdComboBox.Option(p.getPatientId(), p.getFullName())).toList());
            appointmentId.setOptions(appointmentService.findAll(CursorPagination.firstPage(1000)).getItems().stream()
                    .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                            a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                    .toList());
        } catch (Exception ex) {
            toastError("Failed to load patients/appointments: " + ex.getMessage());
        }

        if (!addMode) {
            patientId.selectById(invoice.getPatientId());
            appointmentId.selectById(invoice.getAppointmentId());
            totalAmount.setText(invoice.getTotalAmount() == null ? "" : invoice.getTotalAmount().toPlainString());
            paymentStatus.setValue(invoice.getPaymentStatus());
        }

        formDialogController.open(addMode ? "Add Invoice" : "Update Invoice", "fas-file-invoice-dollar", addMode, v -> {
            String pid = patientId.getSelectedId();
            String aid = appointmentId.getSelectedId();
            String amountText = totalAmount.getText() == null ? "" : totalAmount.getText().trim();

            if (pid == null || aid == null || amountText.isEmpty() || paymentStatus.getValue() == null) {
                formDialogController.setError("Patient, appointment, total amount and payment status are required.");
                formDialogController.setLoading(false);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountText);
            } catch (NumberFormatException ex) {
                formDialogController.setError("Total amount must be a valid number.");
                formDialogController.setLoading(false);
                return;
            }

            Invoice target = addMode ? new Invoice() : invoice;
            if (addMode) target.setInvoiceId(UUID.randomUUID().toString());
            target.setPatientId(pid);
            target.setAppointmentId(aid);
            target.setTotalAmount(amount);
            target.setPaymentStatus(paymentStatus.getValue());
            if (addMode) {
                target.setIssuedAt(LocalDateTime.now());
            } else {
                target.setUpdatedAt(LocalDateTime.now());
            }

            if (addMode) invoices.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Invoice added." : "Invoice updated.");
        });

        formDialogController.addField("Patient", "fas-user", patientId);
        formDialogController.addField("Appointment", "fas-calendar-check", appointmentId);
        formDialogController.addField("Total Amount", "fas-dollar-sign", totalAmount);
        formDialogController.addField("Payment Status", "fas-info-circle", paymentStatus);
    }
}
