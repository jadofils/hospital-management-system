package hospital.management.pages.components.pharmacy;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.pharmacy.MedicalInventoryDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Reused for BOTH the Inventory tab (all rows) and the Low Stock tab
 * (a pre-filtered subset). Filtering by reorder level happens at the page
 * controller level — this class only renders whatever list it's given.
 */
public class MedicalInventoryTableController extends PaginatedTableController<MedicalInventoryDTO> {

    @FXML private TableColumn<MedicalInventoryDTO, String>    inventoryIdColumn;
    @FXML private TableColumn<MedicalInventoryDTO, String>    medicationIdColumn;
    @FXML private TableColumn<MedicalInventoryDTO, String>    batchNumberColumn;
    @FXML private TableColumn<MedicalInventoryDTO, java.time.LocalDate> expiryDateColumn;
    @FXML private TableColumn<MedicalInventoryDTO, Integer>   quantityInStockColumn;
    @FXML private TableColumn<MedicalInventoryDTO, Integer>   reorderLevelColumn;
    @FXML private TableColumn<MedicalInventoryDTO, String>    supplierColumn;
    @FXML private TableColumn<MedicalInventoryDTO, String>    stockAlertColumn;
    @FXML private TableColumn<MedicalInventoryDTO, Void>      actionsColumn;

    @Override
    protected void configureColumns() {
        inventoryIdColumn.setCellValueFactory(new PropertyValueFactory<>("inventoryId"));
        medicationIdColumn.setCellValueFactory(new PropertyValueFactory<>("medicationId"));
        batchNumberColumn.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        expiryDateColumn.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        quantityInStockColumn.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        reorderLevelColumn.setCellValueFactory(new PropertyValueFactory<>("reorderLevel"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        stockAlertColumn.setCellValueFactory(new PropertyValueFactory<>("stockAlert"));
        stockAlertColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String alert, boolean empty) {
                super.updateItem(alert, empty);
                if (empty || alert == null) { setGraphic(null); return; }
                Label badge = new Label(alert);
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add(switch (alert) {
                    case "Out of Stock", "Critical" -> "status-cancelled";
                    case "Low"                      -> "status-pending";
                    default                         -> "status-discharged";
                });
                setGraphic(badge);
            }
        });
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(MedicalInventoryDTO item, String lowerQuery) {
        String qty      = item.getQuantityInStock() != null ? String.valueOf(item.getQuantityInStock()) : "";
        String reorder  = item.getReorderLevel() != null ? String.valueOf(item.getReorderLevel()) : "";
        String expiry   = item.getExpiryDate() != null ? item.getExpiryDate().toString() : "";
        return safe(item.getMedicationId()).contains(lowerQuery)
            || safe(item.getBatchNumber()).contains(lowerQuery)
            || safe(item.getSupplier()).contains(lowerQuery)
            || safe(item.getStockAlert()).contains(lowerQuery)
            || expiry.contains(lowerQuery)
            || qty.contains(lowerQuery)
            || reorder.contains(lowerQuery);
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(); }
}
