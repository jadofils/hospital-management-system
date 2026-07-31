package hospital.management.pages.auth;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.user.User;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.auth.UserTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UsersPageController extends BasePageController {

    @FXML private UserTableController userTableController;

    @FXML private Label    totalLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button   addUserBtn;

    private final List<User> users = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.USERS);

        roleFilter.getItems().addAll("All Roles", "ADMIN", "DOCTOR", "RECEPTIONIST", "ANALYST", "PHARMACIST");
        roleFilter.setValue("All Roles");

        statusFilter.getItems().addAll("All", "Active", "Inactive");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        // Role has no backing field on User yet, so it's a no-op stub for now.
        roleFilter.setOnAction(e -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        addUserBtn.setOnAction(e -> openUserDialog(null));
        userTableController.setRowActions(this::openUserDialog, this::confirmDeleteUser);

        refreshTable();
    }

    private void applyFilter() {
        // Status is the only filter backed by real data (isActive); role filtering
        // is left as a stub until a roles join is modeled.
        String status = statusFilter.getValue();
        boolean statusAll = status == null || "All".equals(status);
        boolean wantActive = "Active".equals(status);

        List<User> visible = statusAll ? users
                : users.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive()) == wantActive).toList();

        userTableController.setItems(visible);
        userTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        applyFilter();
        totalLabel.setText("Total: " + users.size() + " users");
    }

    private void confirmDeleteUser(User user) {
        confirm("Delete User",
                "Are you sure you want to delete " + user.getUsername() + "? This cannot be undone.",
                () -> {
                    users.remove(user);
                    refreshTable();
                    toastSuccess("User deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (user == null) or Update mode. */
    private void openUserDialog(User user) {
        boolean addMode = user == null;

        TextField username = new TextField();
        TextField email    = new TextField();
        ComboBox<String> active = new ComboBox<>();

        List.of(username, email).forEach(f -> f.getStyleClass().add("form-input"));
        active.getStyleClass().add("form-combo");
        active.getItems().addAll("Active", "Inactive");

        if (!addMode) {
            username.setText(user.getUsername());
            email.setText(user.getEmail());
            active.setValue(Boolean.TRUE.equals(user.getIsActive()) ? "Active" : "Inactive");
        } else {
            active.setValue("Active");
        }

        formDialogController.open(addMode ? "Add User" : "Update User", "fas-user", addMode, v -> {
            String un = username.getText() == null ? "" : username.getText().trim();
            String em = email.getText() == null ? "" : email.getText().trim();
            if (un.isEmpty() || em.isEmpty() || active.getValue() == null) {
                formDialogController.setError("Username, email and status are required.");
                formDialogController.setLoading(false);
                return;
            }

            User target = addMode ? new User() : user;
            if (addMode) target.setUserId(UUID.randomUUID().toString());
            target.setUsername(un);
            target.setEmail(em);
            target.setIsActive("Active".equals(active.getValue()));

            if (addMode) users.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "User added." : "User updated.");
        });

        formDialogController.addField("Username", "fas-user", username);
        formDialogController.addField("Email", "fas-envelope", email);
        formDialogController.addField("Status", "fas-toggle-on", active);
    }
}
