package hospital.management.pages.auth;

import hospital.management.backend.dao.auth.PermissionDAOImpl;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.RolePermissionDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dto.auth.CreatePermissionDTO;
import hospital.management.backend.dto.auth.CreateRoleDTO;
import hospital.management.backend.dto.auth.PermissionDTO;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.auth.PermissionServiceImpl;
import hospital.management.backend.service.auth.RoleServiceImpl;
import hospital.management.backend.service.auth.interfaces.PermissionService;
import hospital.management.backend.service.auth.interfaces.RoleService;
import hospital.management.enums.PageRoute;
import hospital.management.pages.BasePageController;
import hospital.management.pages.components.auth.PermissionCardsController;
import hospital.management.pages.components.auth.RoleTableController;
import hospital.management.pages.components.shared.sort.SortBarController;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RolesPageController extends BasePageController {

    private final RoleService roleService = new RoleServiceImpl(
        new RoleDAOImpl(), new UserRoleDAOImpl(), new RolePermissionDAOImpl(), new PermissionDAOImpl());
    private final PermissionService permissionService = new PermissionServiceImpl(new PermissionDAOImpl());

    // Roles tab
    @FXML private RoleTableController roleTableController;
    @FXML private SortBarController roleSortBarController;
    @FXML private TextField roleSearchField;
    @FXML private Button    addRoleBtn;
    @FXML private Button    continueBtn;

    // Permissions tab
    @FXML private PermissionCardsController permissionTableController;
    @FXML private TextField permissionSearchField;

    private List<RoleDTO> allRoles = new ArrayList<>();
    private List<PermissionDTO> allPermissions = new ArrayList<>();
    private final Map<String, String> permissionCountByRoleId = new HashMap<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.ROLES);

        roleTableController.setPermissionCountResolver(
            r -> permissionCountByRoleId.getOrDefault(r.getRoleId(), "0"));
        roleTableController.setRowActions(
                allowUpdate(PageRoute.ROLES, this::openRoleDialog),
                allowDelete(PageRoute.ROLES, this::confirmDeleteRole),
                allowRead(PageRoute.ROLES, this::viewRoleDetail));
        roleSearchField.textProperty().addListener((obs, o, n) -> roleTableController.filter(n));
        applyCreateVisibility(addRoleBtn, PageRoute.ROLES);
        addRoleBtn.setOnAction(e -> openRoleDialog(null));
        setupContinueButton(continueBtn, PageRoute.ROLES);
        if (roleSortBarController != null) {
            roleSortBarController.setOnSort((field, asc) -> roleTableController.applySort(field, asc));
            roleSortBarController.addOptions(roleTableController.getSortOptionLabels());
        }

        permissionTableController.setOnDelete(canDelete(PageRoute.ROLES) ? this::confirmDeletePermission : null);
        permissionSearchField.textProperty().addListener((obs, o, n) -> permissionTableController.filter(n));

        refreshPermissions();
        refreshRoles();
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

    /** Opens the shared form dialog in Add mode (role == null) or Update mode, with a
     *  permission checklist (grouped by resource) alongside the role name field. */
    private void openRoleDialog(RoleDTO role) {
        boolean addMode = role == null;

        TextField name = new TextField();
        name.getStyleClass().add("form-input");
        if (!addMode) {
            name.setText(role.getRoleName());
            name.setDisable(true); // RoleService has no rename endpoint yet
        }

        Set<String> assignedIds = new HashSet<>();
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
                .collect(Collectors.groupingBy(PermissionDTO::getResource, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<PermissionDTO>> entry : byResource.entrySet()) {
            List<PermissionDTO> permissions = entry.getValue();

            Label resourceLabel = new Label(entry.getKey());
            resourceLabel.getStyleClass().add("permission-resource-label");

            CheckBox selectAll = new CheckBox("All");
            selectAll.getStyleClass().add("permission-select-all");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(8, resourceLabel, spacer, selectAll);
            header.setAlignment(Pos.CENTER_LEFT);

            FlowPane actionsRow = new FlowPane(12, 6);
            List<CheckBox> permissionCheckboxes = new ArrayList<>();
            Map<String, CheckBox> actionCheckbox = new LinkedHashMap<>();
            for (PermissionDTO p : permissions) {
                CheckBox cb = new CheckBox(p.getAction());
                cb.setSelected(assignedIds.contains(p.getPermissionId()));
                checkboxByPermissionId.put(p.getPermissionId(), cb);
                permissionCheckboxes.add(cb);
                actionCheckbox.put(p.getAction(), cb);
                actionsRow.getChildren().add(cb);
            }

            permissionCheckboxes.forEach(cb ->
                cb.selectedProperty().addListener((obs, was, is) -> syncSelectAllState(selectAll, permissionCheckboxes)));
            syncSelectAllState(selectAll, permissionCheckboxes);
            selectAll.setOnAction(e ->
                permissionCheckboxes.forEach(cb -> cb.setSelected(selectAll.isSelected())));

            // Creating an entity implies being able to read it — checking "create"
            // auto-checks "read" for the same resource (unless it was already on).
            CheckBox readCb = actionCheckbox.get("read");
            CheckBox createCb = actionCheckbox.get("create");
            if (readCb != null && createCb != null) {
                createCb.selectedProperty().addListener((obs, was, is) -> {
                    if (is && !readCb.isSelected()) readCb.setSelected(true);
                });
            }

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
                RoleDTO saved = addMode ? roleService.create(new CreateRoleDTO(roleName)) : role;
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

    /** Reflects a resource category's checkbox states onto its "All" header checkbox: fully checked,
     *  fully unchecked, or indeterminate when the category is only partially assigned. */
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
                permissionService.create(new CreatePermissionDTO(res, act));
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
