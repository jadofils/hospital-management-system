package hospital.management.pages.clinical;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.clinical.MedicalRecordDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.clinical.CreateMedicalRecordDTO;
import hospital.management.backend.dto.clinical.MedicalRecordDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.clinical.MedicalRecordServiceImpl;
import hospital.management.backend.service.clinical.interfaces.MedicalRecordService;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.clinical.MedicalRecordTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MedicalRecordsController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final MedicalRecordService medicalRecordService = new MedicalRecordServiceImpl(new MedicalRecordDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private MedicalRecordTableController medicalRecordTableController;

    @FXML private TextField  searchField;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button     addRecordBtn;

    private final List<MedicalRecordDTO> records = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.MEDICAL_RECORDS);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        applyCreateVisibility(addRecordBtn, PageRoute.MEDICAL_RECORDS);
        addRecordBtn.setOnAction(e -> openRecordDialog(null));
        medicalRecordTableController.setRowActions(
            allowUpdate(PageRoute.MEDICAL_RECORDS, this::openRecordDialog),
            allowDelete(PageRoute.MEDICAL_RECORDS, this::confirmDeleteRecord),
            allowRead(PageRoute.MEDICAL_RECORDS, this::viewRecordDetail));

        refreshTable();
    }

    private void applyFilter() {
        medicalRecordTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            records.clear();
            List<AppointmentSummaryDTO> appointments =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO appointment : appointments) {
                try {
                    records.add(medicalRecordService.findByAppointment(appointment.getAppointmentId()));
                } catch (ResourceNotFoundException ignored) {
                }
            }
            medicalRecordTableController.setItems(records);
        } catch (Exception e) {
            toastError("Failed to load medical records: " + e.getMessage());
        }
    }

    private void viewRecordDetail(MedicalRecordDTO record) {
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

    private void confirmDeleteRecord(MedicalRecordDTO record) {
        confirm("Delete Medical Record",
                "Are you sure you want to delete record " + record.getRecordId() + "? This cannot be undone.",
                () -> {
                    try {
                        medicalRecordService.delete(record.getRecordId());
                        refreshTable();
                        toastSuccess("Medical record deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete medical record: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog in Add mode (record == null) or Update mode. */
    private void openRecordDialog(MedicalRecordDTO record) {
        boolean addMode = record == null;

        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        TextField diagnosis     = new TextField();
        TextField symptoms      = new TextField();
        TextArea  notes         = new TextArea();
        notes.setPrefRowCount(3);

        // Placeholders
        diagnosis.setPromptText("e.g. Type 2 Diabetes, Hypertension");
        symptoms.setPromptText("e.g. Frequent urination, fatigue (optional)");
        notes.setPromptText("e.g. Patient advised to monitor blood sugar levels (optional)");

        List.of(diagnosis, symptoms).forEach(f -> f.getStyleClass().add("form-input"));
        appointmentId.getStyleClass().add("form-combo");
        notes.getStyleClass().add("form-input");

        // Real-time validators
        FxFormValidator.attachRequired(diagnosis, null, "Diagnosis");
        FxFormValidator.attachMaxLength(diagnosis, null, 500, "Diagnosis");
        FxFormValidator.attachRequired(notes, null, "Notes");

        List<Control> otherFields = List.of(diagnosis, symptoms, notes);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            diagnosis.setText(record.getDiagnosis());
            symptoms.setText(record.getSymptoms());
            notes.setText(record.getNotes());
            FxFormValidator.applyStyle(diagnosis, diagnosis.getText() != null && !diagnosis.getText().isBlank());
        }

        formDialogController.open(addMode ? "Add Record" : "Update Record", "fas-notes-medical", addMode, v -> {
            String appt = appointmentId.getSelectedId();
            String diag = diagnosis.getText() == null ? "" : diagnosis.getText().trim();
            if (appt == null) {
                formDialogController.setError("Appointment is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (diag.isEmpty()) {
                formDialogController.setError("Diagnosis is required.");
                FxFormValidator.applyStyle(diagnosis, false);
                formDialogController.setLoading(false);
                return;
            }

            try {
                CreateMedicalRecordDTO dto = new CreateMedicalRecordDTO(appt, diag, symptoms.getText(), notes.getText());
                if (addMode) {
                    medicalRecordService.create(dto);
                } else {
                    medicalRecordService.update(record.getRecordId(), dto);
                }
                refreshTable();
                formDialogController.close();
                toastSuccess(addMode ? "Medical record added." : "Medical record updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save medical record: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Diagnosis", "fas-stethoscope", diagnosis);
        formDialogController.addField("Symptoms", "fas-head-side-cough", symptoms);
        formDialogController.addField("Notes", "fas-sticky-note", notes);

        loadRecordDropdown(appointmentIdField, otherFields, addMode ? null : record);
    }

    /** Loads the appointment dropdown options asynchronously, showing its own spinner while
     *  data is in flight and keeping the rest of the form disabled until it finishes loading. */
    private void loadRecordDropdown(LoadingIdComboBox appointmentIdField, List<Control> otherFields, MedicalRecordDTO existing) {
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
