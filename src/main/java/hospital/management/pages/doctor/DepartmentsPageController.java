package hospital.management.pages.doctor;

import hospital.management.backend.utils.FxFormValidator;
import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dto.doctor.CreateDepartmentDTO;
import hospital.management.backend.dto.doctor.DepartmentDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.department.DepartmentServiceImpl;
import hospital.management.backend.service.department.interfaces.DepartmentService;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.doctor.DepartmentTableController;
import hospital.management.pages.components.shared.sort.SortBarController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DepartmentsPageController extends BasePageController {

    private final DepartmentService departmentService = new DepartmentServiceImpl(new DepartmentDAOImpl());

    @FXML private DepartmentTableController departmentTableController;
    @FXML private SortBarController sortBarController;

    @FXML private Label     totalLabel;
    @FXML private TextField searchField;
    @FXML private Button    addDeptBtn;
    @FXML private Button    continueBtn;

    private List<DepartmentDTO> departments = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.DEPARTMENTS);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        applyCreateVisibility(addDeptBtn, PageRoute.DEPARTMENTS);
        addDeptBtn.setOnAction(e -> openDepartmentDialog(null));
        setupContinueButton(continueBtn, PageRoute.DEPARTMENTS);
        departmentTableController.setRowActions(
            allowUpdate(PageRoute.DEPARTMENTS, this::openDepartmentDialog),
            allowDelete(PageRoute.DEPARTMENTS, this::confirmDeleteDepartment),
            allowRead(PageRoute.DEPARTMENTS, this::viewDepartmentDetail));

        if (sortBarController != null) {
            sortBarController.setOnSort((field, asc) -> departmentTableController.applySort(field, asc));
            sortBarController.addOptions(departmentTableController.getSortOptionLabels());
        }

        refreshTable();
    }

    private void applyFilter() {
        departmentTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            departments = departmentService.findAll();
            departmentTableController.setItems(departments);
            totalLabel.setText("Total: " + departments.size() + " departments");
        } catch (Exception e) {
            toastError("Failed to load departments: " + e.getMessage());
        }
    }

    private void viewDepartmentDetail(DepartmentDTO department) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Name", department.getName());
        fields.put("Location", department.getLocation());
        fields.put("Phone", department.getPhone());
        detailViewController.show("Department Details", "fas-hospital", fields);
    }

    private void confirmDeleteDepartment(DepartmentDTO department) {
        confirm("Delete Department",
                "Are you sure you want to delete " + department.getName() + "? This cannot be undone.",
                () -> {
                    try {
                        departmentService.delete(department.getDepartmentId());
                        refreshTable();
                        toastSuccess("Department deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete department: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog in Add mode (department == null) or Update mode. */
    private void openDepartmentDialog(DepartmentDTO department) {
        boolean addMode = department == null;

        TextField name     = new TextField();
        TextField location = new TextField();
        TextField phone    = new TextField();

        name.setPromptText("e.g. Cardiology");
        location.setPromptText("e.g. Building B, Floor 2 (optional)");
        phone.setPromptText("e.g. +250 788 000 000 (optional)");

        List.of(name, location, phone).forEach(f -> f.getStyleClass().add("form-input"));

        FxFormValidator.attachRequired(name,     null, "Department name");
        FxFormValidator.attachMaxLength(location, null, 255, "Location");
        FxFormValidator.attachPhone(phone,        null);

        if (!addMode) {
            name.setText(department.getName());
            location.setText(department.getLocation());
            phone.setText(department.getPhone());
            FxFormValidator.applyStyle(name, department.getName() != null && !department.getName().isBlank());
        }

        formDialogController.open(addMode ? "Add Department" : "Update Department", "fas-hospital", addMode, v -> {
            String nm  = name.getText() == null ? "" : name.getText().trim();
            String loc = location.getText() == null ? "" : location.getText().trim();
            if (nm.isEmpty()) {
                formDialogController.setError("Department name is required.");
                FxFormValidator.applyStyle(name, false);
                formDialogController.setLoading(false);
                return;
            }

            try {
                CreateDepartmentDTO dto = new CreateDepartmentDTO(nm, loc, phone.getText());
                if (addMode) {
                    departmentService.create(dto);
                } else {
                    departmentService.update(department.getDepartmentId(), dto);
                }
                refreshTable();
                formDialogController.close();
                toastSuccess(addMode ? "Department added." : "Department updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save department: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Name", "fas-hospital", name);
        formDialogController.addField("Location", "fas-map-marker-alt", location);
        formDialogController.addField("Phone", "fas-phone", phone);
    }
}
