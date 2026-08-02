package hospital.management.pages.clinical;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.patient.MedicalRecord;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.clinical.MedicalRecordTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MedicalRecordsController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

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
        medicalRecordTableController.setRowActions(this::openRecordDialog, this::confirmDeleteRecord, this::viewRecordDetail);

        refreshTable();
    }

    private void applyFilter() {
        medicalRecordTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        medicalRecordTableController.setItems(records);
    }

    private void viewRecordDetail(MedicalRecord record) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Appointment", entityLookupService.appointmentLabel(record.getAppointmentId()));
        } catch (Exception ex) {
            toastError("Failed to resolve appointment: " + ex.getMessage());
        }
        fields.put("Diagnosis", record.getDiagnosis());
        fields.put("Symptoms", record.getSymptoms());
        fields.put("Notes", record.getNotes());
        fields.put("Created At", record.getCreatedAt() == null ? null : record.getCreatedAt().toString());
        detailViewController.show("Medical Record Details", "fas-notes-medical", fields);
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

        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        TextField diagnosis     = new TextField();
        TextField symptoms      = new TextField();
        TextArea  notes         = new TextArea();
        notes.setPrefRowCount(3);

        List.of(diagnosis, symptoms).forEach(f -> f.getStyleClass().add("form-input"));
        appointmentId.getStyleClass().add("form-combo");
        notes.getStyleClass().add("form-input");

        List<Control> otherFields = List.of(diagnosis, symptoms, notes);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            diagnosis.setText(record.getDiagnosis());
            symptoms.setText(record.getSymptoms());
            notes.setText(record.getNotes());
        }

        formDialogController.open(addMode ? "Add Record" : "Update Record", "fas-notes-medical", addMode, v -> {
            String appt = appointmentId.getSelectedId();
            String diag = diagnosis.getText() == null ? "" : diagnosis.getText().trim();
            if (appt == null || diag.isEmpty()) {
                formDialogController.setError("Appointment and diagnosis are required.");
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

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Diagnosis", "fas-stethoscope", diagnosis);
        formDialogController.addField("Symptoms", "fas-head-side-cough", symptoms);
        formDialogController.addField("Notes", "fas-sticky-note", notes);

        loadRecordDropdown(appointmentIdField, otherFields, addMode ? null : record);
    }

    /** Loads the appointment dropdown options asynchronously, showing its own spinner while
     *  data is in flight and keeping the rest of the form disabled until it finishes loading. */
    private void loadRecordDropdown(LoadingIdComboBox appointmentIdField, List<Control> otherFields, MedicalRecord existing) {
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();

        appointmentIdField.setLoading(true);
        formDialogController.setLoading(true);

        AsyncJobRunner.submit(
            () -> appointmentService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                appointmentId.setOptions(items.stream()
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
                if (existing != null) appointmentId.selectById(existing.getAppointmentId());
                appointmentIdField.setLoading(false);
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            },
            ex -> {
                appointmentIdField.setLoading(false);
                toastError("Failed to load appointments: " + ex.getMessage());
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            });
    }
}
