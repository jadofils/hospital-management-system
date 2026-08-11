package hospital.management.pages.finance;

import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.finance.InvoiceDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.finance.CreateInvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.dto.finance.InvoiceSummaryDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.finance.InvoiceServiceImpl;
import hospital.management.backend.service.finance.interfaces.InvoiceService;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.finance.InvoiceTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import hospital.management.pages.components.shared.sort.SortBarController;
import hospital.management.pages.utils.CsvUiIO;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
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

    private final InvoiceService invoiceService =
        new InvoiceServiceImpl(new InvoiceDAOImpl(), new PatientDAOImpl(), new AppointmentDAOImpl());
    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private InvoiceTableController invoiceTableController;
    @FXML private SortBarController sortBarController;

    @FXML private Label totalRevenueLabel;
    @FXML private Label paidLabel;
    @FXML private Label pendingLabel;

    @FXML private Button newInvoiceBtn;
    @FXML private Button importCsvBtn;
    @FXML private Button exportCsvBtn;
    @FXML private Button printReportBtn;
    @FXML private Button continueBtn;

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
        setupContinueButton(continueBtn, PageRoute.BILLING);

        invoiceTableController.setRowActions(
            canUpdate(PageRoute.BILLING) ? invoice -> toast("Invoices can't be edited after issuance.", NotificationType.INFO) : null,
            allowDelete(PageRoute.BILLING, this::confirmDeleteInvoice),
            allowRead(PageRoute.BILLING, this::viewInvoiceDetail));
        invoiceTableController.setOnChangeStatus(canUpdate(PageRoute.BILLING) ? this::markInvoicePaid : null);

        if (sortBarController != null) {
            sortBarController.setOnSort((field, asc) -> invoiceTableController.applySort(field, asc));
            sortBarController.addOptions(invoiceTableController.getSortOptionLabels());
        }

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

        totalAmount.setPromptText("e.g. 150.00");
        totalAmount.getStyleClass().add("form-input");
        List.of(patientId, appointmentId).forEach(f -> f.getStyleClass().add("form-combo"));

        // Real-time: amount must be a valid positive number
        totalAmount.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) {
                FxFormValidator.applyStyle(totalAmount, false);
            } else {
                try {
                    BigDecimal v = new BigDecimal(n.trim());
                    FxFormValidator.applyStyle(totalAmount, v.compareTo(BigDecimal.ZERO) > 0);
                } catch (NumberFormatException ex) {
                    FxFormValidator.applyStyle(totalAmount, false);
                }
            }
        });

        List<Control> otherFields = List.of(totalAmount);
        otherFields.forEach(f -> f.setDisable(true));

        formDialogController.open("Add Invoice", "fas-file-invoice-dollar", true, v -> {
            String pid = patientId.getSelectedId();
            String aid = appointmentId.getSelectedId();
            String amountText = totalAmount.getText() == null ? "" : totalAmount.getText().trim();

            if (pid == null) {
                formDialogController.setError("Patient is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (aid == null) {
                formDialogController.setError("Appointment is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (amountText.isEmpty()) {
                formDialogController.setError("Total amount is required.");
                FxFormValidator.applyStyle(totalAmount, false);
                formDialogController.setLoading(false);
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountText);
            } catch (NumberFormatException ex) {
                formDialogController.setError("Total amount must be a valid number (e.g. 150.00).");
                FxFormValidator.applyStyle(totalAmount, false);
                formDialogController.setLoading(false);
                return;
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                formDialogController.setError("Total amount must be greater than zero.");
                FxFormValidator.applyStyle(totalAmount, false);
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
     *  both have finished loading. Once loaded, the appointment dropdown is filtered to the
     *  selected patient's appointments only — an invoice can never be attached to another
     *  patient's appointment. */
    private void loadInvoiceDropdowns(LoadingIdComboBox patientIdField, LoadingIdComboBox appointmentIdField,
                                       List<Control> otherFields) {
        EntityIdComboBox patientId = patientIdField.getComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();

        patientIdField.setLoading(true);
        appointmentIdField.setLoading(true);
        formDialogController.setLoading(true);

        List<EntityIdComboBox.Option> allAppointments = new ArrayList<>();
        Map<String, String> appointmentPatient = new HashMap<>();
        AtomicInteger pending = new AtomicInteger(2);

        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

        patientId.valueProperty().addListener((obs, oldVal, newVal) -> {
            String selectedPatient = newVal == null ? null : newVal.id();
            if (selectedPatient == null) {
                appointmentId.setOptions(allAppointments);
                return;
            }
            appointmentId.setOptions(allAppointments.stream()
                    .filter(o -> selectedPatient.equals(appointmentPatient.get(o.id())))
                    .toList());
            // Drop any appointment that belongs to a different patient than the newly selected one.
            String current = appointmentId.getSelectedId();
            if (current != null && !selectedPatient.equals(appointmentPatient.get(current))) {
                appointmentId.setValue(null);
            }
        });

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
                allAppointments.clear();
                appointmentPatient.clear();
                for (AppointmentSummaryDTO a : items) {
                    appointmentPatient.put(a.getAppointmentId(), a.getPatientId());
                    allAppointments.add(new EntityIdComboBox.Option(a.getAppointmentId(),
                            a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()));
                }
                String selectedPatient = patientId.getSelectedId();
                if (selectedPatient == null) {
                    appointmentId.setOptions(allAppointments);
                } else {
                    appointmentId.setOptions(allAppointments.stream()
                            .filter(o -> selectedPatient.equals(appointmentPatient.get(o.id())))
                            .toList());
                }
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

            List<InvoiceDTO> source = chooseInvoiceExportSource();
            if (source == null || source.isEmpty()) {
                return;
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (InvoiceDTO invoice : source) {
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

    private List<InvoiceDTO> chooseInvoiceExportSource() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("All loaded rows", "All loaded rows", "Current table view");
        dialog.setTitle("Export Invoices");
        dialog.setHeaderText("Choose what to export");
        dialog.setContentText("Export scope:");
        String choice = dialog.showAndWait().orElse(null);
        if (choice == null) {
            return List.of();
        }
        if ("Current table view".equals(choice)) {
            return new ArrayList<>(invoiceTableController.getTable().getItems());
        }
        return invoices;
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
