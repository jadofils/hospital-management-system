package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.doctor.Department;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class DepartmentTableController extends PaginatedTableController<Department> {

    @FXML private TableColumn<Department, String> deptIdColumn;
    @FXML private TableColumn<Department, String> nameColumn;
    @FXML private TableColumn<Department, String> locationColumn;
    @FXML private TableColumn<Department, String> phoneColumn;
    @FXML private TableColumn<Department, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        deptIdColumn.setCellValueFactory(new PropertyValueFactory<>("departmentId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(Department department, String lowerQuery) {
        String name = department.getName();
        String location = department.getLocation();
        return (name != null && name.toLowerCase().contains(lowerQuery))
                || (location != null && location.toLowerCase().contains(lowerQuery));
    }
}
