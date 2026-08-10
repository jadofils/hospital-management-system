package hospital.management.pages.pharmacy;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicalInventoryDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicationDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionItemDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionItemDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.pharmacy.PrescriptionServiceImpl;
import hospital.management.backend.service.pharmacy.interfaces.PrescriptionService;
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.backend.service.pharmacy.PharmacyServiceImpl;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import hospital.management.pages.utils.CsvUiIO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrescriptionsController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final PrescriptionService prescriptionService =
            new PrescriptionServiceImpl(new PrescriptionDAOImpl(), new PrescriptionItemDAOImpl());
    private final PharmacyServiceImpl pharmacyService = new PharmacyServiceImpl(
        new MedicationDAOImpl(), new MedicalInventoryDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private PrescriptionTableController prescriptionTableController;

    @FXML private TextField  searchField;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button     newPrescriptionBtn;
    @FXML private Button     importBtn;
    @FXML private Button     exportBtn;

    private final List<PrescriptionDTO> prescriptions = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PRESCRIPTIONS);

        searchField.setPromptText("Search by ID or medication…");
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        // Date range filter with validation
        if (fromDatePicker != null) fromDatePicker.setPromptText("From date");
        if (toDatePicker != null)   toDatePicker.setPromptText("To date");
        FxFormValidator.attachDateRange(fromDatePicker, toDatePicker, null);
        if (fromDatePicker != null) fromDatePicker.setOnAction(e -> applyFilter());
        if (toDatePicker   != null) toDatePicker.setOnAction(e -> applyFilter());

        applyCreateVisibility(newPrescriptionBtn, PageRoute.PRESCRIPTIONS);
        applyCreateVisibility(importBtn, PageRoute.PRESCRIPTIONS);
        boolean canExport = canRead(PageRoute.PRESCRIPTIONS);
        exportBtn.setVisible(canExport);
        exportBtn.setManaged(canExport);

        newPrescriptionBtn.setOnAction(e -> openPrescriptionDialog());
        importBtn.setOnAction(e -> withSpinner(importBtn, this::importPrescriptions));
        exportBtn.setOnAction(e -> withSpinner(exportBtn, this::exportPrescriptions));
        prescriptionTableController.setRowActions(
            canUpdate(PageRoute.PRESCRIPTIONS) ? p -> toast("Prescriptions can't be edited once issued.", NotificationType.INFO) : null,
            allowDelete(PageRoute.PRESCRIPTIONS, this::confirmDeletePrescription),
            allowRead(PageRoute.PRESCRIPTIONS, this::viewPrescriptionDetail));

        refreshTable();
    }

    private void applyFilter() {
        prescriptionTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            prescriptions.clear();
            List<AppointmentSummaryDTO> appointments =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO appointment : appointments) {
                try {
                    prescriptions.add(prescriptionService.findByAppointment(appointment.getAppointmentId()));
                } catch (ResourceNotFoundException ignored) {
                }
            }
            prescriptionTableController.setItems(prescriptions);
        } catch (Exception e) {
            toastError("Failed to load prescriptions: " + e.getMessage());
        }
    }

    private void exportPrescriptions() {
        try {
            if (prescriptions.isEmpty()) {
                toastError("No prescriptions available to export.");
                return;
            }
            List<PrescriptionDTO> source = choosePrescriptionExportSource();
            if (source.isEmpty()) {
                return;
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (PrescriptionDTO prescription : source) {
                if (prescription.getItems() == null || prescription.getItems().isEmpty()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("prescription_id", prescription.getPrescriptionId());
                    row.put("appointment_id", prescription.getAppointmentId());
                    row.put("date_issued", prescription.getDateIssued());
                    row.put("medication_id", "");
                    row.put("dosage", "");
                    row.put("quantity", "");
                    row.put("instructions", "");
                    rows.add(row);
                    continue;
                }

                for (hospital.management.backend.dto.pharmacy.PrescriptionItemDTO item : prescription.getItems()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("prescription_id", prescription.getPrescriptionId());
                    row.put("appointment_id", prescription.getAppointmentId());
                    row.put("date_issued", prescription.getDateIssued());
                    row.put("medication_id", item.getMedicationId());
                    row.put("dosage", item.getDosage());
                    row.put("quantity", item.getQuantity());
                    row.put("instructions", item.getInstructions());
                    rows.add(row);
                }
            }

            boolean saved = CsvUiIO.exportRows(exportBtn.getScene().getWindow(), "prescriptions.csv", rows);
            if (saved) {
                toastSuccess("Prescriptions exported successfully.");
            }
        } catch (Exception e) {
            toastError("Failed to export prescriptions: " + e.getMessage());
        }
    }

    private List<PrescriptionDTO> choosePrescriptionExportSource() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("All loaded rows", "All loaded rows", "Current table view");
        dialog.setTitle("Export Prescriptions");
        dialog.setHeaderText("Choose what to export");
        dialog.setContentText("Export scope:");
        String choice = dialog.showAndWait().orElse(null);
        if (choice == null) {
            return List.of();
        }
        if ("Current table view".equals(choice)) {
            return new ArrayList<>(prescriptionTableController.getTable().getItems());
        }
        return prescriptions;
    }

    private void importPrescriptions() {
        try {
            List<Map<String, String>> rows = CsvUiIO.importRows(importBtn.getScene().getWindow(), "Import Prescriptions");
            if (rows.isEmpty()) {
                return;
            }

            int ok = 0;
            int failed = 0;
            for (Map<String, String> row : rows) {
                try {
                    String appointmentId = value(row, "appointment_id");
                    LocalDate dateIssued = LocalDate.parse(value(row, "date_issued"));
                    String medicationId = value(row, "medication_id");
                    int quantity = Integer.parseInt(value(row, "quantity"));
                    CreatePrescriptionItemDTO item = new CreatePrescriptionItemDTO(
                            medicationId,
                            value(row, "dosage"),
                            quantity,
                            value(row, "instructions"));
                    prescriptionService.issue(new CreatePrescriptionDTO(appointmentId, dateIssued, List.of(item)));
                    ok++;
                } catch (Exception ex) {
                    failed++;
                }
            }

            refreshTable();
            if (failed == 0) {
                toastSuccess("Imported " + ok + " prescription row(s).");
            } else {
                toastError("Imported " + ok + " prescription row(s), failed " + failed + ".");
            }
        } catch (Exception e) {
            toastError("Failed to import prescriptions: " + e.getMessage());
        }
    }

    private String value(Map<String, String> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key).trim();
            }
        }
        return "";
    }

    private void viewPrescriptionDetail(PrescriptionDTO prescription) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Appointment", entityLookupService.appointmentLabel(prescription.getAppointmentId()));
        } catch (Exception ex) {
            toastError("Failed to resolve appointment: " + ex.getMessage());
        }
        fields.put("Date Issued", prescription.getDateIssued() == null ? null : prescription.getDateIssued().toString());
        if (prescription.getItems() != null && !prescription.getItems().isEmpty()) {
            StringBuilder items = new StringBuilder();
            for (hospital.management.backend.dto.pharmacy.PrescriptionItemDTO item : prescription.getItems()) {
                items.append("• ").append(item.getDosage() == null ? "" : item.getDosage())
                        .append(" × ").append(item.getQuantity() == null ? "" : item.getQuantity())
                        .append(" (").append(item.getInstructions() == null ? "" : item.getInstructions())
                        .append(")\n");
            }
            fields.put("Items", items.toString());
        }
        detailViewController.show("Prescription Details", "fas-prescription", fields);
    }

    private void confirmDeletePrescription(PrescriptionDTO prescription) {
        confirm("Delete Prescription",
                "Are you sure you want to delete prescription " + prescription.getPrescriptionId() + "? This cannot be undone.",
                () -> {
                    try {
                        prescriptionService.delete(prescription.getPrescriptionId());
                        refreshTable();
                        toastSuccess("Prescription deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete prescription: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog to issue a new prescription (with at least one line item). */
    private void openPrescriptionDialog() {
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        DatePicker dateIssued = new DatePicker();

        LoadingIdComboBox medicationField = new LoadingIdComboBox();
        EntityIdComboBox medicationId = medicationField.getComboBox();
        TextField dosage        = new TextField();
        TextField quantity      = new TextField();
        TextField instructions  = new TextField();
        dosage.setPromptText("e.g. 500mg twice daily");
        quantity.setPromptText("e.g. 30");
        instructions.setPromptText("e.g. Take with food (optional)");
        Button addItemBtn    = new Button("Add Item");
        Button removeItemBtn = new Button("Remove Selected");
        ListView<String> itemsList = new ListView<>();
        itemsList.setPrefHeight(110);

        appointmentId.getStyleClass().add("form-combo");
        dateIssued.getStyleClass().add("form-date-picker");
        List.of(dosage, quantity, instructions).forEach(f -> f.getStyleClass().add("form-input"));
        medicationId.getStyleClass().add("form-combo");
        addItemBtn.getStyleClass().add("secondary-button");
        removeItemBtn.getStyleClass().add("secondary-button");

        // Real-time validation: date issued must be provided
        FxFormValidator.attachDateRequired(dateIssued, null, "Date issued");
        FxFormValidator.attachRequired(dosage, null, "Dosage");

        List<Control> otherFields = List.of(dateIssued);
        otherFields.forEach(f -> f.setDisable(true));
        List<Control> itemEditorFields = List.of(dosage, quantity, instructions, addItemBtn, removeItemBtn);
        itemEditorFields.forEach(f -> f.setDisable(true));

        List<CreatePrescriptionItemDTO> draftItems = new ArrayList<>();

        addItemBtn.setOnAction(e -> {
            String medId = medicationId.getSelectedId();
            String medLabel = medicationId.getValue() == null ? "" : medicationId.getValue().label();
            String qtyText = quantity.getText() == null ? "" : quantity.getText().trim();
            if (medId == null) {
                toastError("Select a medication first.");
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyText);
            } catch (NumberFormatException ex) {
                toastError("Quantity must be a whole number.");
                return;
            }
            if (qty <= 0) {
                toastError("Quantity must be greater than zero.");
                return;
            }
            draftItems.add(new CreatePrescriptionItemDTO(medId, dosage.getText(), qty, instructions.getText()));
            itemsList.getItems().add(medLabel + " | " + dosage.getText() + " | qty " + qty
                    + (instructions.getText() == null || instructions.getText().isBlank() ? "" : " | " + instructions.getText()));
            dosage.clear();
            quantity.clear();
            instructions.clear();
        });

        removeItemBtn.setOnAction(e -> {
            int index = itemsList.getSelectionModel().getSelectedIndex();
            if (index < 0) {
                toastError("Select an item in the list to remove it.");
                return;
            }
            itemsList.getItems().remove(index);
            draftItems.remove(index);
        });

        VBox itemsBox = new VBox(6);
        HBox itemInputRow = new HBox(6, medicationField, dosage, quantity, instructions, addItemBtn);
        itemInputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        itemsBox.getChildren().addAll(itemInputRow, itemsList, removeItemBtn);

        formDialogController.open("New Prescription", "fas-prescription", true, v -> {
            String apptId = appointmentId.getSelectedId();
            if (apptId == null || dateIssued.getValue() == null) {
                formDialogController.setError("Appointment and date issued are required.");
                formDialogController.setLoading(false);
                return;
            }
            if (draftItems.isEmpty()) {
                formDialogController.setError("A prescription must include at least one medication item.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                prescriptionService.issue(new CreatePrescriptionDTO(apptId, dateIssued.getValue(), List.copyOf(draftItems)));
                refreshTable();
                formDialogController.close();
                toastSuccess("Prescription issued.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to issue prescription: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Date Issued", "fas-calendar", dateIssued);
        formDialogController.addField("Medication", "fas-pills", medicationField);
        formDialogController.addRow(itemsBox);

        loadPrescriptionDropdown(appointmentIdField, medicationField, otherFields, itemEditorFields);
    }

    /** Loads the appointment and medication dropdown options asynchronously, showing each
     *  dropdown's own spinner while its data is in flight and keeping the rest of the form
     *  disabled until both have finished loading. */
    private void loadPrescriptionDropdown(LoadingIdComboBox appointmentIdField, LoadingIdComboBox medicationField,
                                          List<Control> otherFields, List<Control> itemEditorFields) {
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox medicationId = medicationField.getComboBox();

        appointmentIdField.setLoading(true);
        medicationField.setLoading(true);
        formDialogController.setLoading(true);

        java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(2);
        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                itemEditorFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

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

        AsyncJobRunner.submit(
            pharmacyService::findAllMedications,
            items -> {
                medicationId.setOptions(items.stream()
                        .map(m -> new EntityIdComboBox.Option(m.getMedicationId(), m.getName())).toList());
                medicationField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                medicationField.setLoading(false);
                toastError("Failed to load medications: " + ex.getMessage());
                onOneLoaded.run();
            });
    }
}
