package hospital.management.pages.finance;

import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.finance.InvoiceDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.finance.CreateInvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceSummaryDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.finance.InvoiceServiceImpl;
import hospital.management.backend.service.finance.interfaces.InvoiceService;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.finance.InvoiceTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import hospital.management.pages.utils.CsvUiIO;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class InvoicePageController extends BasePageController implements QuickAddCapable {

    private static final String STATUS_PAID = "paid";

    private final InvoiceService invoiceService = new InvoiceServiceImpl(new InvoiceDAOImpl(), new PatientDAOImpl());
    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private InvoiceTableController invoiceTableController;

    @FXML private Label totalRevenueLabel;
    @FXML private Label paidLabel;
    @FXML private Label pendingLabel;

    @FXML private Button newInvoiceBtn;
    @FXML private Button importCsvBtn;
    @FXML private Button exportCsvBtn;
    @FXML private Button printReportBtn;

    private final List<InvoiceDTO> invoices = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.BILLING);

        totalRevenueLabel.setText("$0.00");
        paidLabel.setText("$0.00");
        pendingLabel.setText("$0.00");

        applyCreateVisibility(newInvoiceBtn, PageRoute.BILLING);
        applyCreateVisibility(importCsvBtn, PageRoute.BILLING);
        newInvoiceBtn.setOnAction(e -> openInvoiceDialog());
        importCsvBtn.setOnAction(e -> withSpinner(importCsvBtn, this::importInvoicesCsv));
        exportCsvBtn.setOnAction(e -> withSpinner(exportCsvBtn, this::exportInvoicesCsv));
        printReportBtn.setOnAction(e -> printReport());
        printReportBtn.setVisible(canRead(PageRoute.BILLING));
        printReportBtn.setManaged(canRead(PageRoute.BILLING));

        invoiceTableController.setRowActions(
            canUpdate(PageRoute.BILLING) ? invoice -> toast("Invoices can't be edited after issuance.", NotificationType.INFO) : null,
            allowDelete(PageRoute.BILLING, this::confirmDeleteInvoice),
            allowRead(PageRoute.BILLING, this::viewInvoiceDetail));
        invoiceTableController.setOnChangeStatus(canUpdate(PageRoute.BILLING) ? this::markInvoicePaid : null);

        refreshTable();
    }

    private void refreshTable() {
        try {
            invoices.clear();
            List<InvoiceSummaryDTO> summaries =
                    invoiceService.findAll(CursorPagination.firstPage(500)).getItems();
            for (InvoiceSummaryDTO summary : summaries) {
                invoices.add(invoiceService.findById(summary.getInvoiceId()));
            }
            invoiceTableController.setItems(invoices);
            updateSummaryLabels();
        } catch (Exception e) {
            toastError("Failed to load invoices: " + e.getMessage());
        }
    }

    private void updateSummaryLabels() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (InvoiceDTO invoice : invoices) {
            BigDecimal amount = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
            total = total.add(amount);
            if (STATUS_PAID.equalsIgnoreCase(invoice.getPaymentStatus())) {
                paid = paid.add(amount);
            }
        }
        totalRevenueLabel.setText("$" + total.toPlainString());
        paidLabel.setText("$" + paid.toPlainString());
        pendingLabel.setText("$" + total.subtract(paid).toPlainString());
    }

    private void viewInvoiceDetail(InvoiceDTO invoice) {
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

    private void confirmDeleteInvoice(InvoiceDTO invoice) {
        confirm("Delete Invoice",
                "Are you sure you want to delete invoice " + invoice.getInvoiceId() + "? This cannot be undone.",
                () -> {
                    try {
                        invoiceService.delete(invoice.getInvoiceId());
                        refreshTable();
                        toastSuccess("Invoice deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete invoice: " + e.getMessage());
                    }
                });
    }

    /** The backend's only payment-status transition is to mark an unpaid invoice as paid. */
    private void markInvoicePaid(InvoiceDTO invoice) {
        if (STATUS_PAID.equalsIgnoreCase(invoice.getPaymentStatus())) {
            toast("This invoice is already paid.", NotificationType.INFO);
            return;
        }
        confirm("Mark Invoice Paid",
                "Are you sure you want to mark invoice " + invoice.getInvoiceId() + " as paid?",
                () -> {
                    try {
                        invoiceService.markPaid(invoice.getInvoiceId());
                        refreshTable();
                        toastSuccess("Invoice marked as paid.");
                    } catch (Exception e) {
                        toastError("Failed to update invoice status: " + e.getMessage());
                    }
                });
    }

    @Override
    public void openAddDialog() {
        openInvoiceDialog();
    }

    /** Opens the shared form dialog to generate a new invoice. */
    private void openInvoiceDialog() {
        LoadingIdComboBox patientIdField     = new LoadingIdComboBox();
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox patientId     = patientIdField.getComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        TextField totalAmount   = new TextField();

        totalAmount.getStyleClass().add("form-input");
        List.of(patientId, appointmentId).forEach(f -> f.getStyleClass().add("form-combo"));

        List<Control> otherFields = List.of(totalAmount);
        otherFields.forEach(f -> f.setDisable(true));

        formDialogController.open("Add Invoice", "fas-file-invoice-dollar", true, v -> {
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

            try {
                invoiceService.generate(new CreateInvoiceDTO(aid, pid, amount));
                refreshTable();
                formDialogController.close();
                toastSuccess("Invoice added.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save invoice: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Patient", "fas-user", patientIdField);
        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Total Amount", "fas-dollar-sign", totalAmount);

        loadInvoiceDropdowns(patientIdField, appointmentIdField, otherFields);
    }

    /** Loads the patient/appointment dropdown options asynchronously, showing each dropdown's own
     *  spinner while its data is in flight and keeping the rest of the form disabled until
     *  both have finished loading. */
    private void loadInvoiceDropdowns(LoadingIdComboBox patientIdField, LoadingIdComboBox appointmentIdField,
                                       List<Control> otherFields) {
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
                appointmentIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                appointmentIdField.setLoading(false);
                toastError("Failed to load appointments: " + ex.getMessage());
                onOneLoaded.run();
            });
    }

    private void exportInvoicesCsv() {
        try {
            if (invoices.isEmpty()) {
                toastError("No invoices available to export.");
                return;
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (InvoiceDTO invoice : invoices) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("invoice_id", invoice.getInvoiceId());
                row.put("appointment_id", invoice.getAppointmentId());
                row.put("patient_id", invoice.getPatientId());
                row.put("total_amount", invoice.getTotalAmount());
                row.put("payment_status", invoice.getPaymentStatus());
                row.put("issued_at", invoice.getIssuedAt());
                rows.add(row);
            }

            boolean saved = CsvUiIO.exportRows(exportCsvBtn.getScene().getWindow(), "invoices.csv", rows);
            if (saved) {
                toastSuccess("Invoices exported successfully.");
            }
        } catch (Exception e) {
            toastError("Failed to export invoices: " + e.getMessage());
        }
    }

    private void importInvoicesCsv() {
        try {
            List<Map<String, String>> rows = CsvUiIO.importRows(importCsvBtn.getScene().getWindow(), "Import Invoices CSV");
            if (rows.isEmpty()) {
                return;
            }

            int success = 0;
            int failed = 0;
            List<String> failures = new ArrayList<>();

            for (Map<String, String> row : rows) {
                try {
                    String appointmentId = readColumn(row, "appointment_id", "appointmentId");
                    String patientId = readColumn(row, "patient_id", "patientId");
                    String totalAmount = readColumn(row, "total_amount", "totalAmount");

                    if (appointmentId == null || patientId == null || totalAmount == null) {
                        throw new IllegalArgumentException("Missing required columns appointment_id, patient_id, total_amount");
                    }

                    invoiceService.generate(new CreateInvoiceDTO(
                            appointmentId.trim(),
                            patientId.trim(),
                            new BigDecimal(totalAmount.trim())));
                    success++;
                } catch (Exception ex) {
                    failed++;
                    if (failures.size() < 3) {
                        failures.add(ex.getMessage());
                    }
                }
            }

            refreshTable();
            if (failed == 0) {
                toastSuccess("Imported " + success + " invoice rows.");
            } else {
                String details = failures.isEmpty() ? "" : " Example errors: " + String.join(" | ", failures);
                toastError("Imported " + success + " rows, failed " + failed + "." + details);
            }
        } catch (Exception e) {
            toastError("Failed to import invoices: " + e.getMessage());
        }
    }

    private String readColumn(Map<String, String> row, String preferred, String alternate) {
        if (row.containsKey(preferred)) {
            return row.get(preferred);
        }
        if (row.containsKey(alternate)) {
            return row.get(alternate);
        }

        Map<String, String> normalized = new HashMap<>();
        row.forEach((k, v) -> normalized.put(normalizeHeader(k), v));
        String normalizedPreferred = normalizeHeader(preferred);
        if (normalized.containsKey(normalizedPreferred)) {
            return normalized.get(normalizedPreferred);
        }
        return normalized.get(normalizeHeader(alternate));
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toLowerCase().replace(" ", "_");
    }

    private void printReport() {
        if (invoices.isEmpty()) {
            toastError("No billing data available to print.");
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (InvoiceDTO invoice : invoices) {
            BigDecimal amount = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
            total = total.add(amount);
            if (STATUS_PAID.equalsIgnoreCase(invoice.getPaymentStatus())) {
                paid = paid.add(amount);
            }
        }

        StringBuilder content = new StringBuilder();
        content.append("Hospital Billing Report\n\n");
        content.append("Total invoices: ").append(invoices.size()).append("\n");
        content.append("Total revenue: $").append(total.toPlainString()).append("\n");
        content.append("Paid amount: $").append(paid.toPlainString()).append("\n");
        content.append("Pending amount: $").append(total.subtract(paid).toPlainString()).append("\n\n");
        content.append("Invoice ID, Patient ID, Appointment ID, Amount, Status, Issued At\n");
        for (InvoiceDTO invoice : invoices) {
            content.append(invoice.getInvoiceId()).append(",")
                    .append(invoice.getPatientId()).append(",")
                    .append(invoice.getAppointmentId()).append(",")
                    .append(invoice.getTotalAmount() == null ? "0" : invoice.getTotalAmount().toPlainString()).append(",")
                    .append(invoice.getPaymentStatus() == null ? "" : invoice.getPaymentStatus()).append(",")
                    .append(invoice.getIssuedAt() == null ? "" : invoice.getIssuedAt())
                    .append("\n");
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            toastError("No printer available.");
            return;
        }
        Text printable = new Text(content.toString());
        printable.setWrappingWidth(520);

        boolean proceed = job.showPrintDialog(printReportBtn.getScene().getWindow());
        if (!proceed) {
            return;
        }
        boolean success = job.printPage(printable);
        if (success) {
            job.endJob();
            toastSuccess("Billing report sent to printer.");
        } else {
            toastError("Failed to print billing report.");
        }
    }
}
