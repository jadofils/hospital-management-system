package hospital.management.pages.auth;

import hospital.management.backend.dao.auth.PermissionDAOImpl;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.RolePermissionDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dto.auth.CreateUserDTO;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.dto.auth.UpdateUserDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.auth.RoleServiceImpl;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.RoleService;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.pages.BasePageController;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.auth.UserTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersPageController extends BasePageController {

    /** Loads the whole user list in one page — an admin roster is small enough that
     *  client-side search/pagination (like every other table in this app) is simpler
     *  than wiring cursor-by-cursor server paging into the table widget. */
    private static final int FETCH_SIZE = 500;

    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    private final RoleService roleService = new RoleServiceImpl(
        new RoleDAOImpl(), new UserRoleDAOImpl(), new RolePermissionDAOImpl(), new PermissionDAOImpl());

    @FXML private UserTableController userTableController;

    @FXML private Label    totalLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button   addUserBtn;

    private List<UserDTO> users = new ArrayList<>();
    private List<RoleDTO> allRoles = new ArrayList<>();
    private final Map<String, String> roleNameByUserId = new HashMap<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.USERS);

        statusFilter.getItems().addAll("All", "Active", "Inactive");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        roleFilter.setOnAction(e -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        userTableController.setRoleNameResolver(u -> roleNameByUserId.getOrDefault(u.getUserId(), "—"));
        addUserBtn.setOnAction(e -> openUserDialog(null));
        userTableController.setRowActions(this::openUserDialog, this::confirmDeleteUser);

        loadRolesAndUsers();
    }

    private void loadRolesAndUsers() {
        try {
            allRoles = roleService.findAll();
            roleFilter.getItems().setAll("All Roles");
            allRoles.forEach(r -> roleFilter.getItems().add(r.getRoleName()));
            roleFilter.setValue("All Roles");
        } catch (Exception e) {
            toastError("Failed to load roles: " + e.getMessage());
        }
        refreshTable();
    }

    private void refreshTable() {
        try {
            users = userService.findAll(CursorPagination.firstPage(FETCH_SIZE)).getItems();
            roleNameByUserId.clear();
            for (UserDTO user : users) {
                List<RoleDTO> roles = roleService.findRolesForUser(user.getUserId());
                roleNameByUserId.put(user.getUserId(), roles.isEmpty() ? "—" : roles.get(0).getRoleName());
            }
            applyFilter();
            totalLabel.setText("Total: " + users.size() + " users");
        } catch (Exception e) {
            toastError("Failed to load users: " + e.getMessage());
        }
    }

    private void applyFilter() {
        String status = statusFilter.getValue();
        boolean statusAll = status == null || "All".equals(status);
        boolean wantActive = "Active".equals(status);

        String role = roleFilter.getValue();
        boolean roleAll = role == null || "All Roles".equals(role);

        List<UserDTO> visible = users.stream()
                .filter(u -> statusAll || Boolean.TRUE.equals(u.getIsActive()) == wantActive)
                .filter(u -> roleAll || role.equals(roleNameByUserId.get(u.getUserId())))
                .toList();

        userTableController.setItems(visible);
        userTableController.filter(searchField.getText());
    }

    private void confirmDeleteUser(UserDTO user) {
        confirm("Delete User",
                "Are you sure you want to delete " + user.getUsername() + "? This cannot be undone.",
                () -> {
                    try {
                        userService.delete(user.getUserId());
                        refreshTable();
                        toastSuccess("User deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete user: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog in Add mode (user == null) or Update mode. */
    private void openUserDialog(UserDTO user) {
        boolean addMode = user == null;
        String currentRoleName = addMode ? null : roleNameByUserId.get(user.getUserId());

        TextField username = new TextField();
        TextField email    = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<String> role = new ComboBox<>();
        ComboBox<String> active = new ComboBox<>();

        List.of(username, email, password).forEach(f -> f.getStyleClass().add("form-input"));
        role.getStyleClass().add("form-combo");
        active.getStyleClass().add("form-combo");
        allRoles.forEach(r -> role.getItems().add(r.getRoleName()));
        active.getItems().addAll("Active", "Inactive");

        if (!addMode) {
            username.setText(user.getUsername());
            username.setDisable(true); // renaming a username isn't supported by the backend yet
            email.setText(user.getEmail());
            role.setValue(currentRoleName);
            active.setValue(Boolean.TRUE.equals(user.getIsActive()) ? "Active" : "Inactive");
        } else {
            active.setValue("Active");
        }

        formDialogController.open(addMode ? "Add User" : "Update User", "fas-user", addMode, v -> {
            String un = username.getText() == null ? "" : username.getText().trim();
            String em = email.getText() == null ? "" : email.getText().trim();
            if ((addMode && un.isEmpty()) || em.isEmpty() || active.getValue() == null) {
                formDialogController.setError("Username, email and status are required.");
                formDialogController.setLoading(false);
                return;
            }
            if (addMode && password.getText().length() < 8) {
                formDialogController.setError("Password must be at least 8 characters.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                boolean isActive = "Active".equals(active.getValue());
                String selectedRoleName = role.getValue();
                UserDTO saved;

                if (addMode) {
                    saved = userService.create(new CreateUserDTO(null, un, password.getText(), em));
                    if (!isActive) saved = userService.update(new UpdateUserDTO(saved.getUserId(), em, false));
                } else {
                    saved = userService.update(new UpdateUserDTO(user.getUserId(), em, isActive));
                }

                if (selectedRoleName != null && !selectedRoleName.equals(currentRoleName)) {
                    RoleDTO newRole = allRoles.stream()
                            .filter(r -> r.getRoleName().equals(selectedRoleName))
                            .findFirst().orElse(null);
                    if (newRole != null) {
                        if (currentRoleName != null) {
                            RoleDTO oldRole = allRoles.stream()
                                    .filter(r -> r.getRoleName().equals(currentRoleName))
                                    .findFirst().orElse(null);
                            if (oldRole != null) roleService.revokeFromUser(saved.getUserId(), oldRole.getRoleId());
                        }
                        roleService.assignToUser(saved.getUserId(), newRole.getRoleId());
                    }
                }

                refreshTable();
                formDialogController.close();
                toastSuccess(addMode ? "User added." : "User updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save user: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Username", "fas-user", username);
        if (addMode) formDialogController.addField("Password", "fas-lock", password);
        formDialogController.addField("Email", "fas-envelope", email);
        formDialogController.addField("Role", "fas-user-tag", role);
        formDialogController.addField("Status", "fas-toggle-on", active);
    }
}
