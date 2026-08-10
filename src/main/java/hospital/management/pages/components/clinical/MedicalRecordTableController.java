package hospital.management.pages.components.clinical;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.clinical.MedicalRecordDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;

public class MedicalRecordTableController extends PaginatedTableController<MedicalRecordDTO> {

    private static final DateTimeFormatter RECORDED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<MedicalRecordDTO, String> recordIdColumn;
    @FXML private TableColumn<MedicalRecordDTO, String> diagnosisColumn;
    @FXML private TableColumn<MedicalRecordDTO, String> symptomsColumn;
    @FXML private TableColumn<MedicalRecordDTO, String> notesColumn;
    @FXML private TableColumn<MedicalRecordDTO, String> recordDateColumn;
    @FXML private TableColumn<MedicalRecordDTO, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        recordIdColumn.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        diagnosisColumn.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        symptomsColumn.setCellValueFactory(new PropertyValueFactory<>("symptoms"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        recordDateColumn.setCellValueFactory(cell -> {
            var createdAt = cell.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt == null ? "" : createdAt.format(RECORDED_AT_FORMAT));
        });
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(MedicalRecordDTO record, String lowerQuery) {
        String diagnosis = record.getDiagnosis();
        String symptoms = record.getSymptoms();
        return (diagnosis != null && diagnosis.toLowerCase().contains(lowerQuery))
                || (symptoms != null && symptoms.toLowerCase().contains(lowerQuery));
    }
}
