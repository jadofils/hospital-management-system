package hospital.management.pages.components.auth;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.auth.RoleDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.function.Function;

public class RoleTableController extends PaginatedTableController<RoleDTO> {

    @FXML private TableColumn<RoleDTO, String> roleIdColumn;
    @FXML private TableColumn<RoleDTO, String> roleNameColumn;
    @FXML private TableColumn<RoleDTO, String> permissionCountColumn;
    @FXML private TableColumn<RoleDTO, Void>   actionsColumn;

    private Function<RoleDTO, String> permissionCountResolver = r -> "0";

    /** The table has no permission-count field of its own — the page supplies how to resolve one per row. */
    public void setPermissionCountResolver(Function<RoleDTO, String> resolver) {
        this.permissionCountResolver = resolver;
    }

    @Override
    protected void configureColumns() {
        roleIdColumn.setCellValueFactory(new PropertyValueFactory<>("roleId"));
        roleNameColumn.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        permissionCountColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(permissionCountResolver.apply(cell.getValue())));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(RoleDTO role, String lowerQuery) {
        String name = role.getRoleName();
        return name != null && name.toLowerCase().contains(lowerQuery);
    }
}
