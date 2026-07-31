package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.doctor.Doctor;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class DoctorTableController extends PaginatedTableController<Doctor> {

    @FXML private TableColumn<Doctor, String> idColumn;
    @FXML private TableColumn<Doctor, String> nameColumn;
    @FXML private TableColumn<Doctor, String> specializationColumn;
    @FXML private TableColumn<Doctor, String> departmentColumn;
    @FXML private TableColumn<Doctor, String> phoneColumn;
    @FXML private TableColumn<Doctor, String> emailColumn;
    @FXML private TableColumn<Doctor, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFullName()));
        specializationColumn.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("departmentId"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(Doctor doctor, String lowerQuery) {
        String specialization = doctor.getSpecialization();
        return doctor.getFullName().toLowerCase().contains(lowerQuery)
                || (specialization != null && specialization.toLowerCase().contains(lowerQuery));
    }
}
