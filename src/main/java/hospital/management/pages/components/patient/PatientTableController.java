package hospital.management.pages.components.patient;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.patient.Patient;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.Period;

public class PatientTableController extends PaginatedTableController<Patient> {

    @FXML private TableColumn<Patient, String>  idColumn;
    @FXML private TableColumn<Patient, String>  nameColumn;
    @FXML private TableColumn<Patient, Integer> ageColumn;
    @FXML private TableColumn<Patient, String>  genderColumn;
    @FXML private TableColumn<Patient, String>  phoneColumn;
    @FXML private TableColumn<Patient, String>  statusColumn;
    @FXML private TableColumn<Patient, Void>    actionsColumn;

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFullName()));
        ageColumn.setCellValueFactory(cell -> {
            LocalDate dob = cell.getValue().getDob();
            int age = (dob != null) ? Period.between(dob, LocalDate.now()).getYears() : 0;
            return new SimpleIntegerProperty(age).asObject();
        });
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        // status column left unbound — admission status comes from a separate domain
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(Patient patient, String lowerQuery) {
        return patient.getFullName().toLowerCase().contains(lowerQuery)
                || patient.getPatientId().toLowerCase().contains(lowerQuery);
    }
}
