package hospital.management.pages.doctor;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.auth.PermissionDAOImpl;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.RolePermissionDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dto.auth.CreateUserDTO;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dto.doctor.CreateDoctorDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.auth.RoleServiceImpl;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.RoleService;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.department.interfaces.DoctorService;
import hospital.management.backend.service.department.DepartmentServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.doctor.DoctorTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import hospital.management.pages.utils.CsvUiIO;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class DoctorsPageController extends BasePageController {

    private static final int FETCH_SIZE = 500;

    private final DepartmentServiceImpl departmentService = new DepartmentServiceImpl(new DepartmentDAOImpl());
    private final DoctorService doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
        private final UserService userService = new UserServiceImpl(new UserDAOImpl());
        private final RoleService roleService = new RoleServiceImpl(
            new RoleDAOImpl(), new UserRoleDAOImpl(), new RolePermissionDAOImpl(), new PermissionDAOImpl());
        private final UserDAOImpl userDAO = new UserDAOImpl();
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private DoctorTableController doctorTableController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private Button addDoctorBtn;
    @FXML private Button importBtn;
    @FXML private Button exportBtn;
    @FXML private Label totalLabel;

    private List<DoctorDTO> doctors = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.DOCTORS);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        departmentFilter.setOnAction(e -> applyFilter());

        applyCreateVisibility(addDoctorBtn, PageRoute.DOCTORS);
        applyCreateVisibility(importBtn, PageRoute.DOCTORS);
        boolean canExport = canRead(PageRoute.DOCTORS);
        exportBtn.setVisible(canExport);
        exportBtn.setManaged(canExport);

        addDoctorBtn.setOnAction(e -> openDoctorDialog(null));
        importBtn.setOnAction(e -> withSpinner(importBtn, this::importDoctors));
        exportBtn.setOnAction(e -> withSpinner(exportBtn, this::exportDoctors));
        doctorTableController.setRowActions(
            allowUpdate(PageRoute.DOCTORS, this::openDoctorDialog),
            private final Map<String, String> roleNameByDoctorId = new LinkedHashMap<>();
            allowDelete(PageRoute.DOCTORS, this::confirmDeleteDoctor),
            allowRead(PageRoute.DOCTORS, this::viewDoctorDetail));

        refreshTable();
    }

    private void applyFilter() {
        doctorTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            doctors = doctorService.findAll(CursorPagination.firstPage(FETCH_SIZE)).getItems();
            doctorTableController.setItems(doctors);
            totalLabel.setText("Total: " + doctors.size() + " doctors");
        } catch (Exception e) {
                doctorTableController.setDoctorRowActions(
        }
    }
                    allowRead(PageRoute.DOCTORS, this::viewDoctorDetail),
                    allowUpdate(PageRoute.ROLES, this::openRoleAssignmentDialog));
    private void exportDoctors() {
        try {
            if (doctors.isEmpty()) {
                toastError("No doctors available to export.");
                    loadRoleMapForDoctors(doctors);
                    doctorTableController.setRoleByDoctorId(roleNameByDoctorId);
                return;
            }
            List<DoctorDTO> source = chooseDoctorExportSource();
            if (source.isEmpty()) {
                return;
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (DoctorDTO doctor : source) {
                Map<String, Object> row = new LinkedHashMap<>();
                fields.put("Role", roleNameByDoctorId.getOrDefault(doctor.getDoctorId(), "—"));
                row.put("doctor_id", doctor.getDoctorId());
                row.put("department_id", doctor.getDepartmentId());
                row.put("first_name", doctor.getFirstName());
                row.put("last_name", doctor.getLastName());
                row.put("specialization", doctor.getSpecialization());
                row.put("phone", doctor.getPhone());
                row.put("email", doctor.getEmail());
                rows.add(row);
            }

            private void openRoleAssignmentDialog(DoctorDTO doctor) {
                if (doctor.getEmail() == null || doctor.getEmail().isBlank()) {
                    toastError("Doctor email is required before assigning a role.");
                    return;
                }

                LoadingIdComboBox roleField = new LoadingIdComboBox();
                EntityIdComboBox role = roleField.getComboBox();
                role.getStyleClass().add("form-combo");

                formDialogController.open("Assign/Reassign Role", "fas-user-tag", false, v -> {
                    String selectedRoleId = role.getSelectedId();
                    if (selectedRoleId == null) {
                        formDialogController.setError("Role is required.");
                        formDialogController.setLoading(false);
                        return;
                    }
                    try {
                        assignOrReassignRoleForDoctor(doctor, selectedRoleId);
                        refreshTable();
                        formDialogController.close();
                        toastSuccess("Role updated for " + doctor.getFullName() + ".");
                    } catch (AppException ex) {
                        formDialogController.setError(ex.getMessage());
                        formDialogController.setLoading(false);
                    } catch (Exception ex) {
                        formDialogController.setError("Failed to assign role: " + ex.getMessage());
                        formDialogController.setLoading(false);
                    }
                });

                formDialogController.addField("Role", "fas-user-tag", roleField);
                loadRoleDropdownOnly(roleField, doctor);
            }

            boolean saved = CsvUiIO.exportRows(exportBtn.getScene().getWindow(), "doctors.csv", rows);
            if (saved) {
                toastSuccess("Doctors exported successfully.");
            }
        } catch (Exception e) {
            toastError("Failed to export doctors: " + e.getMessage());
        }
    }

    private List<DoctorDTO> chooseDoctorExportSource() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("All loaded rows", "All loaded rows", "Current table view");
        dialog.setTitle("Export Doctors");
        dialog.setHeaderText("Choose what to export");
        dialog.setContentText("Export scope:");
        String choice = dialog.showAndWait().orElse(null);
        if (choice == null) {
            return List.of();
        }
        if ("Current table view".equals(choice)) {
            return new ArrayList<>(doctorTableController.getTable().getItems());
        }
        return doctors;
    }

    private void importDoctors() {
        try {
            List<Map<String, String>> rows = CsvUiIO.importRows(importBtn.getScene().getWindow(), "Import Doctors");
            if (rows.isEmpty()) {
                return;
            }

            int ok = 0;
            int failed = 0;
            for (Map<String, String> row : rows) {
                try {
                    CreateDoctorDTO dto = new CreateDoctorDTO(
                            value(row, "department_id"),
                            value(row, "first_name", "firstname"),
                            value(row, "last_name", "lastname"),
                            value(row, "specialization"),
                            value(row, "phone"),
                            value(row, "email"));
                    doctorService.create(dto);
                    ok++;
                } catch (Exception ex) {
                    failed++;
                }
            }

            refreshTable();
            if (failed == 0) {
                toastSuccess("Imported " + ok + " doctor(s).");
            } else {
                toastError("Imported " + ok + " doctor(s), failed " + failed + ".");
            }
        } catch (Exception e) {
            toastError("Failed to import doctors: " + e.getMessage());
        }
    }

    private String value(Map<String, String> row, String... keys) {

            private void loadRoleDropdownOnly(LoadingIdComboBox roleField, DoctorDTO doctor) {
                EntityIdComboBox role = roleField.getComboBox();
                roleField.setLoading(true);
                formDialogController.setLoading(true);

                AsyncJobRunner.submit(
                    roleService::findAll,
                    items -> {
                        role.setOptions(items.stream()
                                .map(r -> new EntityIdComboBox.Option(r.getRoleId(), r.getRoleName()))
                                .toList());
                        if (doctor != null && doctor.getEmail() != null && !doctor.getEmail().isBlank()) {
                            try {
                                Optional<UserDTO> user = findUserByEmail(doctor.getEmail());
                                if (user.isPresent()) {
                                    List<RoleDTO> roles = roleService.findRolesForUser(user.get().getUserId());
                                    if (!roles.isEmpty()) role.selectById(roles.get(0).getRoleId());
                                }
                            } catch (Exception ignored) {
                                // best-effort pre-selection only
                            }
                        }
                        roleField.setLoading(false);
                        formDialogController.setLoading(false);
                    },
                    ex -> {
                        roleField.setLoading(false);
                        toastError("Failed to load roles: " + ex.getMessage());
                        formDialogController.setLoading(false);
                    });
            }

            private void loadRoleMapForDoctors(List<DoctorDTO> items) {
                roleNameByDoctorId.clear();
                for (DoctorDTO doctor : items) {
                    String roleName = "—";
                    try {
                        if (doctor.getEmail() != null && !doctor.getEmail().isBlank()) {
                            Optional<UserDTO> user = findUserByEmail(doctor.getEmail());
                            if (user.isPresent()) {
                                List<RoleDTO> roles = roleService.findRolesForUser(user.get().getUserId());
                                if (!roles.isEmpty()) roleName = roles.get(0).getRoleName();
                            }
                        }
                    } catch (Exception ignored) {
                        roleName = "—";
                    }
                    roleNameByDoctorId.put(doctor.getDoctorId(), roleName);
                }
            }
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key).trim();
            }
        }
        return "";
    }

    private void viewDoctorDetail(DoctorDTO doctor) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Full Name", doctor.getFullName());
        fields.put("Specialization", doctor.getSpecialization());
        try {
            fields.put("Department", entityLookupService.departmentLabel(doctor.getDepartmentId()));
        } catch (Exception ex) {
            fields.put("Department", "Unknown");
        }
        fields.put("Phone", doctor.getPhone());
        fields.put("Email", doctor.getEmail());
        detailViewController.show("Doctor Details", "fas-user-md", fields);
    }

    private void confirmDeleteDoctor(DoctorDTO doctor) {
        confirm("Delete Doctor",
                "Are you sure you want to delete " + doctor.getFullName() + "? This cannot be undone.",
                () -> {
                    try {
                        doctorService.delete(doctor.getDoctorId());
                        refreshTable();
                        toastSuccess("Doctor deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete doctor: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog in Add mode (doctor == null) or Update mode. */
    private void openDoctorDialog(DoctorDTO doctor) {
        boolean addMode = doctor == null;

        TextField firstName = new TextField();
        TextField lastName = new TextField();
        TextField specialization = new TextField();
        LoadingIdComboBox departmentIdField = new LoadingIdComboBox();
        EntityIdComboBox departmentId = departmentIdField.getComboBox();
        LoadingIdComboBox roleField = new LoadingIdComboBox();
        EntityIdComboBox role = roleField.getComboBox();
        TextField phone = new TextField();
        TextField email = new TextField();

        List.of(firstName, lastName, specialization, phone, email)
                .forEach(f -> f.getStyleClass().add("form-input"));
        List.of(departmentId, role).forEach(f -> f.getStyleClass().add("form-combo"));

        List<Control> otherFields = List.of(firstName, lastName, specialization, phone, email);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            firstName.setText(doctor.getFirstName());
            lastName.setText(doctor.getLastName());
            specialization.setText(doctor.getSpecialization());
            phone.setText(doctor.getPhone());
            email.setText(doctor.getEmail());
        }

        formDialogController.open(addMode ? "Add Doctor" : "Update Doctor", "fas-user-md", addMode, v -> {
            String fn = firstName.getText() == null ? "" : firstName.getText().trim();
            String ln = lastName.getText() == null ? "" : lastName.getText().trim();
            if (fn.isEmpty() || ln.isEmpty()) {
                formDialogController.setError("First name and last name are required.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                CreateDoctorDTO dto = new CreateDoctorDTO(
                        departmentId.getSelectedId(), fn, ln,
                        specialization.getText(), phone.getText(), email.getText());
                DoctorDTO savedDoctor;
                if (addMode) {
                    savedDoctor = doctorService.create(dto);
                } else {
                    savedDoctor = doctorService.update(doctor.getDoctorId(), dto);
                }

                assignOrReassignRoleForDoctor(savedDoctor, role.getSelectedId());

                refreshTable();
                formDialogController.close();
                toastSuccess(addMode ? "Doctor added." : "Doctor updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save doctor: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("First Name", "fas-user", firstName);
        formDialogController.addField("Last Name", "fas-user", lastName);
        formDialogController.addField("Specialization", "fas-stethoscope", specialization);
        formDialogController.addField("Department", "fas-hospital", departmentIdField);
        formDialogController.addField("Login Role", "fas-user-tag", roleField);
        formDialogController.addField("Phone", "fas-phone", phone);
        formDialogController.addField("Email", "fas-envelope", email);

        loadDialogDropdowns(departmentIdField, roleField, otherFields, addMode ? null : doctor);
    }

    /** Loads department + role dropdowns asynchronously and enables the rest of the form once ready. */
    private void loadDialogDropdowns(LoadingIdComboBox departmentIdField, LoadingIdComboBox roleField,
                                     List<Control> otherFields, DoctorDTO existing) {
        EntityIdComboBox departmentId = departmentIdField.getComboBox();
        EntityIdComboBox role = roleField.getComboBox();

        departmentIdField.setLoading(true);
        roleField.setLoading(true);
        formDialogController.setLoading(true);

        AtomicInteger pending = new AtomicInteger(2);
        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

        AsyncJobRunner.submit(
            departmentService::findAll,
            items -> {
                departmentId.setOptions(items.stream()
                        .map(d -> new EntityIdComboBox.Option(d.getDepartmentId(), d.getName()))
                        .toList());
                if (existing != null) departmentId.selectById(existing.getDepartmentId());
                departmentIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                departmentIdField.setLoading(false);
                toastError("Failed to load departments: " + ex.getMessage());
                onOneLoaded.run();
            });

        AsyncJobRunner.submit(
            roleService::findAll,
            items -> {
                role.setOptions(items.stream()
                        .map(r -> new EntityIdComboBox.Option(r.getRoleId(), r.getRoleName()))
                        .toList());
                if (existing != null && existing.getEmail() != null && !existing.getEmail().isBlank()) {
                    try {
                        Optional<UserDTO> user = findUserByEmail(existing.getEmail());
                        if (user.isPresent()) {
                            List<RoleDTO> roles = roleService.findRolesForUser(user.get().getUserId());
                            if (!roles.isEmpty()) {
                                role.selectById(roles.get(0).getRoleId());
                            }
                        }
                    } catch (Exception ignored) {
                        // best-effort pre-selection only
                    }
                }
                roleField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                roleField.setLoading(false);
                toastError("Failed to load roles: " + ex.getMessage());
                onOneLoaded.run();
            });
    }

    private Optional<UserDTO> findUserByEmail(String email) throws Exception {
        return userDAO.findByEmail(email).map(hospital.management.backend.mapper.auth.UserMapper::toDTO);
    }

    private void assignOrReassignRoleForDoctor(DoctorDTO doctor, String selectedRoleId) throws Exception {
        if (selectedRoleId == null || selectedRoleId.isBlank()) return;
        String email = doctor.getEmail() == null ? "" : doctor.getEmail().trim();
        if (email.isBlank()) {
            throw new AppException("Doctor email is required to assign a login role.");
        }

        Optional<UserDTO> existingUser = findUserByEmail(email);
        UserDTO user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            String username = email;
            user = userService.create(new CreateUserDTO(
                    doctor.getDoctorId(), username, "Password@12", email));
        }

        List<RoleDTO> currentRoles = roleService.findRolesForUser(user.getUserId());
        boolean alreadyAssigned = currentRoles.stream().anyMatch(r -> r.getRoleId().equals(selectedRoleId));
        if (!alreadyAssigned) {
            for (RoleDTO oldRole : currentRoles) {
                roleService.revokeFromUser(user.getUserId(), oldRole.getRoleId());
            }
            roleService.assignToUser(user.getUserId(), selectedRoleId);
        }
    }
}
