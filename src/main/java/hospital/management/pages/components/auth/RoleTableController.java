package hospital.management.pages.components.auth;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.auth.RoleDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
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
        roleIdColumn.setVisible(false);
        roleNameColumn.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        permissionCountColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(formatPermissionCount(permissionCountResolver.apply(cell.getValue()))));
        Label permissionHeader = new Label("Permissions");
        Tooltip.install(permissionHeader, new Tooltip(
                "Total permission grants for this role (every checked action, across every resource, on "
                        + "the Update Role dialog). Click “View” on a role to see the full breakdown "
                        + "by resource."));
        permissionCountColumn.setGraphic(permissionHeader);
        permissionCountColumn.setText("");
        addSortOption("Role Name", roleNameColumn);
        addSortOption("Permissions", permissionCountColumn);
        wireActionsColumn(actionsColumn);
    }

    /** Spells the count out ("12 permissions") instead of a bare number, so the column is
     *  self-explanatory without needing to hover the header tooltip first. */
    private static String formatPermissionCount(String rawCount) {
        return "1".equals(rawCount) ? "1 permission" : rawCount + " permissions";
    }

    @Override
    protected boolean matches(RoleDTO role, String lowerQuery) {
        String name = role.getRoleName();
        return name != null && name.toLowerCase().contains(lowerQuery);
    }
}
