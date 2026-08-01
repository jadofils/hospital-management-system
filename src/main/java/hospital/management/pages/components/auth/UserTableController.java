package hospital.management.pages.components.auth;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.auth.UserDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.function.Function;

public class UserTableController extends PaginatedTableController<UserDTO> {

    @FXML private TableColumn<UserDTO, String> idColumn;
    @FXML private TableColumn<UserDTO, String> usernameColumn;
    @FXML private TableColumn<UserDTO, String> emailColumn;
    @FXML private TableColumn<UserDTO, String> roleColumn;
    @FXML private TableColumn<UserDTO, String> statusColumn;
    @FXML private TableColumn<UserDTO, Void>   actionsColumn;

    private Function<UserDTO, String> roleNameResolver = u -> "—";

    /** The table has no role field of its own — the page supplies how to resolve one per row. */
    public void setRoleNameResolver(Function<UserDTO, String> resolver) {
        this.roleNameResolver = resolver;
    }

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(cell -> new SimpleStringProperty(roleNameResolver.apply(cell.getValue())));
        statusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(Boolean.TRUE.equals(cell.getValue().getIsActive()) ? "Active" : "Inactive"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(UserDTO user, String lowerQuery) {
        String username = user.getUsername();
        String email = user.getEmail();
        return (username != null && username.toLowerCase().contains(lowerQuery))
                || (email != null && email.toLowerCase().contains(lowerQuery));
    }
}
