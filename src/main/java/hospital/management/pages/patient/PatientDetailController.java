package hospital.management.pages.patient;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.model.lab.LabOrder;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.MedicalRecord;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.model.patient.PatientAllergy;
import hospital.management.backend.model.patient.VitalSign;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.clinical.AppointmentTableController;
import hospital.management.pages.components.finance.InvoiceTableController;
import hospital.management.pages.components.lab.LabOrderTableController;
import hospital.management.pages.components.clinical.MedicalRecordTableController;
import hospital.management.pages.components.patient.PatientAllergyTableController;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import hospital.management.pages.components.patient.VitalSignTableController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Read-heavy drill-down view for a single patient. Reached by calling
 * {@link #loadPatient(Patient)} directly on the loaded controller instance
 * (no FXML nav-param passing) from a "view details" / row-edit action on
 * the Patients page.
 */
public class PatientDetailController extends BasePageController {

    // Header
    @FXML private Label  patientNameLabel;
    @FXML private Button backBtn;

    // Summary card labels
    @FXML private Label fullNameLabel;
    @FXML private Label patientIdLabel;
    @FXML private Label dobLabel;
    @FXML private Label genderLabel;
    @FXML private Label phoneLabel;
    @FXML private Label emailLabel;
    @FXML private Label addressLabel;
    @FXML private Button editPatientBtn;

    // Tabs
    @FXML private TabPane detailTabs;

    // Vitals tab
    @FXML private Button addVitalBtn;
    @FXML private VitalSignTableController vitalSignTableController;

    // Medical records tab
    @FXML private Button addRecordBtn;
    @FXML private MedicalRecordTableController detailMedicalRecordTableController;

    // Appointments tab (read-only in this drill-down — full CRUD lives on its own page)
    @FXML private AppointmentTableController detailAppointmentTableController;

    // Prescriptions tab
    @FXML private Button addPrescriptionBtn;
    @FXML private PrescriptionTableController detailPrescriptionTableController;

    // Lab results tab (read-only in this drill-down — full CRUD lives on its own page)
    @FXML private LabOrderTableController detailLabOrderTableController;

    // Allergies tab
    @FXML private Button addAllergyBtn;
    @FXML private PatientAllergyTableController patientAllergyTableController;

    // Billing tab (read-only in this drill-down — full CRUD lives on its own page)
    @FXML private InvoiceTableController detailInvoiceTableController;

    private Patient currentPatient;

    private final List<VitalSign>      vitals         = new ArrayList<>();
    private final List<MedicalRecord>  medicalRecords = new ArrayList<>();
    private final List<Appointment>    appointments   = new ArrayList<>();
    private final List<Prescription>   prescriptions  = new ArrayList<>();
    private final List<LabOrder>       labOrders      = new ArrayList<>();
    private final List<PatientAllergy> allergies      = new ArrayList<>();
    private final List<Invoice>        invoices       = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PATIENTS);

        backBtn.setOnAction(e -> navigateBack());
        editPatientBtn.setOnAction(e -> openEditPatientDialog());

        addVitalBtn.setOnAction(e -> openVitalDialog(null));
        vitalSignTableController.setRowActions(this::openVitalDialog, this::confirmDeleteVital);

        addRecordBtn.setOnAction(e -> openRecordDialog(null));
        addPrescriptionBtn.setOnAction(e -> openPrescriptionDialog(null));

        addAllergyBtn.setOnAction(e -> openAllergyDialog(null));
        patientAllergyTableController.setRowActions(this::openAllergyDialog, this::confirmDeleteAllergy);

        // Medical Records, Appointments, Prescriptions, Lab Results and Billing are read-only
        // in this drill-down (full CRUD lives on their own pages) — hide their Actions column
        // instead of leaving dead edit/delete icons with no callback wired.
        detailMedicalRecordTableController.hideActionsColumn();
        detailAppointmentTableController.hideActionsColumn();
        detailPrescriptionTableController.hideActionsColumn();
        detailLabOrderTableController.hideActionsColumn();
        detailInvoiceTableController.hideActionsColumn();

        refreshAllTables();
    }

    /** Opens the shared form dialog to update the currently loaded patient's summary details. */
    private void openEditPatientDialog() {
        if (currentPatient == null) return;

        TextField firstName = new TextField(currentPatient.getFirstName());
        TextField lastName  = new TextField(currentPatient.getLastName());
        DatePicker dob       = new DatePicker(currentPatient.getDob());
        ComboBox<String> gender = new ComboBox<>();
        TextField phone     = new TextField(currentPatient.getPhone());
        TextField email     = new TextField(currentPatient.getEmail());
        TextField address   = new TextField(currentPatient.getAddress());

        List.of(firstName, lastName, phone, email, address).forEach(f -> f.getStyleClass().add("form-input"));
        dob.getStyleClass().add("form-date-picker");
        gender.getStyleClass().add("form-combo");
        gender.getItems().addAll("Male", "Female", "Other");
        gender.setValue(currentPatient.getGender());

        formDialogController.open("Update Patient", "fas-user-injured", false, v -> {
            String fn = firstName.getText() == null ? "" : firstName.getText().trim();
            String ln = lastName.getText() == null ? "" : lastName.getText().trim();
            if (fn.isEmpty() || ln.isEmpty() || dob.getValue() == null || gender.getValue() == null) {
                formDialogController.setError("First name, last name, date of birth and gender are required.");
                formDialogController.setLoading(false);
                return;
            }

            currentPatient.setFirstName(fn);
            currentPatient.setLastName(ln);
            currentPatient.setDob(dob.getValue());
            currentPatient.setGender(gender.getValue());
            currentPatient.setPhone(phone.getText());
            currentPatient.setEmail(email.getText());
            currentPatient.setAddress(address.getText());

            loadPatient(currentPatient);
            formDialogController.close();
            toastSuccess("Patient updated.");
        });

        formDialogController.addField("First Name", "fas-user", firstName);
        formDialogController.addField("Last Name", "fas-user", lastName);
        formDialogController.addField("Date of Birth", "fas-calendar", dob);
        formDialogController.addField("Gender", "fas-venus-mars", gender);
        formDialogController.addField("Phone", "fas-phone", phone);
        formDialogController.addField("Email", "fas-envelope", email);
        formDialogController.addField("Address", "fas-map-marker-alt", address);
    }

    /** Call this from the navigating controller to load a specific patient. */
    public void loadPatient(Patient patient) {
        this.currentPatient = patient;
        fullNameLabel.setText(patient.getFullName());
        patientIdLabel.setText("ID: " + patient.getPatientId());
        dobLabel.setText("DOB: " + (patient.getDob() != null ? patient.getDob() : "—"));
        genderLabel.setText("Gender: " + (patient.getGender() != null ? patient.getGender() : "—"));
        phoneLabel.setText("Phone: " + (patient.getPhone() != null ? patient.getPhone() : "—"));
        emailLabel.setText("Email: " + (patient.getEmail() != null ? patient.getEmail() : "—"));
        addressLabel.setText("Address: " + (patient.getAddress() != null ? patient.getAddress() : "—"));
        patientNameLabel.setText(patient.getFullName());
    }

    private void refreshAllTables() {
        vitalSignTableController.setItems(vitals);
        detailMedicalRecordTableController.setItems(medicalRecords);
        detailAppointmentTableController.setItems(appointments);
        detailPrescriptionTableController.setItems(prescriptions);
        detailLabOrderTableController.setItems(labOrders);
        patientAllergyTableController.setItems(allergies);
        detailInvoiceTableController.setItems(invoices);
    }

    // ── Vitals ────────────────────────────────────────────────────────────

    private void confirmDeleteVital(VitalSign vital) {
        confirm("Delete Vital Record",
                "Are you sure you want to delete this vital record? This cannot be undone.",
                () -> {
                    vitals.remove(vital);
                    vitalSignTableController.setItems(vitals);
                    toastSuccess("Vital record deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (vital == null) or Update mode. */
    private void openVitalDialog(VitalSign vital) {
        boolean addMode = vital == null;

        TextField heartRate   = new TextField();
        TextField temperature = new TextField();
        TextField weight      = new TextField();
        TextField height      = new TextField();
        List.of(heartRate, temperature, weight, height).forEach(f -> f.getStyleClass().add("form-input"));

        if (!addMode) {
            if (vital.getHeartRate() != null) heartRate.setText(String.valueOf(vital.getHeartRate()));
            if (vital.getTemperatureCelsius() != null) temperature.setText(vital.getTemperatureCelsius().toString());
            if (vital.getWeightKg() != null) weight.setText(vital.getWeightKg().toString());
            if (vital.getHeightCm() != null) height.setText(vital.getHeightCm().toString());
        }

        formDialogController.open(addMode ? "Record Vital" : "Update Vital", "fas-heartbeat", addMode, v -> {
            Integer hr;
            BigDecimal temp;
            BigDecimal wt;
            BigDecimal ht;
            try {
                hr = isBlank(heartRate.getText()) ? null : Integer.valueOf(heartRate.getText().trim());
                temp = isBlank(temperature.getText()) ? null : new BigDecimal(temperature.getText().trim());
                wt = isBlank(weight.getText()) ? null : new BigDecimal(weight.getText().trim());
                ht = isBlank(height.getText()) ? null : new BigDecimal(height.getText().trim());
            } catch (NumberFormatException ex) {
                formDialogController.setError("Heart rate must be a whole number; temperature/weight/height must be numeric.");
                formDialogController.setLoading(false);
                return;
            }

            VitalSign target = addMode ? new VitalSign() : vital;
            if (addMode) target.setVitalId(UUID.randomUUID().toString());
            target.setHeartRate(hr);
            target.setTemperatureCelsius(temp);
            target.setWeightKg(wt);
            target.setHeightCm(ht);
            target.setRecordedAt(LocalDateTime.now());

            if (addMode) vitals.add(target);
            vitalSignTableController.setItems(vitals);
            formDialogController.close();
            toastSuccess(addMode ? "Vital recorded." : "Vital updated.");
        });

        formDialogController.addField("Heart Rate (bpm)", "fas-heartbeat", heartRate);
        formDialogController.addField("Temperature (°C)", "fas-thermometer-half", temperature);
        formDialogController.addField("Weight (kg)", "fas-weight", weight);
        formDialogController.addField("Height (cm)", "fas-ruler-vertical", height);
    }

    // ── Medical records ──────────────────────────────────────────────────

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

        formDialogController.open(addMode ? "New Record" : "Update Record", "fas-notes-medical", addMode, v -> {
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

            if (addMode) medicalRecords.add(target);
            detailMedicalRecordTableController.setItems(medicalRecords);
            formDialogController.close();
            toastSuccess(addMode ? "Medical record added." : "Medical record updated.");
        });

        formDialogController.addField("Appointment Id", "fas-calendar-check", appointmentId);
        formDialogController.addField("Diagnosis", "fas-stethoscope", diagnosis);
        formDialogController.addField("Symptoms", "fas-head-side-cough", symptoms);
        formDialogController.addField("Notes", "fas-sticky-note", notes);
    }

    // ── Prescriptions ─────────────────────────────────────────────────────

    /** Opens the shared form dialog in Add mode (prescription == null) or Update mode. */
    private void openPrescriptionDialog(Prescription prescription) {
        boolean addMode = prescription == null;

        TextField appointmentId = new TextField();
        DatePicker dateIssued   = new DatePicker();
        appointmentId.getStyleClass().add("form-input");
        dateIssued.getStyleClass().add("form-date-picker");

        if (!addMode) {
            appointmentId.setText(prescription.getAppointmentId());
            dateIssued.setValue(prescription.getDateIssued());
        }

        formDialogController.open(addMode ? "New Prescription" : "Update Prescription", "fas-prescription", addMode, v -> {
            String apptId = appointmentId.getText() == null ? "" : appointmentId.getText().trim();
            if (apptId.isEmpty() || dateIssued.getValue() == null) {
                formDialogController.setError("Appointment ID and date issued are required.");
                formDialogController.setLoading(false);
                return;
            }

            Prescription target = addMode ? new Prescription() : prescription;
            if (addMode) target.setPrescriptionId(UUID.randomUUID().toString());
            target.setAppointmentId(apptId);
            target.setDateIssued(dateIssued.getValue());

            if (addMode) prescriptions.add(target);
            detailPrescriptionTableController.setItems(prescriptions);
            formDialogController.close();
            toastSuccess(addMode ? "Prescription added." : "Prescription updated.");
        });

        formDialogController.addField("Appointment Id", "fas-calendar-check", appointmentId);
        formDialogController.addField("Date Issued", "fas-calendar", dateIssued);
    }

    // ── Allergies ─────────────────────────────────────────────────────────

    private void confirmDeleteAllergy(PatientAllergy allergy) {
        confirm("Delete Allergy",
                "Are you sure you want to delete the allergy \"" + allergy.getAllergen() + "\"? This cannot be undone.",
                () -> {
                    allergies.remove(allergy);
                    patientAllergyTableController.setItems(allergies);
                    toastSuccess("Allergy deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (allergy == null) or Update mode. */
    private void openAllergyDialog(PatientAllergy allergy) {
        boolean addMode = allergy == null;

        TextField allergen = new TextField();
        TextField reaction = new TextField();
        TextField severity = new TextField();
        List.of(allergen, reaction, severity).forEach(f -> f.getStyleClass().add("form-input"));

        if (!addMode) {
            allergen.setText(allergy.getAllergen());
            reaction.setText(allergy.getReaction());
            severity.setText(allergy.getSeverity());
        }

        formDialogController.open(addMode ? "Add Allergy" : "Update Allergy", "fas-allergies", addMode, v -> {
            String allergenText = allergen.getText() == null ? "" : allergen.getText().trim();
            if (allergenText.isEmpty()) {
                formDialogController.setError("Allergen is required.");
                formDialogController.setLoading(false);
                return;
            }

            PatientAllergy target = addMode ? new PatientAllergy() : allergy;
            if (addMode) {
                target.setAllergyId(UUID.randomUUID().toString());
                target.setCreatedAt(LocalDateTime.now());
                if (currentPatient != null) target.setPatientId(currentPatient.getPatientId());
            } else {
                target.setUpdatedAt(LocalDateTime.now());
            }
            target.setAllergen(allergenText);
            target.setReaction(reaction.getText());
            target.setSeverity(severity.getText());

            if (addMode) allergies.add(target);
            patientAllergyTableController.setItems(allergies);
            formDialogController.close();
            toastSuccess(addMode ? "Allergy added." : "Allergy updated.");
        });

        formDialogController.addField("Allergen", "fas-allergies", allergen);
        formDialogController.addField("Reaction", "fas-notes-medical", reaction);
        formDialogController.addField("Severity", "fas-exclamation-triangle", severity);
    }

    // ── Navigation ────────────────────────────────────────────────────────

    /** Swaps the Back button's icon for a spinner and loads the Patients page in place. */
    private void navigateBack() {
        Node originalGraphic = backBtn.getGraphic();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(14, 14);
        backBtn.setGraphic(spinner);
        backBtn.setDisable(true);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(PageRoute.PATIENTS.getFxmlPath()));
                Parent root = loader.load();
                Scene scene = backBtn.getScene();
                Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
                newScene.getStylesheets().add(
                        getClass().getResource("/hospital/management/css/global.css").toExternalForm()
                );
                ((Stage) scene.getWindow()).setScene(newScene);
            } catch (Exception e) {
                System.err.println("Navigation to patients page failed: " + e.getMessage());
                backBtn.setDisable(false);
                backBtn.setGraphic(originalGraphic);
                toastError("Couldn't return to the Patients page. Please try again.");
            }
        });
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
