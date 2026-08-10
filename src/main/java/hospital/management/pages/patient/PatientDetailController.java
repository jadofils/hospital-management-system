package hospital.management.pages.patient;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.clinical.MedicalRecordDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.finance.InvoiceDAOImpl;
import hospital.management.backend.dao.lab.LabOrderDAOImpl;
import hospital.management.backend.dao.lab.LabResultDAOImpl;
import hospital.management.backend.dao.patient.PatientAllergyDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.patient.VitalSignDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicalInventoryDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicationDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionDAOImpl;
import hospital.management.backend.dao.pharmacy.PrescriptionItemDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.clinical.CreateMedicalRecordDTO;
import hospital.management.backend.dto.clinical.MedicalRecordDTO;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.dto.patient.CreatePatientAllergyDTO;
import hospital.management.backend.dto.patient.CreateVitalSignDTO;
import hospital.management.backend.dto.patient.PatientAllergyDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.UpdatePatientDTO;
import hospital.management.backend.dto.patient.VitalSignDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionDTO;
import hospital.management.backend.dto.pharmacy.CreatePrescriptionItemDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.clinical.MedicalRecordServiceImpl;
import hospital.management.backend.service.clinical.interfaces.MedicalRecordService;
import hospital.management.backend.service.finance.InvoiceServiceImpl;
import hospital.management.backend.service.finance.interfaces.InvoiceService;
import hospital.management.backend.service.lab.LabServiceImpl;
import hospital.management.backend.service.lab.interfaces.LabService;
import hospital.management.backend.service.patient.AllergyServiceImpl;
import hospital.management.backend.service.patient.interfaces.AllergyService;
import hospital.management.backend.service.patient.interfaces.PatientService;
import hospital.management.backend.service.patient.interfaces.VitalSignService;
import hospital.management.backend.dao.patient.PatientFeedbackDAOImpl;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.dto.patient.PatientNoteDTO;
import hospital.management.backend.service.patient.PatientFeedbackServiceImpl;
import hospital.management.backend.service.patient.PatientNotesNoSqlService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.service.patient.interfaces.PatientFeedbackService;
import hospital.management.backend.service.patient.VitalSignServiceImpl;
import hospital.management.backend.service.pharmacy.PharmacyServiceImpl;
import hospital.management.backend.service.pharmacy.PrescriptionServiceImpl;
import hospital.management.backend.service.pharmacy.interfaces.PrescriptionService;
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.clinical.AppointmentTableController;
import hospital.management.pages.components.finance.InvoiceTableController;
import hospital.management.pages.components.lab.LabOrderTableController;
import hospital.management.pages.components.clinical.MedicalRecordTableController;
import hospital.management.pages.components.patient.PatientAllergyTableController;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import hospital.management.pages.components.patient.VitalSignTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

import hospital.management.backend.model.enums.Gender;

/**
 * Read-heavy drill-down view for a single patient. Reached by calling
 * {@link #loadPatient(PatientDTO)} directly on the loaded controller instance
 * (no FXML nav-param passing) from a "view details" / row-edit action on
 * the Patients page.
 */
public class PatientDetailController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final PatientService patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final VitalSignService vitalSignService = new VitalSignServiceImpl(new VitalSignDAOImpl());
    private final AllergyService allergyService = new AllergyServiceImpl(new PatientAllergyDAOImpl());
    private final MedicalRecordService medicalRecordService = new MedicalRecordServiceImpl(new MedicalRecordDAOImpl());
    private final PrescriptionService prescriptionService =
            new PrescriptionServiceImpl(new PrescriptionDAOImpl(), new PrescriptionItemDAOImpl());
    private final LabService labService = new LabServiceImpl(new LabOrderDAOImpl(), new LabResultDAOImpl());
    private final InvoiceService invoiceService = new InvoiceServiceImpl(new InvoiceDAOImpl(), new PatientDAOImpl());
    private final PharmacyServiceImpl pharmacyService = new PharmacyServiceImpl(
        new MedicationDAOImpl(), new MedicalInventoryDAOImpl());
    private final PatientNotesNoSqlService patientNotesNoSqlService = new PatientNotesNoSqlService();
    private final PatientFeedbackService feedbackService =
        new PatientFeedbackServiceImpl(new PatientFeedbackDAOImpl());

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

    // Notes tab
    @FXML private Button addNoteBtn;
    @FXML private TableView<PatientNoteDTO> notesTable;
    @FXML private TableColumn<PatientNoteDTO, String> noteDateCol;
    @FXML private TableColumn<PatientNoteDTO, String> noteRoleCol;
    @FXML private TableColumn<PatientNoteDTO, String> noteSourceCol;
    @FXML private TableColumn<PatientNoteDTO, String> noteTextCol;

    // Feedback tab
    @FXML private Button addFeedbackBtn;
    @FXML private TableView<PatientFeedbackDTO> feedbackTable;
    @FXML private TableColumn<PatientFeedbackDTO, String> feedbackDateCol;
    @FXML private TableColumn<PatientFeedbackDTO, String> feedbackRatingCol;
    @FXML private TableColumn<PatientFeedbackDTO, String> feedbackCommentsCol;

    private PatientDTO currentPatient;

    private final List<VitalSignDTO>      vitals         = new ArrayList<>();
    private final List<MedicalRecordDTO>  medicalRecords = new ArrayList<>();
    private final List<AppointmentDTO>    appointments   = new ArrayList<>();
    private final List<PrescriptionDTO>   prescriptions  = new ArrayList<>();
    private final List<LabOrderDTO>       labOrders      = new ArrayList<>();
    private final List<PatientAllergyDTO> allergies      = new ArrayList<>();
    private final List<InvoiceDTO>        invoices       = new ArrayList<>();
    private final List<PatientNoteDTO>    notes          = new ArrayList<>();
    private final List<PatientFeedbackDTO> feedbacks     = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PATIENTS);

        backBtn.setOnAction(e -> navigateBack());
        editPatientBtn.setOnAction(e -> openEditPatientDialog());
        editPatientBtn.setVisible(canUpdate(PageRoute.PATIENT_DETAIL));
        editPatientBtn.setManaged(canUpdate(PageRoute.PATIENT_DETAIL));

        applyCreateVisibility(addVitalBtn, PageRoute.PATIENT_DETAIL);
        addVitalBtn.setOnAction(e -> openVitalDialog(null));
        vitalSignTableController.setRowActions(
            allowUpdate(PageRoute.PATIENT_DETAIL, this::openVitalDialog),
            allowDelete(PageRoute.PATIENT_DETAIL, this::confirmDeleteVital),
            allowRead(PageRoute.PATIENT_DETAIL, this::viewVitalDetail));

        applyCreateVisibility(addRecordBtn, PageRoute.MEDICAL_RECORDS);
        applyCreateVisibility(addPrescriptionBtn, PageRoute.PRESCRIPTIONS);
        addRecordBtn.setOnAction(e -> openRecordDialog(null));
        addPrescriptionBtn.setOnAction(e -> openPrescriptionDialog());

        applyCreateVisibility(addAllergyBtn, PageRoute.PATIENT_DETAIL);
        addAllergyBtn.setOnAction(e -> openAllergyDialog(null));
        patientAllergyTableController.setRowActions(
            allowUpdate(PageRoute.PATIENT_DETAIL, this::openAllergyDialog),
            allowDelete(PageRoute.PATIENT_DETAIL, this::confirmDeleteAllergy),
            allowRead(PageRoute.PATIENT_DETAIL, this::viewAllergyDetail));

        // Medical Records, Appointments, Prescriptions, Lab Results and Billing are read-only
        // in this drill-down (full CRUD lives on their own pages) — hide their Actions column
        // instead of leaving dead edit/delete icons with no callback wired.
        detailMedicalRecordTableController.hideActionsColumn();
        detailAppointmentTableController.hideActionsColumn();
        detailPrescriptionTableController.hideActionsColumn();
        detailLabOrderTableController.hideActionsColumn();
        detailInvoiceTableController.hideActionsColumn();

        // Notes tab — wire columns and bind observable list
        noteDateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCreatedAt() == null ? "—" : c.getValue().getCreatedAt().toLocalDate().toString()));
        noteRoleCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getAuthorRole() != null ? c.getValue().getAuthorRole() : "—"));
        noteSourceCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getSource() != null ? c.getValue().getSource() : "—"));
        noteTextCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNoteText()));
        notesTable.setItems(FXCollections.observableList(notes));
        applyCreateVisibility(addNoteBtn, PageRoute.PATIENT_DETAIL);
        addNoteBtn.setOnAction(e -> openAddNoteDialog());

        // Feedback tab — wire columns and bind observable list
        feedbackDateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDateSubmitted() == null ? "—" : c.getValue().getDateSubmitted().toString()));
        feedbackRatingCol.setCellValueFactory(c -> {
            Integer r = c.getValue().getRating();
            return new SimpleStringProperty(r == null ? "—" : "★".repeat(r) + "☆".repeat(Math.max(0, 5 - r)));
        });
        feedbackCommentsCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getComments() != null ? c.getValue().getComments() : ""));
        feedbackTable.setItems(FXCollections.observableList(feedbacks));
        applyCreateVisibility(addFeedbackBtn, PageRoute.PATIENT_DETAIL);
        addFeedbackBtn.setOnAction(e -> openSubmitFeedbackDialog());

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

        // Placeholders
        firstName.setPromptText("e.g. Jane");
        lastName.setPromptText("e.g. Doe");
        phone.setPromptText("e.g. +250 788 000 000 (optional)");
        email.setPromptText("e.g. jane.doe@hospital.com (optional)");
        address.setPromptText("e.g. 123 Main Street, Kigali (optional)");

        List.of(firstName, lastName, phone, email, address).forEach(f -> f.getStyleClass().add("form-input"));
        dob.getStyleClass().add("form-date-picker");
        gender.getStyleClass().add("form-combo");
        // Populate from the canonical Gender enum labels to ensure consistency with DTOs/schema
        gender.getItems().setAll(Arrays.stream(Gender.values()).map(Gender::getLabel).collect(Collectors.toList()));
        gender.setValue(currentPatient.getGender());

        // Real-time validators
        FxFormValidator.attachRequired(firstName, null, "First name");
        FxFormValidator.attachRequired(lastName,  null, "Last name");
        FxFormValidator.attachDateRequired(dob,   null, "Date of birth");
        FxFormValidator.attachPastDate(dob,       null, "Date of birth");
        FxFormValidator.attachPhone(phone,        null);
        FxFormValidator.attachEmail(email,        null);
        FxFormValidator.attachMaxLength(address,  null, 255, "Address");
        // Pre-fill triggers validation state
        FxFormValidator.applyStyle(firstName, firstName.getText() != null && !firstName.getText().isBlank());
        FxFormValidator.applyStyle(lastName,  lastName.getText()  != null && !lastName.getText().isBlank());

        formDialogController.open("Update Patient", "fas-user-injured", false, v -> {
            String fn = firstName.getText() == null ? "" : firstName.getText().trim();
            String ln = lastName.getText() == null ? "" : lastName.getText().trim();
            if (fn.isEmpty()) {
                formDialogController.setError("First name is required.");
                FxFormValidator.applyStyle(firstName, false);
                formDialogController.setLoading(false);
                return;
            }
            if (ln.isEmpty()) {
                formDialogController.setError("Last name is required.");
                FxFormValidator.applyStyle(lastName, false);
                formDialogController.setLoading(false);
                return;
            }
            if (dob.getValue() == null) {
                formDialogController.setError("Date of birth is required.");
                FxFormValidator.applyStyle(dob, false);
                formDialogController.setLoading(false);
                return;
            }
            if (gender.getValue() == null) {
                formDialogController.setError("Gender is required.");
                formDialogController.setLoading(false);
                return;
            }
            String phoneVal = phone.getText() == null ? "" : phone.getText().trim();
            if (!phoneVal.isEmpty() && !ValidatorUtils.isValidPhone(phoneVal)) {
                formDialogController.setError("Phone number format is invalid.");
                FxFormValidator.applyStyle(phone, false);
                formDialogController.setLoading(false);
                return;
            }
            String emailVal = email.getText() == null ? "" : email.getText().trim();
            if (!emailVal.isEmpty() && !ValidatorUtils.isValidEmail(emailVal)) {
                formDialogController.setError("Email address format is invalid.");
                FxFormValidator.applyStyle(email, false);
                formDialogController.setLoading(false);
                return;
            }

            try {
                PatientDTO updated = patientService.update(new UpdatePatientDTO(
                        currentPatient.getPatientId(), phone.getText(), email.getText(), address.getText()));
                loadPatient(updated);
                formDialogController.close();
                toastSuccess("Patient updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save patient: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
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
    public void loadPatient(PatientDTO patient) {
        this.currentPatient = patient;
        fullNameLabel.setText(patient.getFullName());
        patientIdLabel.setText("ID: " + patient.getPatientId());
        dobLabel.setText("DOB: " + (patient.getDob() != null ? patient.getDob() : "—"));
        genderLabel.setText("Gender: " + (patient.getGender() != null ? patient.getGender() : "—"));
        phoneLabel.setText("Phone: " + (patient.getPhone() != null ? patient.getPhone() : "—"));
        emailLabel.setText("Email: " + (patient.getEmail() != null ? patient.getEmail() : "—"));
        addressLabel.setText("Address: " + (patient.getAddress() != null ? patient.getAddress() : "—"));
        patientNameLabel.setText(patient.getFullName());
        refreshVitals();
        refreshAllergies();
        refreshAppointments();
        refreshInvoices();
        refreshNotes();
        refreshFeedback();
    }

    private void refreshVitals() {
        if (currentPatient == null) return;
        try {
            vitals.clear();
            vitals.addAll(vitalSignService.findByPatient(currentPatient.getPatientId()));
            vitalSignTableController.setItems(vitals);
        } catch (Exception e) {
            toastError("Failed to load vitals: " + e.getMessage());
        }
    }

    private void refreshAllergies() {
        if (currentPatient == null) return;
        try {
            allergies.clear();
            allergies.addAll(allergyService.findByPatient(currentPatient.getPatientId()));
            patientAllergyTableController.setItems(allergies);
        } catch (Exception e) {
            toastError("Failed to load allergies: " + e.getMessage());
        }
    }

    private void refreshAppointments() {
        if (currentPatient == null) return;
        try {
            appointments.clear();
            appointments.addAll(appointmentService.findByPatient(currentPatient.getPatientId()));
            detailAppointmentTableController.setItems(appointments);
            refreshMedicalRecords();
            refreshPrescriptions();
            refreshLabOrders();
        } catch (Exception e) {
            toastError("Failed to load appointments: " + e.getMessage());
        }
    }

    private void refreshMedicalRecords() {
        try {
            medicalRecords.clear();
            for (AppointmentDTO appointment : appointments) {
                try {
                    medicalRecords.add(medicalRecordService.findByAppointment(appointment.getAppointmentId()));
                } catch (ResourceNotFoundException ignored) {
                }
            }
            detailMedicalRecordTableController.setItems(medicalRecords);
        } catch (Exception e) {
            toastError("Failed to load medical records: " + e.getMessage());
        }
    }

    private void refreshPrescriptions() {
        try {
            prescriptions.clear();
            for (AppointmentDTO appointment : appointments) {
                try {
                    prescriptions.add(prescriptionService.findByAppointment(appointment.getAppointmentId()));
                } catch (ResourceNotFoundException ignored) {
                }
            }
            detailPrescriptionTableController.setItems(prescriptions);
        } catch (Exception e) {
            toastError("Failed to load prescriptions: " + e.getMessage());
        }
    }

    private void refreshLabOrders() {
        try {
            labOrders.clear();
            for (AppointmentDTO appointment : appointments) {
                labOrders.addAll(labService.findOrdersByAppointment(appointment.getAppointmentId()));
            }
            detailLabOrderTableController.setItems(labOrders);
        } catch (Exception e) {
            toastError("Failed to load lab orders: " + e.getMessage());
        }
    }

    private void refreshInvoices() {
        if (currentPatient == null) return;
        try {
            invoices.clear();
            invoices.addAll(invoiceService.findByPatient(currentPatient.getPatientId()));
            detailInvoiceTableController.setItems(invoices);
        } catch (Exception e) {
            toastError("Failed to load invoices: " + e.getMessage());
        }
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

    private void viewVitalDetail(VitalSignDTO vital) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Heart Rate (bpm)", vital.getHeartRate() == null ? null : String.valueOf(vital.getHeartRate()));
        fields.put("Blood Pressure", (vital.getBloodPressureSystolic() == null || vital.getBloodPressureDiastolic() == null)
                ? null : vital.getBloodPressureSystolic() + "/" + vital.getBloodPressureDiastolic());
        fields.put("Temperature (°C)", vital.getTemperatureCelsius() == null ? null : vital.getTemperatureCelsius().toPlainString());
        fields.put("Weight (kg)", vital.getWeightKg() == null ? null : vital.getWeightKg().toPlainString());
        fields.put("Height (cm)", vital.getHeightCm() == null ? null : vital.getHeightCm().toPlainString());
        fields.put("Recorded At", vital.getRecordedAt() == null ? null : vital.getRecordedAt().toString());
        detailViewController.show("Vital Record Details", "fas-heartbeat", fields);
    }

    private void confirmDeleteVital(VitalSignDTO vital) {
        confirm("Delete Vital Record",
                "Are you sure you want to delete this vital record? This cannot be undone.",
                () -> {
                    try {
                        vitalSignService.delete(vital.getVitalId());
                        refreshVitals();
                        toastSuccess("Vital record deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete vital record: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog to record a vital reading for the current patient's appointment. */
    private void openVitalDialog(VitalSignDTO vital) {
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        TextField heartRate   = new TextField();
        TextField temperature = new TextField();
        TextField weight      = new TextField();
        TextField height      = new TextField();
        heartRate.setPromptText("e.g. 72 (bpm, optional)");
        temperature.setPromptText("e.g. 36.5 (°C, optional)");
        weight.setPromptText("e.g. 70.5 (kg, optional)");
        height.setPromptText("e.g. 175 (cm, optional)");

        List.of(heartRate, temperature, weight, height).forEach(f -> f.getStyleClass().add("form-input"));
        appointmentId.getStyleClass().add("form-combo");

        List<Control> vitalOtherFields = List.of(heartRate, temperature, weight, height);
        vitalOtherFields.forEach(f -> f.setDisable(true));

        if (vital != null) {
            if (vital.getHeartRate() != null) heartRate.setText(String.valueOf(vital.getHeartRate()));
            if (vital.getTemperatureCelsius() != null) temperature.setText(vital.getTemperatureCelsius().toString());
            if (vital.getWeightKg() != null) weight.setText(vital.getWeightKg().toString());
            if (vital.getHeightCm() != null) height.setText(vital.getHeightCm().toString());
        }

        formDialogController.open("Record Vital", "fas-heartbeat", true, v -> {
            String appt = appointmentId.getSelectedId();
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
            if (appt == null) {
                formDialogController.setError("Appointment is required.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                vitalSignService.record(new CreateVitalSignDTO(appt, null, null, hr, temp, wt, ht));
                refreshVitals();
                formDialogController.close();
                toastSuccess("Vital recorded.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save vital: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Heart Rate (bpm)", "fas-heartbeat", heartRate);
        formDialogController.addField("Temperature (°C)", "fas-thermometer-half", temperature);
        formDialogController.addField("Weight (kg)", "fas-weight", weight);
        formDialogController.addField("Height (cm)", "fas-ruler-vertical", height);

        loadAppointmentDropdown(appointmentIdField, vitalOtherFields, null);
    }

    // ── Medical records ──────────────────────────────────────────────────

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

        FxFormValidator.attachRequired(diagnosis, null, "Diagnosis");

        List<Control> recordOtherFields = List.of(diagnosis, symptoms, notes);
        recordOtherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            diagnosis.setText(record.getDiagnosis());
            symptoms.setText(record.getSymptoms());
            notes.setText(record.getNotes());
            FxFormValidator.applyStyle(diagnosis, diagnosis.getText() != null && !diagnosis.getText().isBlank());
        }

        formDialogController.open(addMode ? "New Record" : "Update Record", "fas-notes-medical", addMode, v -> {
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

                mirrorNotesToNoSql(appt, notes.getText());

                refreshMedicalRecords();
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

        loadAppointmentDropdown(appointmentIdField, recordOtherFields, addMode ? null : record.getAppointmentId());
    }

    private void mirrorNotesToNoSql(String appointmentId, String noteText) {
        try {
            if (currentPatient == null || noteText == null || noteText.trim().isEmpty()) {
                return;
            }
            String role = SessionManager.getCurrentRole();
            if (!("doctor".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role))) {
                return;
            }
            patientNotesNoSqlService.saveNote(
                    currentPatient.getPatientId(),
                    appointmentId,
                    SessionManager.getCurrentUserId(),
                    role,
                    noteText.trim());
        } catch (Exception ignored) {
            // Best-effort mirror: SQL medical record remains the source of truth.
        }
    }

    // ── Prescriptions ─────────────────────────────────────────────────────

    /** Opens the shared form dialog to issue a new prescription for the current patient. */
    private void openPrescriptionDialog() {
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        DatePicker dateIssued   = new DatePicker();

        LoadingIdComboBox medicationField = new LoadingIdComboBox();
        EntityIdComboBox medicationId = medicationField.getComboBox();
        TextField dosage        = new TextField();
        TextField quantity      = new TextField();
        TextField instructions  = new TextField();
        dosage.setPromptText("e.g. 500mg twice daily");
        quantity.setPromptText("e.g. 30");
        instructions.setPromptText("e.g. Take with food (optional)");
        Button addItemBtn    = new Button("Add Item");
        Button removeItemBtn = new Button("Remove Selected");
        ListView<String> itemsList = new ListView<>();
        itemsList.setPrefHeight(110);

        appointmentId.getStyleClass().add("form-combo");
        dateIssued.getStyleClass().add("form-date-picker");
        List.of(dosage, quantity, instructions).forEach(f -> f.getStyleClass().add("form-input"));
        medicationId.getStyleClass().add("form-combo");
        addItemBtn.getStyleClass().add("secondary-button");
        removeItemBtn.getStyleClass().add("secondary-button");

        FxFormValidator.attachDateRequired(dateIssued, null, "Date issued");

        List<Control> prescriptionOtherFields = List.of(dateIssued);
        prescriptionOtherFields.forEach(f -> f.setDisable(true));
        List<Control> itemEditorFields = List.of(dosage, quantity, instructions, addItemBtn, removeItemBtn);
        itemEditorFields.forEach(f -> f.setDisable(true));

        List<CreatePrescriptionItemDTO> draftItems = new ArrayList<>();

        addItemBtn.setOnAction(e -> {
            String medId = medicationId.getSelectedId();
            String medLabel = medicationId.getValue() == null ? "" : medicationId.getValue().label();
            String qtyText = quantity.getText() == null ? "" : quantity.getText().trim();
            if (medId == null) {
                toastError("Select a medication first.");
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyText);
            } catch (NumberFormatException ex) {
                toastError("Quantity must be a whole number.");
                return;
            }
            if (qty <= 0) {
                toastError("Quantity must be greater than zero.");
                return;
            }
            draftItems.add(new CreatePrescriptionItemDTO(medId, dosage.getText(), qty, instructions.getText()));
            itemsList.getItems().add(medLabel + " | " + dosage.getText() + " | qty " + qty
                    + (instructions.getText() == null || instructions.getText().isBlank() ? "" : " | " + instructions.getText()));
            dosage.clear();
            quantity.clear();
            instructions.clear();
        });

        removeItemBtn.setOnAction(e -> {
            int index = itemsList.getSelectionModel().getSelectedIndex();
            if (index < 0) {
                toastError("Select an item in the list to remove it.");
                return;
            }
            itemsList.getItems().remove(index);
            draftItems.remove(index);
        });

        VBox itemsBox = new VBox(6);
        HBox itemInputRow = new HBox(6, medicationField, dosage, quantity, instructions, addItemBtn);
        itemInputRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        itemsBox.getChildren().addAll(itemInputRow, itemsList, removeItemBtn);

        formDialogController.open("New Prescription", "fas-prescription", true, v -> {
            String apptId = appointmentId.getSelectedId();
            if (apptId == null || dateIssued.getValue() == null) {
                formDialogController.setError("Appointment and date issued are required.");
                formDialogController.setLoading(false);
                return;
            }
            if (draftItems.isEmpty()) {
                formDialogController.setError("A prescription must include at least one medication item.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                prescriptionService.issue(new CreatePrescriptionDTO(apptId, dateIssued.getValue(), List.copyOf(draftItems)));
                refreshPrescriptions();
                formDialogController.close();
                toastSuccess("Prescription issued.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to issue prescription: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Date Issued", "fas-calendar", dateIssued);
        formDialogController.addField("Medication", "fas-pills", medicationField);
        formDialogController.addRow(itemsBox);

        loadPrescriptionDropdowns(appointmentIdField, medicationField, prescriptionOtherFields, itemEditorFields);
    }

    /** Loads the appointment and medication dropdown options asynchronously for the prescription dialog. */
    private void loadPrescriptionDropdowns(LoadingIdComboBox appointmentIdField, LoadingIdComboBox medicationField,
                                           List<Control> otherFields, List<Control> itemEditorFields) {
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox medicationId = medicationField.getComboBox();

        appointmentIdField.setLoading(true);
        medicationField.setLoading(true);
        formDialogController.setLoading(true);

        java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger(2);
        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                itemEditorFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

        AsyncJobRunner.submit(
            () -> appointmentService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                appointmentId.setOptions(items.stream()
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
                appointmentIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                appointmentIdField.setLoading(false);
                toastError("Failed to load appointments: " + ex.getMessage());
                onOneLoaded.run();
            });

        AsyncJobRunner.submit(
            pharmacyService::findAllMedications,
            items -> {
                medicationId.setOptions(items.stream()
                        .map(m -> new EntityIdComboBox.Option(m.getMedicationId(), m.getName())).toList());
                medicationField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                medicationField.setLoading(false);
                toastError("Failed to load medications: " + ex.getMessage());
                onOneLoaded.run();
            });
    }

    /** Loads the appointment dropdown options asynchronously (shared by the Medical Record and
     *  Prescription dialogs), showing its own spinner while data is in flight and keeping the
     *  rest of the calling form disabled until it finishes loading. */
    private void loadAppointmentDropdown(LoadingIdComboBox appointmentIdField, List<Control> otherFields, String existingAppointmentId) {
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
                if (existingAppointmentId != null) appointmentId.selectById(existingAppointmentId);
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

    // ── Allergies ─────────────────────────────────────────────────────────

    private void viewAllergyDetail(PatientAllergyDTO allergy) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Allergen", allergy.getAllergen());
        fields.put("Reaction", allergy.getReaction());
        fields.put("Severity", allergy.getSeverity());
        fields.put("Recorded At", allergy.getCreatedAt() == null ? null : allergy.getCreatedAt().toString());
        detailViewController.show("Allergy Details", "fas-allergies", fields);
    }

    private void confirmDeleteAllergy(PatientAllergyDTO allergy) {
        confirm("Delete Allergy",
                "Are you sure you want to delete the allergy \"" + allergy.getAllergen() + "\"? This cannot be undone.",
                () -> {
                    try {
                        allergyService.delete(allergy.getAllergyId());
                        refreshAllergies();
                        toastSuccess("Allergy deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete allergy: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog to record a new allergy for the current patient. */
    private void openAllergyDialog(PatientAllergyDTO allergy) {
        TextField allergen = new TextField();
        TextField reaction = new TextField();
        TextField severity = new TextField();

        allergen.setPromptText("e.g. Penicillin, Peanuts");
        reaction.setPromptText("e.g. Rash, anaphylaxis (optional)");
        severity.setPromptText("e.g. Mild, Moderate, Severe (optional)");

        List.of(allergen, reaction, severity).forEach(f -> f.getStyleClass().add("form-input"));

        FxFormValidator.attachRequired(allergen, null, "Allergen");

        if (allergy != null) {
            allergen.setText(allergy.getAllergen());
            reaction.setText(allergy.getReaction());
            severity.setText(allergy.getSeverity());
            FxFormValidator.applyStyle(allergen, allergen.getText() != null && !allergen.getText().isBlank());
        }

        formDialogController.open("Add Allergy", "fas-allergies", true, v -> {
            String allergenText = allergen.getText() == null ? "" : allergen.getText().trim();
            if (allergenText.isEmpty()) {
                formDialogController.setError("Allergen is required.");
                FxFormValidator.applyStyle(allergen, false);
                formDialogController.setLoading(false);
                return;
            }
            if (currentPatient == null) {
                formDialogController.setError("No patient loaded.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                allergyService.add(new CreatePatientAllergyDTO(
                        currentPatient.getPatientId(), allergenText, reaction.getText(), severity.getText()));
                refreshAllergies();
                formDialogController.close();
                toastSuccess("Allergy added.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save allergy: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Allergen", "fas-allergies", allergen);
        formDialogController.addField("Reaction", "fas-notes-medical", reaction);
        formDialogController.addField("Severity", "fas-exclamation-triangle", severity);
    }

    // ── Notes ─────────────────────────────────────────────────────────────

    private void refreshNotes() {
        if (currentPatient == null) return;
        try {
            notes.clear();
            notes.addAll(patientNotesNoSqlService.findByPatientId(currentPatient.getPatientId()));
            notesTable.refresh();
        } catch (Exception e) {
            toastError("Failed to load clinical notes: " + e.getMessage());
        }
    }

    private void openAddNoteDialog() {
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        appointmentId.getStyleClass().add("form-combo");

        TextArea noteText = new TextArea();
        noteText.setPrefRowCount(4);
        noteText.getStyleClass().add("form-input");
        noteText.setPromptText("Enter clinical note…");
        noteText.setDisable(true);

        formDialogController.open("Add Clinical Note", "fas-sticky-note", true, v -> {
            String text = noteText.getText() == null ? "" : noteText.getText().trim();
            if (text.isEmpty()) {
                formDialogController.setError("Note text is required.");
                formDialogController.setLoading(false);
                return;
            }
            try {
                patientNotesNoSqlService.saveNote(
                    currentPatient.getPatientId(),
                    appointmentId.getSelectedId(),
                    SessionManager.getCurrentUserId(),
                    SessionManager.getCurrentRole(),
                    text);
                refreshNotes();
                formDialogController.close();
                toastSuccess("Note added.");
            } catch (Exception ex) {
                formDialogController.setError("Failed to save note: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment (optional)", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Note", "fas-sticky-note", noteText);
        loadAppointmentDropdown(appointmentIdField, List.of(noteText), null);
    }

    // ── Feedback ──────────────────────────────────────────────────────────

    private void refreshFeedback() {
        if (currentPatient == null) return;
        try {
            feedbacks.clear();
            feedbacks.addAll(feedbackService.findByPatientId(currentPatient.getPatientId()));
            feedbackTable.refresh();
        } catch (Exception e) {
            toastError("Failed to load feedback: " + e.getMessage());
        }
    }

    private void openSubmitFeedbackDialog() {
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        appointmentId.getStyleClass().add("form-combo");

        ComboBox<Integer> rating = new ComboBox<>();
        rating.getItems().setAll(1, 2, 3, 4, 5);
        rating.setValue(5);
        rating.getStyleClass().add("form-combo");
        rating.setDisable(true);

        TextArea comments = new TextArea();
        comments.setPrefRowCount(3);
        comments.getStyleClass().add("form-input");
        comments.setPromptText("Comments (optional)…");
        comments.setDisable(true);

        formDialogController.open("Submit Feedback", "fas-star", true, v -> {
            if (rating.getValue() == null) {
                formDialogController.setError("Rating is required (1–5).");
                formDialogController.setLoading(false);
                return;
            }
            try {
                PatientFeedbackDTO dto = new PatientFeedbackDTO();
                dto.setSubmittedBy(SessionManager.getCurrentUserId());
                dto.setPatientId(currentPatient.getPatientId());
                dto.setAppointmentId(appointmentId.getSelectedId());
                dto.setRating(rating.getValue());
                dto.setComments(comments.getText());
                dto.setDateSubmitted(java.time.LocalDate.now());
                feedbackService.submitFeedback(dto);
                refreshFeedback();
                formDialogController.close();
                toastSuccess("Feedback submitted.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to submit feedback: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment (optional)", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Rating (1–5 stars)", "fas-star", rating);
        formDialogController.addField("Comments", "fas-comment-alt", comments);
        loadAppointmentDropdown(appointmentIdField, List.of(rating, comments), null);
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
