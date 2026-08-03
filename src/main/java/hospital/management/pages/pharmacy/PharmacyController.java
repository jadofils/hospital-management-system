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
import hospital.management.backend.dto.pharmacy.CreateMedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.CreateMedicationDTO;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import hospital.management.backend.dto.pharmacy.MedicationDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
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
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

    @FXML private TabPane pharmacyTabs;

    // Inventory tab
    @FXML private TextField inventorySearchField;
    @FXML private Button    newMedicationBtn;
    @FXML private Button    addMedBtn;
    @FXML private MedicalInventoryTableController inventoryTableController;

    // Low stock tab (same underlying data, filtered)
    @FXML private MedicalInventoryTableController lowStockTableController;

    // Pending prescriptions tab
    @FXML private PrescriptionTableController pendingPrescriptionsTableController;

    private final List<MedicalInventoryDTO> inventory = new ArrayList<>();
    private final List<PrescriptionDTO> pendingPrescriptions = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PHARMACY);

        applyCreateVisibility(newMedicationBtn, PageRoute.PHARMACY);
        applyCreateVisibility(addMedBtn, PageRoute.PHARMACY);
        newMedicationBtn.setOnAction(e -> openMedicationDialog());
        addMedBtn.setOnAction(e -> openInventoryDialog(null));
        inventorySearchField.textProperty().addListener((obs, o, n) -> applyFilter());

        inventoryTableController.setRowActions(
            allowUpdate(PageRoute.PHARMACY, this::openInventoryDialog),
            allowDelete(PageRoute.PHARMACY, this::confirmDeleteInventory),
            allowRead(PageRoute.PHARMACY, this::viewInventoryDetail));
        lowStockTableController.setRowActions(
            allowUpdate(PageRoute.PHARMACY, this::openInventoryDialog),
            allowDelete(PageRoute.PHARMACY, this::confirmDeleteInventory),
            allowRead(PageRoute.PHARMACY, this::viewInventoryDetail));

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
                inventory.addAll(pharmacyService.findStockByMedication(medication.getMedicationId()));
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

        List.of(nameField, genericNameField, formField, unitPriceField)
                .forEach(f -> f.getStyleClass().add("form-input"));

        formDialogController.open("New Medication", "fas-capsules", true, v -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String genericName = genericNameField.getText() == null ? "" : genericNameField.getText().trim();
            String form = formField.getText() == null ? "" : formField.getText().trim();
            String unitPriceText = unitPriceField.getText() == null ? "" : unitPriceField.getText().trim();

            if (name.isBlank() || form.isBlank() || unitPriceText.isBlank()) {
                formDialogController.setError("Medication name, form, and unit price are required.");
                formDialogController.setLoading(false);
                return;
            }

            BigDecimal unitPrice;
            try {
                unitPrice = new BigDecimal(unitPriceText);
            } catch (NumberFormatException ex) {
                formDialogController.setError("Unit price must be a valid number.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                pharmacyService.addMedication(new CreateMedicationDTO(name, genericName, form, unitPrice));
                refreshInventoryTables();
                formDialogController.close();
                toastSuccess("Medication created. You can now select it in the inventory dropdown.");
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
        boolean addMode = item == null;

        LoadingIdComboBox medicationIdField = new LoadingIdComboBox();
        EntityIdComboBox medicationId = medicationIdField.getComboBox();
        TextField batchNumber     = new TextField();
        DatePicker expiryDate     = new DatePicker();
        TextField quantityInStock = new TextField();
        TextField reorderLevel    = new TextField();
        TextField supplier        = new TextField();

        List.of(batchNumber, quantityInStock, reorderLevel, supplier)
                .forEach(f -> f.getStyleClass().add("form-input"));
        medicationId.getStyleClass().add("form-combo");
        expiryDate.getStyleClass().add("form-date-picker");

        List<Control> otherFields = List.of(batchNumber, expiryDate, quantityInStock, reorderLevel, supplier);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            batchNumber.setText(item.getBatchNumber());
            expiryDate.setValue(item.getExpiryDate());
            quantityInStock.setText(item.getQuantityInStock() != null ? String.valueOf(item.getQuantityInStock()) : "");
            reorderLevel.setText(item.getReorderLevel() != null ? String.valueOf(item.getReorderLevel()) : "");
            supplier.setText(item.getSupplier());
        }

        formDialogController.open(addMode ? "Add Medication" : "Update Medication", "fas-pills", addMode, v -> {
            String medId = medicationId.getSelectedId();
            String batch = batchNumber.getText() == null ? "" : batchNumber.getText().trim();

            if (medId == null || batch.isEmpty() || expiryDate.getValue() == null) {
                formDialogController.setError("Medication, batch number and expiry date are required.");
                formDialogController.setLoading(false);
                return;
            }

            int qty;
            int reorder;
            try {
                qty = Integer.parseInt(quantityInStock.getText().trim());
            } catch (NumberFormatException ex) {
                formDialogController.setError("Quantity in stock must be a whole number.");
                formDialogController.setLoading(false);
                return;
            }
            try {
                reorder = Integer.parseInt(reorderLevel.getText().trim());
            } catch (NumberFormatException ex) {
                formDialogController.setError("Reorder level must be a whole number.");
                formDialogController.setLoading(false);
                return;
            }

            CreateMedicalInventoryDTO dto = new CreateMedicalInventoryDTO(
                    medId, batch, expiryDate.getValue(), qty, reorder, supplier.getText());

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

        loadMedicationDropdown(medicationIdField, otherFields, addMode ? null : item);
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
