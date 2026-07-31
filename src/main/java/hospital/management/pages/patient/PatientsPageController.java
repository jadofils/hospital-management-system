package hospital.management.pages.patient;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.patient.Patient;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.patient.PatientTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PatientsPageController extends BasePageController {

    @FXML private PatientTableController patientTableController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button addPatientBtn;
    @FXML private Label totalLabel;

    private final List<Patient> patients = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PATIENTS);

        statusFilter.getItems().addAll("All", "Admitted", "Discharged", "Pending", "Cancelled");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        addPatientBtn.setOnAction(e -> openPatientDialog(null));
        patientTableController.setRowActions(this::openPatientDialog, this::confirmDeletePatient);

        refreshTable();
    }

    private void applyFilter() {
        patientTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        patientTableController.setItems(patients);
        totalLabel.setText("Total: " + patients.size() + " patients");
    }

    private void confirmDeletePatient(Patient patient) {
        confirm("Delete Patient",
                "Are you sure you want to delete " + patient.getFullName() + "? This cannot be undone.",
                () -> {
                    patients.remove(patient);
                    refreshTable();
                    toastSuccess("Patient deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (patient == null) or Update mode. */
    private void openPatientDialog(Patient patient) {
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

            Patient target = addMode ? new Patient() : patient;
            if (addMode) target.setPatientId(UUID.randomUUID().toString());
            target.setFirstName(fn);
            target.setLastName(ln);
            target.setDob(dob.getValue());
            target.setGender(gender.getValue());
            target.setPhone(phone.getText());
            target.setEmail(email.getText());
            target.setAddress(address.getText());

            if (addMode) patients.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Patient added." : "Patient updated.");
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
