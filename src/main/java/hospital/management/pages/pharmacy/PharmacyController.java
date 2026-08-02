package hospital.management.pages.pharmacy;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.pharmacy.MedicalInventoryDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicationDAOImpl;
import hospital.management.backend.model.pharmacy.MedicalInventory;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.backend.service.pharmacy.PharmacyServiceImpl;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.pharmacy.MedicalInventoryTableController;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PharmacyController extends BasePageController {

    private final PharmacyServiceImpl pharmacyService = new PharmacyServiceImpl(
        new MedicationDAOImpl(), new MedicalInventoryDAOImpl());

    @FXML private TabPane pharmacyTabs;

    // Inventory tab
    @FXML private TextField inventorySearchField;
    @FXML private Button    addMedBtn;
    @FXML private MedicalInventoryTableController inventoryTableController;

    // Low stock tab (same underlying data, filtered)
    @FXML private MedicalInventoryTableController lowStockTableController;

    // Pending prescriptions tab
    @FXML private PrescriptionTableController pendingPrescriptionsTableController;

    private final List<MedicalInventory> inventory = new ArrayList<>();
    private final List<Prescription> pendingPrescriptions = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PHARMACY);

        addMedBtn.setOnAction(e -> openInventoryDialog(null));
        inventorySearchField.textProperty().addListener((obs, o, n) -> applyFilter());

        inventoryTableController.setRowActions(this::openInventoryDialog, this::confirmDeleteInventory, this::viewInventoryDetail);
        lowStockTableController.setRowActions(this::openInventoryDialog, this::confirmDeleteInventory, this::viewInventoryDetail);

        pendingPrescriptionsTableController.setItems(pendingPrescriptions);

        refreshInventoryTables();
    }

    private void applyFilter() {
        inventoryTableController.filter(inventorySearchField.getText());
    }

    private List<MedicalInventory> computeLowStock() {
        return inventory.stream()
                .filter(i -> i.getQuantityInStock() != null && i.getReorderLevel() != null
                        && i.getQuantityInStock() <= i.getReorderLevel())
                .toList();
    }

    private void refreshInventoryTables() {
        inventoryTableController.setItems(inventory);
        lowStockTableController.setItems(computeLowStock());
    }

    private void viewInventoryDetail(MedicalInventory item) {
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

    private void confirmDeleteInventory(MedicalInventory item) {
        confirm("Delete Inventory Item",
                "Are you sure you want to delete batch " + item.getBatchNumber() + "? This cannot be undone.",
                () -> {
                    inventory.remove(item);
                    refreshInventoryTables();
                    toastSuccess("Inventory item deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (item == null) or Update mode. */
    private void openInventoryDialog(MedicalInventory item) {
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

            MedicalInventory target = addMode ? new MedicalInventory() : item;
            if (addMode) target.setInventoryId(UUID.randomUUID().toString());
            target.setMedicationId(medId);
            target.setBatchNumber(batch);
            target.setExpiryDate(expiryDate.getValue());
            target.setQuantityInStock(qty);
            target.setReorderLevel(reorder);
            target.setSupplier(supplier.getText());

            if (addMode) inventory.add(target);
            refreshInventoryTables();
            formDialogController.close();
            toastSuccess(addMode ? "Medication added." : "Medication updated.");
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
    private void loadMedicationDropdown(LoadingIdComboBox medicationIdField, List<Control> otherFields, MedicalInventory existing) {
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
