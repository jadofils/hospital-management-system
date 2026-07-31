package hospital.management.pages.components.auth;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.user.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class UserTableController extends PaginatedTableController<User> {

    @FXML private TableColumn<User, String> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        statusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(Boolean.TRUE.equals(cell.getValue().getIsActive()) ? "Active" : "Inactive"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(User user, String lowerQuery) {
        String username = user.getUsername();
        String email = user.getEmail();
        return (username != null && username.toLowerCase().contains(lowerQuery))
                || (email != null && email.toLowerCase().contains(lowerQuery));
    }
}
