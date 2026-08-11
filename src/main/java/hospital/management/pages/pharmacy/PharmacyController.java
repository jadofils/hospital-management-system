package hospital.management.pages.pharmacy;

import hospital.management.backend.utils.FxFormValidator;
import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicalInventoryDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicationDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionItemDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.pharmacy.CreateMedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.CreateMedicationDTO;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.MedicationDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.pharmacy.PharmacyServiceImpl;
import hospital.management.backend.service.pharmacy.PrescriptionServiceImpl;
import hospital.management.backend.service.pharmacy.interfaces.PrescriptionService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.pharmacy.MedicalInventoryTableController;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import hospital.management.pages.components.shared.sort.SortBarController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PharmacyController extends BasePageController {

    private final PharmacyServiceImpl pharmacyService = new PharmacyServiceImpl(
        new MedicationDAOImpl(), new MedicalInventoryDAOImpl());
    private final PrescriptionService prescriptionService =
            new PrescriptionServiceImpl(new PrescriptionDAOImpl(), new PrescriptionItemDAOImpl());
    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private TabPane pharmacyTabs;

    // Inventory tab
    @FXML private TextField inventorySearchField;
    @FXML private Button    newMedicationBtn;
    @FXML private Button    addMedBtn;
    @FXML private Button    continueBtn;
    @FXML private MedicalInventoryTableController inventoryTableController;
    @FXML private SortBarController inventorySortBarController;

    // Low stock tab (same underlying data, filtered)
    @FXML private MedicalInventoryTableController lowStockTableController;
    @FXML private SortBarController lowStockSortBarController;

    // Pending prescriptions tab
    @FXML private PrescriptionTableController pendingPrescriptionsTableController;
    @FXML private SortBarController pendingSortBarController;

    private final List<MedicalInventoryDTO> inventory = new ArrayList<>();
    private final List<PrescriptionDTO> pendingPrescriptions = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PHARMACY);

        applyCreateVisibility(newMedicationBtn, PageRoute.PHARMACY);
        applyCreateVisibility(addMedBtn, PageRoute.PHARMACY);
        newMedicationBtn.setOnAction(e -> openMedicationDialog());
        addMedBtn.setOnAction(e -> openInventoryDialog(null));
        setupContinueButton(continueBtn, PageRoute.PHARMACY);
        inventorySearchField.textProperty().addListener((obs, o, n) -> applyFilter());

        inventoryTableController.setRowActions(
            allowUpdate(PageRoute.PHARMACY, this::openInventoryDialog),
            allowDelete(PageRoute.PHARMACY, this::confirmDeleteInventory),
            allowRead(PageRoute.PHARMACY, this::viewInventoryDetail));
        lowStockTableController.setRowActions(
            allowUpdate(PageRoute.PHARMACY, this::openInventoryDialog),
            allowDelete(PageRoute.PHARMACY, this::confirmDeleteInventory),
            allowRead(PageRoute.PHARMACY, this::viewInventoryDetail));
        pendingPrescriptionsTableController.setRowActions(
            null, null, allowRead(PageRoute.PHARMACY, this::viewPrescriptionDetail));

        if (inventorySortBarController != null) {
            inventorySortBarController.setOnSort((field, asc) -> inventoryTableController.applySort(field, asc));
            inventorySortBarController.addOptions(inventoryTableController.getSortOptionLabels());
        }
        if (lowStockSortBarController != null) {
            lowStockSortBarController.setOnSort((field, asc) -> lowStockTableController.applySort(field, asc));
            lowStockSortBarController.addOptions(lowStockTableController.getSortOptionLabels());
        }
        if (pendingSortBarController != null) {
            pendingSortBarController.setOnSort((field, asc) -> pendingPrescriptionsTableController.applySort(field, asc));
            pendingSortBarController.addOptions(pendingPrescriptionsTableController.getSortOptionLabels());
        }

        refreshInventoryTables();
        refreshPendingPrescriptions();
    }

    private void applyFilter() {
        inventoryTableController.filter(inventorySearchField.getText());
    }

    private void refreshInventoryTables() {
        try {
            inventory.clear();
            List<MedicationDTO> medications = pharmacyService.findAllMedications();
            for (MedicationDTO medication : medications) {
                List<MedicalInventoryDTO> rows = pharmacyService.findStockByMedication(medication.getMedicationId());
                if (rows.isEmpty()) {
                    MedicalInventoryDTO virtualRow = new MedicalInventoryDTO();
                    virtualRow.setInventoryId("VIRTUAL:" + medication.getMedicationId());
                    virtualRow.setMedicationId(medication.getMedicationId());
                    virtualRow.setBatchNumber("No stock batch yet");
                    virtualRow.setQuantityInStock(0);
                    virtualRow.setReorderLevel(0);
                    virtualRow.setSupplier("-");
                    inventory.add(virtualRow);
                } else {
                    inventory.addAll(rows);
                }
            }
            inventoryTableController.setItems(inventory);
            lowStockTableController.setItems(pharmacyService.findLowStock());
        } catch (Exception e) {
            toastError("Failed to load inventory: " + e.getMessage());
        }
    }

    private void refreshPendingPrescriptions() {
        try {
            pendingPrescriptions.clear();
            List<AppointmentSummaryDTO> appointments =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO appointment : appointments) {
                try {
                    pendingPrescriptions.add(prescriptionService.findByAppointment(appointment.getAppointmentId()));
                } catch (ResourceNotFoundException ignored) {
                }
            }
            pendingPrescriptionsTableController.setItems(pendingPrescriptions);
        } catch (Exception e) {
            toastError("Failed to load prescriptions: " + e.getMessage());
        }
    }

    private void viewPrescriptionDetail(PrescriptionDTO p) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            AppointmentDTO appointment = appointmentService.findById(p.getAppointmentId());
            fields.put("Patient", entityLookupService.patientLabel(appointment.getPatientId()));
            fields.put("Doctor", entityLookupService.doctorLabel(appointment.getDoctorId()));
            fields.put("Appointment Date", appointment.getAppointmentDate() == null
                    ? null : appointment.getAppointmentDate().toLocalDate().toString());
        } catch (Exception ex) {
            fields.put("Appointment", "Unknown");
        }
        fields.put("Date Issued", p.getDateIssued() == null ? null : p.getDateIssued().toString());
        fields.put("Status", p.getStatus());
        if (p.getItems() != null) fields.put("Items", String.valueOf(p.getItems().size()));
        detailViewController.show("Prescription Details", "fas-prescription", fields);
    }

    private void viewInventoryDetail(MedicalInventoryDTO item) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Medication", pharmacyService.findMedicationById(item.getMedicationId()).getName());
        } catch (Exception ex) {
            fields.put("Medication", "Unknown");
        }
        fields.put("Batch Number", item.getBatchNumber());
        fields.put("Expiry Date", item.getExpiryDate() == null ? null : item.getExpiryDate().toString());
        fields.put("Quantity In Stock", item.getQuantityInStock() == null ? null : String.valueOf(item.getQuantityInStock()));
        fields.put("Reorder Level", item.getReorderLevel() == null ? null : String.valueOf(item.getReorderLevel()));
        fields.put("Supplier", item.getSupplier());
        detailViewController.show("Medication Details", "fas-pills", fields);
    }

    private void confirmDeleteInventory(MedicalInventoryDTO item) {
        toast("Inventory batches can't be deleted — update the batch instead.", NotificationType.INFO);
    }

    private void openMedicationDialog() {
        TextField nameField = new TextField();
        TextField genericNameField = new TextField();
        TextField formField = new TextField();
        TextField unitPriceField = new TextField();

        nameField.setPromptText("e.g. Amoxicillin");
        genericNameField.setPromptText("e.g. Amoxicillin trihydrate (optional)");
        formField.setPromptText("e.g. Capsule, Tablet, Syrup");
        unitPriceField.setPromptText("e.g. 5.00");

        List.of(nameField, genericNameField, formField, unitPriceField)
                .forEach(f -> f.getStyleClass().add("form-input"));

        FxFormValidator.attachRequired(nameField,    null, "Medication name");
        FxFormValidator.attachRequired(formField,    null, "Form");
        FxFormValidator.attachMaxLength(genericNameField, null, 200, "Generic name");
        unitPriceField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) { FxFormValidator.clearStyle(unitPriceField); return; }
            try { new BigDecimal(val.trim()); FxFormValidator.applyStyle(unitPriceField, true); }
            catch (NumberFormatException e) { FxFormValidator.applyStyle(unitPriceField, false); }
        });

        formDialogController.open("New Medication", "fas-capsules", true, v -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String genericName = genericNameField.getText() == null ? "" : genericNameField.getText().trim();
            String form = formField.getText() == null ? "" : formField.getText().trim();
            String unitPriceText = unitPriceField.getText() == null ? "" : unitPriceField.getText().trim();

            if (name.isBlank()) {
                formDialogController.setError("Medication name is required.");
                FxFormValidator.applyStyle(nameField, false);
                formDialogController.setLoading(false);
                return;
            }
            if (form.isBlank()) {
                formDialogController.setError("Form is required (e.g. Capsule, Tablet).");
                FxFormValidator.applyStyle(formField, false);
                formDialogController.setLoading(false);
                return;
            }
            if (unitPriceText.isBlank()) {
                formDialogController.setError("Unit price is required.");
                FxFormValidator.applyStyle(unitPriceField, false);
                formDialogController.setLoading(false);
                return;
            }

            BigDecimal unitPrice;
            try {
                unitPrice = new BigDecimal(unitPriceText);
                if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    formDialogController.setError("Unit price must be greater than zero.");
                    FxFormValidator.applyStyle(unitPriceField, false);
                    formDialogController.setLoading(false);
                    return;
                }
            } catch (NumberFormatException ex) {
                formDialogController.setError("Unit price must be a valid number.");
                FxFormValidator.applyStyle(unitPriceField, false);
                formDialogController.setLoading(false);
                return;
            }

            try {
                pharmacyService.addMedication(new CreateMedicationDTO(name, genericName, form, unitPrice));
                refreshInventoryTables();
                formDialogController.close();
                toastSuccess("Medication created and shown in inventory table. Add a batch to stock it.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to create medication: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Medication Name", "fas-capsules", nameField);
        formDialogController.addField("Generic Name", "fas-prescription-bottle", genericNameField);
        formDialogController.addField("Form", "fas-notes-medical", formField);
        formDialogController.addField("Unit Price", "fas-dollar-sign", unitPriceField);
    }

    /** Opens the shared form dialog in Add mode (item == null) or Update mode. */
    private void openInventoryDialog(MedicalInventoryDTO item) {
        boolean virtualRow = item != null && item.getInventoryId() != null && item.getInventoryId().startsWith("VIRTUAL:");
        boolean addMode = item == null || virtualRow;

        LoadingIdComboBox medicationIdField = new LoadingIdComboBox();
        EntityIdComboBox medicationId = medicationIdField.getComboBox();
        TextField batchNumber     = new TextField();
        DatePicker expiryDate     = new DatePicker();
        TextField quantityInStock = new TextField();
        TextField reorderLevel    = new TextField();
        TextField supplier        = new TextField();

        batchNumber.setPromptText("e.g. BATCH-2024-001");
        expiryDate.setPromptText("e.g. 2026-12-31");
        quantityInStock.setPromptText("e.g. 100");
        reorderLevel.setPromptText("e.g. 20 (optional)");
        supplier.setPromptText("e.g. Rwanda Pharma Ltd (optional)");

        List.of(batchNumber, quantityInStock, reorderLevel, supplier)
                .forEach(f -> f.getStyleClass().add("form-input"));
        medicationId.getStyleClass().add("form-combo");
        expiryDate.getStyleClass().add("form-date-picker");

        FxFormValidator.attachRequired(batchNumber,     null, "Batch number");
        FxFormValidator.attachNotPastDate(expiryDate,   null, "Expiry date");
        FxFormValidator.disallowPastDates(expiryDate);
        FxFormValidator.attachMaxLength(supplier,       null, 255, "Supplier");
        quantityInStock.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) { FxFormValidator.clearStyle(quantityInStock); return; }
            try { int q = Integer.parseInt(val.trim()); FxFormValidator.applyStyle(quantityInStock, q >= 0); }
            catch (NumberFormatException e) { FxFormValidator.applyStyle(quantityInStock, false); }
        });
        reorderLevel.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) { FxFormValidator.clearStyle(reorderLevel); return; }
            try { int r = Integer.parseInt(val.trim()); FxFormValidator.applyStyle(reorderLevel, r >= 0); }
            catch (NumberFormatException e) { FxFormValidator.applyStyle(reorderLevel, false); }
        });
        supplier.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) { FxFormValidator.clearStyle(supplier); return; }
            FxFormValidator.applyStyle(supplier, !val.trim().matches("\\d+"));
        });

        List<Control> otherFields = List.of(batchNumber, expiryDate, quantityInStock, reorderLevel, supplier);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            batchNumber.setText(item.getBatchNumber());
            expiryDate.setValue(item.getExpiryDate());
            quantityInStock.setText(item.getQuantityInStock() != null ? String.valueOf(item.getQuantityInStock()) : "");
            reorderLevel.setText(item.getReorderLevel() != null ? String.valueOf(item.getReorderLevel()) : "");
            supplier.setText(item.getSupplier());
            FxFormValidator.applyStyle(batchNumber, item.getBatchNumber() != null && !item.getBatchNumber().isBlank());
        }

        formDialogController.open(addMode ? "Add Medication" : "Update Medication", "fas-pills", addMode, v -> {
            String medId = medicationId.getSelectedId();
            String batch = batchNumber.getText() == null ? "" : batchNumber.getText().trim();

            if (medId == null) {
                formDialogController.setError("Medication is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (batch.isEmpty()) {
                formDialogController.setError("Batch number is required.");
                FxFormValidator.applyStyle(batchNumber, false);
                formDialogController.setLoading(false);
                return;
            }
            if (expiryDate.getValue() == null) {
                formDialogController.setError("Expiry date is required.");
                FxFormValidator.applyStyle(expiryDate, false);
                formDialogController.setLoading(false);
                return;
            }
            if (expiryDate.getValue().isBefore(LocalDate.now())) {
                formDialogController.setError("Expiry date cannot be in the past.");
                FxFormValidator.applyStyle(expiryDate, false);
                formDialogController.setLoading(false);
                return;
            }

            int qty;
            int reorder;
            try {
                qty = Integer.parseInt(quantityInStock.getText().trim());
                if (qty < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                formDialogController.setError("Quantity in stock must be a non-negative whole number.");
                FxFormValidator.applyStyle(quantityInStock, false);
                formDialogController.setLoading(false);
                return;
            }
            try {
                reorder = Integer.parseInt(reorderLevel.getText().trim());
                if (reorder < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                formDialogController.setError("Reorder level must be a non-negative whole number.");
                FxFormValidator.applyStyle(reorderLevel, false);
                formDialogController.setLoading(false);
                return;
            }

            String supplierText = supplier.getText() == null ? "" : supplier.getText().trim();
            if (!supplierText.isEmpty() && supplierText.matches("\\d+")) {
                formDialogController.setError("Supplier must be a name, not a number.");
                FxFormValidator.applyStyle(supplier, false);
                formDialogController.setLoading(false);
                return;
            }

            CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(
                    medId, batch, expiryDate.getValue(), qty, reorder, supplierText);

            try {
                if (addMode) {
                    pharmacyService.addStock(dto);
                } else {
                    pharmacyService.updateStock(item.getInventoryId(), dto);
                }
                refreshInventoryTables();
                formDialogController.close();
                toastSuccess(addMode ? "Inventory batch added." : "Inventory batch updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save inventory: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Medication", "fas-pills", medicationIdField);
        formDialogController.addField("Batch Number", "fas-barcode", batchNumber);
        formDialogController.addField("Expiry Date", "fas-calendar", expiryDate);
        formDialogController.addField("Quantity In Stock", "fas-boxes", quantityInStock);
        formDialogController.addField("Reorder Level", "fas-exclamation-triangle", reorderLevel);
        formDialogController.addField("Supplier", "fas-truck", supplier);

        loadMedicationDropdown(medicationIdField, otherFields, item);

        if (virtualRow && item != null) {
            medicationId.selectById(item.getMedicationId());
        }
    }

    /** Loads the medication dropdown options asynchronously, showing its own spinner while
     *  data is in flight and keeping the rest of the form disabled until it finishes loading. */
    private void loadMedicationDropdown(LoadingIdComboBox medicationIdField, List<Control> otherFields, MedicalInventoryDTO existing) {
        EntityIdComboBox medicationId = medicationIdField.getComboBox();

        medicationIdField.setLoading(true);
        formDialogController.setLoading(true);

        AsyncJobRunner.submit(
            pharmacyService::findAllMedications,
            items -> {
                medicationId.setOptions(items.stream()
                        .map(m -> new EntityIdComboBox.Option(m.getMedicationId(), m.getName()))
                        .toList());
                if (existing != null) medicationId.selectById(existing.getMedicationId());
                medicationIdField.setLoading(false);
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            },
            ex -> {
                medicationIdField.setLoading(false);
                toastError("Failed to load medications: " + ex.getMessage());
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            });
    }
}
