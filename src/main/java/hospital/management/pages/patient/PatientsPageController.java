package hospital.management.pages.patient;

import hospital.management.backend.utils.FxFormValidator;
import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.patient.CreatePatientDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.UpdatePatientDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.patient.interfaces.PatientService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.patient.PatientTableController;
import hospital.management.pages.components.shared.search.AdvancedSearchController;
import hospital.management.pages.utils.CsvUiIO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PatientsPageController extends BasePageController implements QuickAddCapable {

    private final PatientService patientService = new PatientServiceImpl(new PatientDAOImpl());

    @FXML private PatientTableController patientTableController;
    @FXML private AdvancedSearchController advancedSearchController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button addPatientBtn;
    @FXML private Button importBtn;
    @FXML private Button exportBtn;
    @FXML private Label totalLabel;

    private List<PatientDTO> patients = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PATIENTS);

        statusFilter.getItems().addAll("All", "Admitted", "Discharged", "Pending", "Cancelled");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        applyCreateVisibility(addPatientBtn, PageRoute.PATIENTS);
        applyCreateVisibility(importBtn, PageRoute.PATIENTS);
        boolean canExport = canRead(PageRoute.PATIENTS);
        exportBtn.setVisible(canExport);
        exportBtn.setManaged(canExport);

        addPatientBtn.setOnAction(e -> openPatientDialog(null));
        importBtn.setOnAction(e -> withSpinner(importBtn, this::importPatients));
        exportBtn.setOnAction(e -> withSpinner(exportBtn, this::exportPatients));
        patientTableController.setRowActions(
            allowUpdate(PageRoute.PATIENTS, this::openPatientDialog),
            allowDelete(PageRoute.PATIENTS, this::confirmDeletePatient),
            allowRead(PageRoute.PATIENTS, this::viewPatientDetail));

        if (advancedSearchController != null) {
            advancedSearchController.setOnSearch(this::applyAdvancedSearch);
            advancedSearchController.setOnReset(this::refreshTable);
        }

        refreshTable();
    }

    private void applyFilter() {
        patientTableController.filter(searchField.getText());
    }

    /** Only patientId maps onto this page's data — doctor/date/status are Appointment-domain
     *  fields the shared component also exposes for its Appointments-page usage. */
    private void applyAdvancedSearch(AdvancedSearchController.Criteria criteria) {
        String patientId = criteria.patientId() == null ? "" : criteria.patientId().trim();
        patientTableController.filter(patientId);
        toastSuccess(patientId.isEmpty() ? "Search cleared." : "Search applied for patient ID \"" + patientId + "\".");
    }

    private void refreshTable() {
        try {
            patients = patientService.findAll(CursorPagination.firstPage(500)).getItems();
            patientTableController.setItems(patients);
            totalLabel.setText("Total: " + patients.size() + " patients");
        } catch (Exception e) {
            toastError("Failed to load patients: " + e.getMessage());
        }
    }

    private void exportPatients() {
        try {
            if (patients.isEmpty()) {
                toastError("No patients available to include in the report.");
                return;
            }
            List<PatientDTO> source = choosePatientExportSource();
            if (source.isEmpty()) {
                return;
            }

            // Deterministic ordering keeps the exported report consistent across runs.
            source.sort(Comparator
                .comparing((PatientDTO p) -> safe(p.getLastName()))
                .thenComparing(p -> safe(p.getFirstName()))
                .thenComparing(PatientDTO::getPatientId));

            List<Map<String, Object>> rows = new ArrayList<>();
            for (PatientDTO patient : source) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("patient_id", patient.getPatientId());
                row.put("first_name", patient.getFirstName());
                row.put("last_name", patient.getLastName());
                row.put("dob", patient.getDob());
                row.put("gender", patient.getGender());
                row.put("phone", patient.getPhone());
                row.put("email", patient.getEmail());
                row.put("address", patient.getAddress());
                rows.add(row);
            }

            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").format(LocalDateTime.now());
            String fileName = "patients_report_" + timestamp + ".csv";
            boolean saved = CsvUiIO.exportRows(exportBtn.getScene().getWindow(), fileName, rows);
            if (saved) {
                toastSuccess("Patients report downloaded successfully.");
            }
        } catch (Exception e) {
            toastError("Failed to download patients report: " + e.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private List<PatientDTO> choosePatientExportSource() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("All loaded rows", "All loaded rows", "Current table view");
        dialog.setTitle("Export Patients");
        dialog.setHeaderText("Choose what to export");
        dialog.setContentText("Export scope:");
        String choice = dialog.showAndWait().orElse(null);
        if (choice == null) {
            return List.of();
        }
        if ("Current table view".equals(choice)) {
            return new ArrayList<>(patientTableController.getTable().getItems());
        }
        return patients;
    }

    private void importPatients() {
        try {
            List<Map<String, String>> rows = CsvUiIO.importRows(importBtn.getScene().getWindow(), "Import Patients");
            if (rows.isEmpty()) {
                return;
            }

            int ok = 0;
            int failed = 0;
            for (Map<String, String> row : rows) {
                try {
                    String firstName = value(row, "first_name", "firstname");
                    String lastName = value(row, "last_name", "lastname");
                    LocalDate dob = LocalDate.parse(value(row, "dob", "date_of_birth"));
                    String gender = value(row, "gender");
                    String phone = value(row, "phone");
                    String email = value(row, "email");
                    String address = value(row, "address");
                    patientService.create(new CreatePatientDTO(firstName, lastName, dob, gender, phone, email, address));
                    ok++;
                } catch (Exception ex) {
                    failed++;
                }
            }

            refreshTable();
            if (failed == 0) {
                toastSuccess("Imported " + ok + " patient(s).");
            } else {
                toastError("Imported " + ok + " patient(s), failed " + failed + ".");
            }
        } catch (Exception e) {
            toastError("Failed to import patients: " + e.getMessage());
        }
    }

    private String value(Map<String, String> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key).trim();
            }
        }
        return "";
    }

    /** Navigates to the full PatientDetailController drill-down page for this patient. */
    private void viewPatientDetail(PatientDTO patient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(PageRoute.PATIENT_DETAIL.getFxmlPath()));
            Parent root = loader.load();
            PatientDetailController controller = loader.getController();
            controller.loadPatient(patient);
            Scene scene = addPatientBtn.getScene();
            Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
            newScene.getStylesheets().add(
                getClass().getResource("/hospital/management/css/global.css").toExternalForm()
            );
            ((Stage) scene.getWindow()).setScene(newScene);
        } catch (Exception e) {
            toastError("Couldn't open patient details: " + e.getMessage());
        }
    }

    private void confirmDeletePatient(PatientDTO patient) {
        confirm("Delete Patient",
                "Are you sure you want to delete " + patient.getFullName() + "? This cannot be undone.",
                () -> {
                    try {
                        patientService.delete(patient.getPatientId());
                        refreshTable();
                        toastSuccess("Patient deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete patient: " + e.getMessage());
                    }
                });
    }

    @Override
    public void openAddDialog() {
        openPatientDialog(null);
    }

    /** Opens the shared form dialog in Add mode (patient == null) or Update mode. */
    private void openPatientDialog(PatientDTO patient) {
        boolean addMode = patient == null;

        TextField firstName = new TextField();
        TextField lastName  = new TextField();
        DatePicker dob       = new DatePicker();
        ComboBox<String> gender = new ComboBox<>();
        TextField phone     = new TextField();
        TextField email     = new TextField();
        TextField address   = new TextField();

        // Placeholders with examples
        firstName.setPromptText("e.g. Jane");
        lastName.setPromptText("e.g. Doe");
        dob.setPromptText("e.g. 1990-05-15");
        phone.setPromptText("e.g. +250 788 000 000 (optional)");
        email.setPromptText("e.g. jane.doe@hospital.com (optional)");
        address.setPromptText("e.g. 123 Main Street, Kigali (optional)");

        List.of(firstName, lastName, phone, email, address).forEach(f -> f.getStyleClass().add("form-input"));
        dob.getStyleClass().add("form-date-picker");
        gender.getStyleClass().add("form-combo");
        gender.getItems().addAll("Male", "Female", "Other", "Prefer not to say");

        // Real-time validation
        FxFormValidator.attachRequired(firstName, null, "First name");
        FxFormValidator.attachRequired(lastName,  null, "Last name");
        FxFormValidator.attachDateRequired(dob,   null, "Date of birth");
        FxFormValidator.attachPastDate(dob,       null, "Date of birth");
        FxFormValidator.attachRequired(gender,    null, "Gender");
        FxFormValidator.attachPhone(phone,        null);
        FxFormValidator.attachEmail(email,        null);
        FxFormValidator.attachMaxLength(address,  null, 255, "Address");

        if (!addMode) {
            firstName.setText(patient.getFirstName());
            lastName.setText(patient.getLastName());
            dob.setValue(patient.getDob());
            gender.setValue(patient.getGender());
            phone.setText(patient.getPhone());
            email.setText(patient.getEmail());
            address.setText(patient.getAddress());
            // Trigger validation state for pre-filled values
            FxFormValidator.applyStyle(firstName, firstName.getText() != null && !firstName.getText().isBlank());
            FxFormValidator.applyStyle(lastName,  lastName.getText()  != null && !lastName.getText().isBlank());
        }

        formDialogController.open(addMode ? "Add Patient" : "Update Patient", "fas-user-injured", addMode, v -> {
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
                FxFormValidator.applyStyle(gender, false);
                formDialogController.setLoading(false);
                return;
            }
            String phoneVal = phone.getText() == null ? "" : phone.getText().trim();
            if (!phoneVal.isEmpty() && !hospital.management.backend.utils.ValidatorUtils.isValidPhone(phoneVal)) {
                formDialogController.setError("Phone number format is invalid (e.g. +250 788 000 000).");
                FxFormValidator.applyStyle(phone, false);
                formDialogController.setLoading(false);
                return;
            }
            String emailVal = email.getText() == null ? "" : email.getText().trim();
            if (!emailVal.isEmpty() && !hospital.management.backend.utils.ValidatorUtils.isValidEmail(emailVal)) {
                formDialogController.setError("Email address format is invalid.");
                FxFormValidator.applyStyle(email, false);
                formDialogController.setLoading(false);
                return;
            }

            try {
                if (addMode) {
                    patientService.create(new CreatePatientDTO(fn, ln, dob.getValue(), gender.getValue(),
                            phone.getText(), email.getText(), address.getText()));
                } else {
                    patientService.update(new UpdatePatientDTO(patient.getPatientId(),
                            phone.getText(), email.getText(), address.getText()));
                }
                refreshTable();
                formDialogController.close();
                toastSuccess(addMode ? "Patient added." : "Patient updated.");
            } catch (AppException ex) {
                // Domain-level validation or expected failures — show inline error
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
                toastError(ex.getMessage());
            } catch (Exception ex) {
                // Unexpected errors (DB constraint violations, SQLExceptions) — surface as toast
                String msg = ex.getMessage() == null ? "Failed to save patient." : ex.getMessage();
                formDialogController.setError("Failed to save patient: " + msg);
                formDialogController.setLoading(false);
                toastError("Failed to save patient: " + (msg.length() > 120 ? msg.substring(0, 120) + "..." : msg));

                // Show full details in an alert so the user (or tester) can copy the DB error.
                try {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Save Failed");
                    alert.setHeaderText("Could not save patient record");
                    alert.getDialogPane().setExpandableContent(new javafx.scene.control.TextArea(msg));
                    alert.initOwner(addPatientBtn.getScene().getWindow());
                    alert.showAndWait();
                } catch (Exception ignore) {
                }
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
}
