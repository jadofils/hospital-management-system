package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.doctor.DepartmentDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class DepartmentTableController extends PaginatedTableController<DepartmentDTO> {

    @FXML private TableColumn<DepartmentDTO, String> deptIdColumn;
    @FXML private TableColumn<DepartmentDTO, String> nameColumn;
    @FXML private TableColumn<DepartmentDTO, String> locationColumn;
    @FXML private TableColumn<DepartmentDTO, String> phoneColumn;
    @FXML private TableColumn<DepartmentDTO, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        deptIdColumn.setCellValueFactory(new PropertyValueFactory<>("departmentId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(DepartmentDTO department, String lowerQuery) {
        String name = department.getName();
        String location = department.getLocation();
        return (name != null && name.toLowerCase().contains(lowerQuery))
                || (location != null && location.toLowerCase().contains(lowerQuery));
    }
}
