package hospital.management.pages.doctor;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.doctor.Department;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.doctor.DepartmentTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DepartmentsPageController extends BasePageController {

    @FXML private DepartmentTableController departmentTableController;

    @FXML private Label     totalLabel;
    @FXML private TextField searchField;
    @FXML private Button    addDeptBtn;

    private final List<Department> departments = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.DEPARTMENTS);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        addDeptBtn.setOnAction(e -> openDepartmentDialog(null));
        departmentTableController.setRowActions(this::openDepartmentDialog, this::confirmDeleteDepartment);

        refreshTable();
    }

    private void applyFilter() {
        departmentTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        departmentTableController.setItems(departments);
        totalLabel.setText("Total: " + departments.size() + " departments");
    }

    private void confirmDeleteDepartment(Department department) {
        confirm("Delete Department",
                "Are you sure you want to delete " + department.getName() + "? This cannot be undone.",
                () -> {
                    departments.remove(department);
                    refreshTable();
                    toastSuccess("Department deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (department == null) or Update mode. */
    private void openDepartmentDialog(Department department) {
        boolean addMode = department == null;

        TextField name     = new TextField();
        TextField location = new TextField();
        TextField phone    = new TextField();

        List.of(name, location, phone).forEach(f -> f.getStyleClass().add("form-input"));

        if (!addMode) {
            name.setText(department.getName());
            location.setText(department.getLocation());
            phone.setText(department.getPhone());
        }

        formDialogController.open(addMode ? "Add Department" : "Update Department", "fas-hospital", addMode, v -> {
            String nm  = name.getText() == null ? "" : name.getText().trim();
            String loc = location.getText() == null ? "" : location.getText().trim();
            if (nm.isEmpty()) {
                formDialogController.setError("Department name is required.");
                formDialogController.setLoading(false);
                return;
            }

            Department target = addMode ? new Department() : department;
            if (addMode) target.setDepartmentId(UUID.randomUUID().toString());
            target.setName(nm);
            target.setLocation(loc);
            target.setPhone(phone.getText());

            if (addMode) departments.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Department added." : "Department updated.");
        });

        formDialogController.addField("Name", "fas-hospital", name);
        formDialogController.addField("Location", "fas-map-marker-alt", location);
        formDialogController.addField("Phone", "fas-phone", phone);
    }
}
