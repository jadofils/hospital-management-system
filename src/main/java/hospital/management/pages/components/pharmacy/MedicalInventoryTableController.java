package hospital.management.pages.components.pharmacy;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.pharmacy.MedicalInventory;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Reused for BOTH the Inventory tab (all rows) and the Low Stock tab
 * (a pre-filtered subset). Filtering by reorder level happens at the page
 * controller level — this class only renders whatever list it's given.
 */
public class MedicalInventoryTableController extends PaginatedTableController<MedicalInventory> {

    @FXML private TableColumn<MedicalInventory, String>    inventoryIdColumn;
    @FXML private TableColumn<MedicalInventory, String>    medicationIdColumn;
    @FXML private TableColumn<MedicalInventory, String>    batchNumberColumn;
    @FXML private TableColumn<MedicalInventory, java.time.LocalDate> expiryDateColumn;
    @FXML private TableColumn<MedicalInventory, Integer>   quantityInStockColumn;
    @FXML private TableColumn<MedicalInventory, Integer>   reorderLevelColumn;
    @FXML private TableColumn<MedicalInventory, String>    supplierColumn;
    @FXML private TableColumn<MedicalInventory, Void>      actionsColumn;

    @Override
    protected void configureColumns() {
        inventoryIdColumn.setCellValueFactory(new PropertyValueFactory<>("inventoryId"));
        medicationIdColumn.setCellValueFactory(new PropertyValueFactory<>("medicationId"));
        batchNumberColumn.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        expiryDateColumn.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        quantityInStockColumn.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        reorderLevelColumn.setCellValueFactory(new PropertyValueFactory<>("reorderLevel"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(MedicalInventory item, String lowerQuery) {
        return (item.getMedicationId() != null && item.getMedicationId().toLowerCase().contains(lowerQuery))
                || (item.getBatchNumber() != null && item.getBatchNumber().toLowerCase().contains(lowerQuery))
                || (item.getSupplier() != null && item.getSupplier().toLowerCase().contains(lowerQuery));
    }
}
