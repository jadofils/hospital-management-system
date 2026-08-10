package hospital.management.pages.components.patient;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.patient.PatientDTO;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.Period;

public class PatientTableController extends PaginatedTableController<PatientDTO> {

    @FXML private TableColumn<PatientDTO, String>  idColumn;
    @FXML private TableColumn<PatientDTO, String>  nameColumn;
    @FXML private TableColumn<PatientDTO, Integer> ageColumn;
    @FXML private TableColumn<PatientDTO, String>  genderColumn;
    @FXML private TableColumn<PatientDTO, String>  phoneColumn;
    @FXML private TableColumn<PatientDTO, String>  statusColumn;
    @FXML private TableColumn<PatientDTO, Void>    actionsColumn;

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
    protected boolean matches(PatientDTO patient, String lowerQuery) {
        return patient.getFullName().toLowerCase().contains(lowerQuery)
            || patient.getPatientId().toLowerCase().contains(lowerQuery)
            || safe(patient.getPhone()).contains(lowerQuery)
            || safe(patient.getEmail()).contains(lowerQuery)
            || safe(patient.getGender()).contains(lowerQuery)
            || safe(patient.getAddress()).contains(lowerQuery);
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(); }
}
