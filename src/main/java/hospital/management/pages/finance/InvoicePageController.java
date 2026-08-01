package hospital.management.pages.finance;

import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.finance.InvoiceTableController;
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

        TextField patientId     = new TextField();
        TextField appointmentId = new TextField();
        TextField totalAmount   = new TextField();
        ComboBox<String> paymentStatus = new ComboBox<>();

        List.of(patientId, appointmentId, totalAmount).forEach(f -> f.getStyleClass().add("form-input"));
        paymentStatus.getStyleClass().add("form-combo");
        paymentStatus.getItems().addAll("Pending", "Paid", "Overdue", "Cancelled");

        if (!addMode) {
            patientId.setText(invoice.getPatientId());
            appointmentId.setText(invoice.getAppointmentId());
            totalAmount.setText(invoice.getTotalAmount() == null ? "" : invoice.getTotalAmount().toPlainString());
            paymentStatus.setValue(invoice.getPaymentStatus());
        }

        formDialogController.open(addMode ? "Add Invoice" : "Update Invoice", "fas-file-invoice-dollar", addMode, v -> {
            String pid = patientId.getText() == null ? "" : patientId.getText().trim();
            String aid = appointmentId.getText() == null ? "" : appointmentId.getText().trim();
            String amountText = totalAmount.getText() == null ? "" : totalAmount.getText().trim();

            if (pid.isEmpty() || aid.isEmpty() || amountText.isEmpty() || paymentStatus.getValue() == null) {
                formDialogController.setError("Patient ID, appointment ID, total amount and payment status are required.");
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

        formDialogController.addField("Patient Id", "fas-user", patientId);
        formDialogController.addField("Appointment Id", "fas-calendar-check", appointmentId);
        formDialogController.addField("Total Amount", "fas-dollar-sign", totalAmount);
        formDialogController.addField("Payment Status", "fas-info-circle", paymentStatus);
    }
}
