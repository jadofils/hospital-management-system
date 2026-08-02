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
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.BasePageController;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.auth.UserTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        userTableController.setRowActions(this::openUserDialog, this::confirmDeleteUser, this::viewUserDetail);
        userTableController.setOnChangeStatus(this::confirmToggleActive);

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

    private void viewUserDetail(UserDTO user) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Username", user.getUsername());
        fields.put("Email", user.getEmail());
        fields.put("Role", roleNameByUserId.getOrDefault(user.getUserId(), "—"));
        fields.put("Status", Boolean.TRUE.equals(user.getIsActive()) ? "Active" : "Inactive");
        fields.put("Created At", user.getCreatedAt() == null ? null : user.getCreatedAt().toString());
        detailViewController.show("User Details", "fas-user", fields);
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

    /** Dedicated activate/deactivate action, kept out of the Add/Edit form (binary, so a confirm suffices — no mini-dialog needed). */
    private void confirmToggleActive(UserDTO user) {
        boolean currentlyActive = Boolean.TRUE.equals(user.getIsActive());
        String action = currentlyActive ? "Deactivate" : "Activate";
        confirm(action + " User",
                "Are you sure you want to " + action.toLowerCase() + " " + user.getUsername() + "?",
                () -> {
                    try {
                        userService.update(new UpdateUserDTO(user.getUserId(), user.getEmail(), !currentlyActive));
                        refreshTable();
                        toastSuccess("User " + (currentlyActive ? "deactivated." : "activated.") );
                    } catch (Exception e) {
                        toastError("Failed to update user status: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog in Add mode (user == null) or Update mode. */
    private void openUserDialog(UserDTO user) {
        boolean addMode = user == null;

        TextField username = new TextField();
        TextField email    = new TextField();
        PasswordField password = new PasswordField();
        LoadingIdComboBox roleField = new LoadingIdComboBox();
        EntityIdComboBox role = roleField.getComboBox();

        List.of(username, email, password).forEach(f -> f.getStyleClass().add("form-input"));
        role.getStyleClass().add("form-combo");

        List<Control> otherFields = List.of(username, email, password);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            username.setText(user.getUsername());
            username.setDisable(true); // renaming a username isn't supported by the backend yet
            email.setText(user.getEmail());
        }

        formDialogController.open(addMode ? "Add User" : "Update User", "fas-user", addMode, v -> {
            String un = username.getText() == null ? "" : username.getText().trim();
            String em = email.getText() == null ? "" : email.getText().trim();
            if ((addMode && un.isEmpty()) || em.isEmpty()) {
                formDialogController.setError("Username and email are required.");
                formDialogController.setLoading(false);
                return;
            }
            if (addMode && password.getText().length() < 8) {
                formDialogController.setError("Password must be at least 8 characters.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                String selectedRoleId = role.getSelectedId();
                UserDTO saved;

                if (addMode) {
                    saved = userService.create(new CreateUserDTO(null, un, password.getText(), em));
                } else {
                    saved = userService.update(new UpdateUserDTO(user.getUserId(), em, user.getIsActive()));
                }

                if (selectedRoleId != null) {
                    List<RoleDTO> currentRoles = roleService.findRolesForUser(saved.getUserId());
                    boolean alreadyAssigned = currentRoles.stream().anyMatch(r -> r.getRoleId().equals(selectedRoleId));
                    if (!alreadyAssigned) {
                        for (RoleDTO oldRole : currentRoles) {
                            roleService.revokeFromUser(saved.getUserId(), oldRole.getRoleId());
                        }
                        roleService.assignToUser(saved.getUserId(), selectedRoleId);
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
        formDialogController.addField("Role", "fas-user-tag", roleField);

        loadRoleDropdown(roleField, otherFields, addMode ? null : user.getUserId());
    }

    /** Loads the role dropdown fresh from the DB every time the dialog opens — not from the
     *  page-level allRoles cache — so a custom role just created on the Roles page is
     *  immediately assignable here without needing to reload the Users page. */
    private void loadRoleDropdown(LoadingIdComboBox roleField, List<Control> otherFields, String userIdForCurrentRole) {
        EntityIdComboBox role = roleField.getComboBox();

        roleField.setLoading(true);
        formDialogController.setLoading(true);

        AsyncJobRunner.submit(
            roleService::findAll,
            roles -> {
                role.setOptions(roles.stream()
                        .map(r -> new EntityIdComboBox.Option(r.getRoleId(), r.getRoleName())).toList());
                if (userIdForCurrentRole != null) {
                    String currentRoleName = roleNameByUserId.get(userIdForCurrentRole);
                    roles.stream()
                            .filter(r -> r.getRoleName().equals(currentRoleName))
                            .findFirst()
                            .ifPresent(r -> role.selectById(r.getRoleId()));
                }
                roleField.setLoading(false);
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            },
            ex -> {
                roleField.setLoading(false);
                toastError("Failed to load roles: " + ex.getMessage());
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            });
    }
}
