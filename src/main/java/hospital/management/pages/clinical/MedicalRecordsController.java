package hospital.management.pages.clinical;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.patient.MedicalRecord;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.clinical.MedicalRecordTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MedicalRecordsController extends BasePageController {

    @FXML private MedicalRecordTableController medicalRecordTableController;

    @FXML private TextField  searchField;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button     addRecordBtn;

    private final List<MedicalRecord> records = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.MEDICAL_RECORDS);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        addRecordBtn.setOnAction(e -> openRecordDialog(null));
        medicalRecordTableController.setRowActions(this::openRecordDialog, this::confirmDeleteRecord);

        refreshTable();
    }

    private void applyFilter() {
        medicalRecordTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        medicalRecordTableController.setItems(records);
    }

    private void confirmDeleteRecord(MedicalRecord record) {
        confirm("Delete Medical Record",
                "Are you sure you want to delete record " + record.getRecordId() + "? This cannot be undone.",
                () -> {
                    records.remove(record);
                    refreshTable();
                    toastSuccess("Medical record deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (record == null) or Update mode. */
    private void openRecordDialog(MedicalRecord record) {
        boolean addMode = record == null;

        TextField appointmentId = new TextField();
        TextField diagnosis     = new TextField();
        TextField symptoms      = new TextField();
        TextArea  notes         = new TextArea();
        notes.setPrefRowCount(3);

        List.of(appointmentId, diagnosis, symptoms).forEach(f -> f.getStyleClass().add("form-input"));
        notes.getStyleClass().add("form-input");

        if (!addMode) {
            appointmentId.setText(record.getAppointmentId());
            diagnosis.setText(record.getDiagnosis());
            symptoms.setText(record.getSymptoms());
            notes.setText(record.getNotes());
        }

        formDialogController.open(addMode ? "Add Record" : "Update Record", "fas-notes-medical", addMode, v -> {
            String appt = appointmentId.getText() == null ? "" : appointmentId.getText().trim();
            String diag = diagnosis.getText() == null ? "" : diagnosis.getText().trim();
            if (appt.isEmpty() || diag.isEmpty()) {
                formDialogController.setError("Appointment ID and diagnosis are required.");
                formDialogController.setLoading(false);
                return;
            }

            MedicalRecord target = addMode ? new MedicalRecord() : record;
            if (addMode) {
                target.setRecordId(UUID.randomUUID().toString());
                target.setCreatedAt(LocalDateTime.now());
            } else {
                target.setUpdatedAt(LocalDateTime.now());
            }
            target.setAppointmentId(appt);
            target.setDiagnosis(diag);
            target.setSymptoms(symptoms.getText());
            target.setNotes(notes.getText());

            if (addMode) records.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Medical record added." : "Medical record updated.");
        });

        formDialogController.addField("Appointment Id", "fas-calendar-check", appointmentId);
        formDialogController.addField("Diagnosis", "fas-stethoscope", diagnosis);
        formDialogController.addField("Symptoms", "fas-head-side-cough", symptoms);
        formDialogController.addField("Notes", "fas-sticky-note", notes);
    }
}
