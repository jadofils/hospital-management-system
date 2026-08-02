package hospital.management.pages.finance;

import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.finance.InvoiceTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class InvoicePageController extends BasePageController implements QuickAddCapable {

    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

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

        invoiceTableController.setRowActions(this::openInvoiceDialog, this::confirmDeleteInvoice, this::viewInvoiceDetail);
        invoiceTableController.setOnChangeStatus(this::openInvoiceStatusDialog);

        refreshTable();
    }

    private void refreshTable() {
        invoiceTableController.setItems(invoices);
    }

    private void viewInvoiceDetail(Invoice invoice) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Patient", entityLookupService.patientLabel(invoice.getPatientId()));
            fields.put("Appointment", entityLookupService.appointmentLabel(invoice.getAppointmentId()));
        } catch (Exception ex) {
            toastError("Failed to resolve invoice details: " + ex.getMessage());
        }
        fields.put("Total Amount", invoice.getTotalAmount() == null ? null : invoice.getTotalAmount().toPlainString());
        fields.put("Payment Status", invoice.getPaymentStatus());
        fields.put("Issued At", invoice.getIssuedAt() == null ? null : invoice.getIssuedAt().toString());
        detailViewController.show("Invoice Details", "fas-file-invoice-dollar", fields);
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

        LoadingIdComboBox patientIdField     = new LoadingIdComboBox();
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox patientId     = patientIdField.getComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        TextField totalAmount   = new TextField();

        totalAmount.getStyleClass().add("form-input");
        List.of(patientId, appointmentId).forEach(f -> f.getStyleClass().add("form-combo"));

        List<Control> otherFields = List.of(totalAmount);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            totalAmount.setText(invoice.getTotalAmount() == null ? "" : invoice.getTotalAmount().toPlainString());
        }

        formDialogController.open(addMode ? "Add Invoice" : "Update Invoice", "fas-file-invoice-dollar", addMode, v -> {
            String pid = patientId.getSelectedId();
            String aid = appointmentId.getSelectedId();
            String amountText = totalAmount.getText() == null ? "" : totalAmount.getText().trim();

            if (pid == null || aid == null || amountText.isEmpty()) {
                formDialogController.setError("Patient, appointment and total amount are required.");
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
            if (addMode) {
                target.setInvoiceId(UUID.randomUUID().toString());
                target.setPaymentStatus("Pending");
            }
            target.setPatientId(pid);
            target.setAppointmentId(aid);
            target.setTotalAmount(amount);
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

        formDialogController.addField("Patient", "fas-user", patientIdField);
        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Total Amount", "fas-dollar-sign", totalAmount);

        loadInvoiceDropdowns(patientIdField, appointmentIdField, otherFields, addMode ? null : invoice);
    }

    /** Loads the patient/appointment dropdown options asynchronously, showing each dropdown's own
     *  spinner while its data is in flight and keeping the rest of the form disabled until
     *  both have finished loading. */
    private void loadInvoiceDropdowns(LoadingIdComboBox patientIdField, LoadingIdComboBox appointmentIdField,
                                       List<Control> otherFields, Invoice existing) {
        EntityIdComboBox patientId = patientIdField.getComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();

        patientIdField.setLoading(true);
        appointmentIdField.setLoading(true);
        formDialogController.setLoading(true);

        AtomicInteger pending = new AtomicInteger(2);
        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

        AsyncJobRunner.submit(
            () -> patientService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                patientId.setOptions(items.stream()
                        .map(p -> new EntityIdComboBox.Option(p.getPatientId(), p.getFullName())).toList());
                if (existing != null) patientId.selectById(existing.getPatientId());
                patientIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                patientIdField.setLoading(false);
                toastError("Failed to load patients: " + ex.getMessage());
                onOneLoaded.run();
            });

        AsyncJobRunner.submit(
            () -> appointmentService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                appointmentId.setOptions(items.stream()
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
                if (existing != null) appointmentId.selectById(existing.getAppointmentId());
                appointmentIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                appointmentIdField.setLoading(false);
                toastError("Failed to load appointments: " + ex.getMessage());
                onOneLoaded.run();
            });
    }

    /** Minimal single-field dialog for changing an existing invoice's payment status, kept out of the main Add/Edit form. */
    private void openInvoiceStatusDialog(Invoice invoice) {
        ComboBox<String> paymentStatus = new ComboBox<>();
        paymentStatus.getStyleClass().add("form-combo");
        paymentStatus.getItems().addAll("Pending", "Paid", "Overdue", "Cancelled");
        paymentStatus.setValue(invoice.getPaymentStatus());

        formDialogController.open("Change Payment Status", "fas-info-circle", false, v -> {
            if (paymentStatus.getValue() == null) {
                formDialogController.setError("Payment status is required.");
                formDialogController.setLoading(false);
                return;
            }
            invoice.setPaymentStatus(paymentStatus.getValue());
            invoice.setUpdatedAt(LocalDateTime.now());
            refreshTable();
            formDialogController.close();
            toastSuccess("Invoice payment status updated.");
        });

        formDialogController.addField("Payment Status", "fas-info-circle", paymentStatus);
    }
}
