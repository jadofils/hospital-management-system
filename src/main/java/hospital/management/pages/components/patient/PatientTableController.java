package hospital.management.pages.components.patient;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.model.enums.PatientStatus;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.Period;
import java.util.function.Consumer;

public class PatientTableController extends PaginatedTableController<PatientDTO> {

    @FXML private TableColumn<PatientDTO, String>  idColumn;
    @FXML private TableColumn<PatientDTO, String>  nameColumn;
    @FXML private TableColumn<PatientDTO, Integer> ageColumn;
    @FXML private TableColumn<PatientDTO, String>  genderColumn;
    @FXML private TableColumn<PatientDTO, String>  phoneColumn;
    @FXML private TableColumn<PatientDTO, String>  statusColumn;
    @FXML private TableColumn<PatientDTO, Void>    changeStatusColumn;
    @FXML private TableColumn<PatientDTO, Void>    actionsColumn;

    private Consumer<PatientDTO> onChangeStatus;

    /** Registers the row-level activate/deactivate callback used by the changeStatusColumn button. */
    public void setOnChangeStatus(Consumer<PatientDTO> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    /** Hides the activate/deactivate column for read-only usages of this table (e.g. a doctor's
     *  own "My Patients" glance tab) that never call {@link #setOnChangeStatus} — mirrors
     *  {@link #hideActionsColumn()}'s rationale so the button doesn't render as a silent no-op. */
    public void hideChangeStatusColumn() {
        if (changeStatusColumn != null) {
            changeStatusColumn.setVisible(false);
        }
    }

    @Override
    protected void configureColumns() {
        idColumn.setVisible(false);
        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFullName()));
        ageColumn.setCellValueFactory(cell -> {
            LocalDate dob = cell.getValue().getDob();
            int age = (dob != null) ? Period.between(dob, LocalDate.now()).getYears() : 0;
            return new SimpleIntegerProperty(age).asObject();
        });
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(statusLabel(cell.getValue().getStatus())));
        wireSingleActionColumn(changeStatusColumn, "fas-power-off", "Activate or deactivate patient",
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        addSortOption("Name", nameColumn);
        addSortOption("Age", ageColumn);
        addSortOption("Gender", genderColumn);
        addSortOption("Phone", phoneColumn);
        addSortOption("Status", statusColumn);
        wireActionsColumn(actionsColumn);
    }

    private static String statusLabel(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) return "Active";
        try {
            return PatientStatus.fromDbValue(dbValue).getLabel();
        } catch (IllegalArgumentException e) {
            return dbValue;
        }
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
