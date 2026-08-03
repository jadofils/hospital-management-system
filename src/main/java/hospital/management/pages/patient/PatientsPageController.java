package hospital.management.pages.patient;

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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class PatientsPageController extends BasePageController implements QuickAddCapable {

    private final PatientService patientService = new PatientServiceImpl(new PatientDAOImpl());

    @FXML private PatientTableController patientTableController;
    @FXML private AdvancedSearchController advancedSearchController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button addPatientBtn;
    @FXML private Label totalLabel;

    private List<PatientDTO> patients = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PATIENTS);

        statusFilter.getItems().addAll("All", "Admitted", "Discharged", "Pending", "Cancelled");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        addPatientBtn.setOnAction(e -> openPatientDialog(null));
        patientTableController.setRowActions(this::openPatientDialog, this::confirmDeletePatient, this::viewPatientDetail);

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

        List.of(firstName, lastName, phone, email, address).forEach(f -> f.getStyleClass().add("form-input"));
        dob.getStyleClass().add("form-date-picker");
        gender.getStyleClass().add("form-combo");
        gender.getItems().addAll("Male", "Female", "Other");

        if (!addMode) {
            firstName.setText(patient.getFirstName());
            lastName.setText(patient.getLastName());
            dob.setValue(patient.getDob());
            gender.setValue(patient.getGender());
            phone.setText(patient.getPhone());
            email.setText(patient.getEmail());
            address.setText(patient.getAddress());
        }

        formDialogController.open(addMode ? "Add Patient" : "Update Patient", "fas-user-injured", addMode, v -> {
            String fn = firstName.getText() == null ? "" : firstName.getText().trim();
            String ln = lastName.getText() == null ? "" : lastName.getText().trim();
            if (fn.isEmpty() || ln.isEmpty() || dob.getValue() == null || gender.getValue() == null) {
                formDialogController.setError("First name, last name, date of birth and gender are required.");
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
