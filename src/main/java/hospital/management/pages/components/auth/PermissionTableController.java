package hospital.management.pages.components.auth;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.auth.PermissionDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * Permissions have no update path (renaming a resource/action pair would silently
 * detach it from whatever role_permissions rows reference it) — only add and delete,
 * so this table wires a single delete button per row instead of the usual edit+delete pair.
 */
public class PermissionTableController extends PaginatedTableController<PermissionDTO> {

    @FXML private TableColumn<PermissionDTO, String> permissionIdColumn;
    @FXML private TableColumn<PermissionDTO, String> resourceColumn;
    @FXML private TableColumn<PermissionDTO, String> actionColumn;
    @FXML private TableColumn<PermissionDTO, Void>   actionsColumn;

    private Consumer<PermissionDTO> onDelete;

    public void setOnDelete(Consumer<PermissionDTO> onDelete) {
        this.onDelete = onDelete;
    }

    @Override
    protected void configureColumns() {
        permissionIdColumn.setVisible(false);
        resourceColumn.setCellValueFactory(new PropertyValueFactory<>("resource"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        addSortOption("Resource", resourceColumn);
        addSortOption("Action", actionColumn);
        wireDeleteColumn(actionsColumn);
    }

    @Override
    protected boolean matches(PermissionDTO permission, String lowerQuery) {
        return (permission.getResource() != null && permission.getResource().toLowerCase().contains(lowerQuery))
                || (permission.getAction() != null && permission.getAction().toLowerCase().contains(lowerQuery));
    }

    private void wireDeleteColumn(TableColumn<PermissionDTO, Void> actionsColumn) {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("", new FontIcon("fas-trash"));
            {
                deleteBtn.getStyleClass().addAll("row-action-btn", "danger");
                Tooltip.install(deleteBtn, new Tooltip("Delete permission"));
                deleteBtn.setOnAction(e -> {
                    if (onDelete != null) onDelete.accept(getTableView().getItems().get(getIndex()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }
}
