package hospital.management.pages.auth;

import hospital.management.backend.dao.auth.PermissionDAOImpl;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.RolePermissionDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dto.auth.CreateUserDTO;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.dto.auth.PermissionDTO;
import hospital.management.backend.dto.auth.UpdateUserDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.auth.RoleServiceImpl;
import hospital.management.backend.service.auth.PermissionServiceImpl;
import hospital.management.backend.service.auth.interfaces.PermissionService;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.RoleService;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.BasePageController;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.auth.UserTableController;
import hospital.management.pages.components.auth.RoleTableController;
import hospital.management.pages.components.auth.PermissionCardsController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class UsersPageController extends BasePageController {

    /** Loads the whole user list in one page — an admin roster is small enough that
     *  client-side search/pagination (like every other table in this app) is simpler
     *  than wiring cursor-by-cursor server paging into the table widget. */
    private static final int FETCH_SIZE = 500;

    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    private final RoleService roleService = new RoleServiceImpl(
        new RoleDAOImpl(), new UserRoleDAOImpl(), new RolePermissionDAOImpl(), new PermissionDAOImpl());
    private final PermissionService permissionService = new PermissionServiceImpl(new PermissionDAOImpl());

    @FXML private UserTableController userTableController;

    // Roles & Permissions tab
    @FXML private RoleTableController roleTableController;
    @FXML private TextField roleSearchField;
    @FXML private Button    addRoleBtn;
    @FXML private PermissionCardsController permissionTableController;
    @FXML private TextField permissionSearchField;

    @FXML private Label    totalLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button   addUserBtn;

    private List<UserDTO> users = new ArrayList<>();
    private final Map<String, String> roleNameByUserId = new HashMap<>();
    private List<RoleDTO> allRoles = new ArrayList<>();
    private List<PermissionDTO> allPermissions = new ArrayList<>();
    private final Map<String, String> permissionCountByRoleId = new HashMap<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.USERS);

        statusFilter.getItems().addAll("All", "Active", "Inactive");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        roleFilter.setOnAction(e -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        userTableController.setRoleNameResolver(u -> roleNameByUserId.getOrDefault(u.getUserId(), "—"));
        applyCreateVisibility(addUserBtn, PageRoute.USERS);
        addUserBtn.setOnAction(e -> openUserDialog(null));
        userTableController.setRowActions(
            allowUpdate(PageRoute.USERS, this::openUserDialog),
            allowDelete(PageRoute.USERS, this::confirmDeleteUser),
            allowRead(PageRoute.USERS, this::viewUserDetail));
        userTableController.setOnChangeStatus(canUpdate(PageRoute.USERS) ? this::confirmToggleActive : null);

        loadRolesAndUsers();
        // Roles & permissions tab setup
        roleTableController.setPermissionCountResolver(
            r -> permissionCountByRoleId.getOrDefault(r.getRoleId(), "0"));
        roleTableController.setRowActions(
            allowUpdate(PageRoute.ROLES, this::openRoleDialog),
            allowDelete(PageRoute.ROLES, this::confirmDeleteRole),
            allowRead(PageRoute.ROLES, this::viewRoleDetail));
        roleSearchField.textProperty().addListener((obs, o, n) -> roleTableController.filter(n));
        applyCreateVisibility(addRoleBtn, PageRoute.ROLES);
        addRoleBtn.setOnAction(e -> openRoleDialog(null));

        permissionTableController.setOnDelete(this::confirmDeletePermission);
        permissionSearchField.textProperty().addListener((obs, o, n) -> permissionTableController.filter(n));

        refreshPermissions();
        refreshRoles();
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

    private void refreshRoles() {
        try {
            allRoles = roleService.findAll();
            permissionCountByRoleId.clear();
            for (RoleDTO role : allRoles) {
                int count = roleService.findPermissionsForRole(role.getRoleId()).size();
                permissionCountByRoleId.put(role.getRoleId(), String.valueOf(count));
            }
            roleTableController.setItems(allRoles);
        } catch (Exception e) {
            toastError("Failed to load roles: " + e.getMessage());
        }
    }

    private void refreshPermissions() {
        try {
            allPermissions = permissionService.findAll();
            permissionTableController.setItems(allPermissions);
        } catch (Exception e) {
            toastError("Failed to load permissions: " + e.getMessage());
        }
    }

    private void viewRoleDetail(RoleDTO role) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Role Name", role.getRoleName());
        fields.put("Permission Count", permissionCountByRoleId.getOrDefault(role.getRoleId(), "0"));
        try {
            String assigned = roleService.findPermissionsForRole(role.getRoleId()).stream()
                    .map(p -> p.getResource() + ":" + p.getAction())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("None");
            fields.put("Permissions", assigned);
        } catch (Exception ex) {
            toastError("Failed to load permissions: " + ex.getMessage());
        }
        fields.put("Created At", role.getCreatedAt() == null ? null : role.getCreatedAt().toString());
        detailViewController.show("Role Details", "fas-user-shield", fields);
    }

    private void confirmDeleteRole(RoleDTO role) {
        confirm("Delete Role",
                "Are you sure you want to delete \"" + role.getRoleName() + "\"? "
                        + "Any user currently assigned this role will lose it. This cannot be undone.",
                () -> {
                    try {
                        roleService.delete(role.getRoleId());
                        refreshRoles();
                        toastSuccess("Role deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete role: " + e.getMessage());
                    }
                });
    }

    private void confirmDeletePermission(PermissionDTO permission) {
        confirm("Delete Permission",
                "Are you sure you want to delete \"" + permission.getAction() + "\" on \""
                        + permission.getResource() + "\"? Any role granting it will lose it. This cannot be undone.",
                () -> {
                    try {
                        permissionService.delete(permission.getPermissionId());
                        refreshPermissions();
                        refreshRoles();
                        toastSuccess("Permission deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete permission: " + e.getMessage());
                    }
                });
    }

    private void openRoleDialog(RoleDTO role) {
        boolean addMode = role == null;

        TextField name = new TextField();
        name.getStyleClass().add("form-input");
        if (!addMode) {
            name.setText(role.getRoleName());
            name.setDisable(true); // RoleService has no rename endpoint yet
        }

        Set<String> assignedIds = new java.util.HashSet<>();
        if (!addMode) {
            try {
                for (PermissionDTO p : roleService.findPermissionsForRole(role.getRoleId())) {
                    assignedIds.add(p.getPermissionId());
                }
            } catch (Exception e) {
                toastError("Failed to load current permissions: " + e.getMessage());
            }
        }

        Map<String, CheckBox> checkboxByPermissionId = new LinkedHashMap<>();
        VBox permissionList = new VBox(10);
        Map<String, List<PermissionDTO>> byResource = allPermissions.stream()
                .collect(java.util.stream.Collectors.groupingBy(PermissionDTO::getResource, LinkedHashMap::new, java.util.stream.Collectors.toList()));
        for (Map.Entry<String, List<PermissionDTO>> entry : byResource.entrySet()) {
            List<PermissionDTO> permissions = entry.getValue();

            Label resourceLabel = new Label(entry.getKey());
            resourceLabel.getStyleClass().add("permission-resource-label");

            CheckBox selectAll = new CheckBox("All");
            selectAll.getStyleClass().add("permission-select-all");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(8, resourceLabel, spacer, selectAll);
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            FlowPane actionsRow = new FlowPane(12, 6);
            List<CheckBox> permissionCheckboxes = new ArrayList<>();
            for (PermissionDTO p : permissions) {
                CheckBox cb = new CheckBox(p.getAction());
                cb.setSelected(assignedIds.contains(p.getPermissionId()));
                checkboxByPermissionId.put(p.getPermissionId(), cb);
                permissionCheckboxes.add(cb);
                actionsRow.getChildren().add(cb);
            }

            permissionCheckboxes.forEach(cb ->
                cb.selectedProperty().addListener((obs, was, is) -> syncSelectAllState(selectAll, permissionCheckboxes)));
            syncSelectAllState(selectAll, permissionCheckboxes);
            selectAll.setOnAction(e ->
                permissionCheckboxes.forEach(cb -> cb.setSelected(selectAll.isSelected())));

            VBox resourceCard = new VBox(6, header, actionsRow);
            resourceCard.getStyleClass().add("permission-resource-card");
            permissionList.getChildren().add(resourceCard);
        }

        formDialogController.open(addMode ? "Add Role" : "Update Role", "fas-user-shield", addMode, v -> {
            String roleName = name.getText() == null ? "" : name.getText().trim();
            if (addMode && roleName.isEmpty()) {
                formDialogController.setError("Role name is required.");
                formDialogController.setLoading(false);
                return;
            }
            try {
                RoleDTO saved = addMode ? roleService.create(new hospital.management.backend.dto.auth.CreateRoleDTO(roleName)) : role;
                for (Map.Entry<String, CheckBox> entry : checkboxByPermissionId.entrySet()) {
                    boolean checked = entry.getValue().isSelected();
                    boolean wasAssigned = assignedIds.contains(entry.getKey());
                    if (checked && !wasAssigned) {
                        roleService.assignPermission(saved.getRoleId(), entry.getKey());
                    } else if (!checked && wasAssigned) {
                        roleService.revokePermission(saved.getRoleId(), entry.getKey());
                    }
                }
                refreshRoles();
                formDialogController.close();
                toastSuccess(addMode ? "Role added." : "Role updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save role: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Role Name", "fas-tag", name);
        Label permLabel = new Label("Permissions");
        permLabel.getStyleClass().add("field-label");
        formDialogController.addRow(permLabel);
        formDialogController.addRow(permissionList);
    }

    private void syncSelectAllState(CheckBox selectAll, List<CheckBox> permissionCheckboxes) {
        long checkedCount = permissionCheckboxes.stream().filter(CheckBox::isSelected).count();
        if (checkedCount == 0) {
            selectAll.setIndeterminate(false);
            selectAll.setSelected(false);
        } else if (checkedCount == permissionCheckboxes.size()) {
            selectAll.setIndeterminate(false);
            selectAll.setSelected(true);
        } else {
            selectAll.setIndeterminate(true);
        }
    }

    private void openPermissionDialog() {
        TextField resource = new TextField();
        TextField action   = new TextField();
        resource.getStyleClass().add("form-input");
        action.getStyleClass().add("form-input");

        formDialogController.open("Add Permission", "fas-key", true, v -> {
            String res = resource.getText() == null ? "" : resource.getText().trim();
            String act = action.getText() == null ? "" : action.getText().trim();
            if (res.isEmpty() || act.isEmpty()) {
                formDialogController.setError("Resource and action are required.");
                formDialogController.setLoading(false);
                return;
            }
            try {
                permissionService.create(new hospital.management.backend.dto.auth.CreatePermissionDTO(res, act));
                refreshPermissions();
                formDialogController.close();
                toastSuccess("Permission added.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to add permission: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Resource", "fas-cube", resource);
        formDialogController.addField("Action", "fas-bolt", action);
    }
}
