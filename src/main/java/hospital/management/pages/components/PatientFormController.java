package hospital.management.pages.components;

import hospital.management.backend.model.enums.Gender;
import hospital.management.enums.BloodGroup;
import hospital.management.enums.PatientFormStep;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.Arrays;

public class PatientFormController {

    @FXML private Label step1Indicator;
    @FXML private Label step2Indicator;
    @FXML private Label step3Indicator;

    @FXML private VBox step1Pane;
    @FXML private VBox step2Pane;
    @FXML private VBox step3Pane;

    @FXML private TextField        firstNameField;
    @FXML private TextField        lastNameField;
    @FXML private DatePicker       dobPicker;
    @FXML private ComboBox<String> genderCombo;

    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;

    @FXML private ComboBox<String> bloodGroupCombo;
    @FXML private ComboBox<String> assignedDoctorCombo;
    @FXML private TextArea         allergiesArea;

    @FXML private Label  validationMessage;
    @FXML private Button backBtn;
    @FXML private Button nextBtn;
    @FXML private Button submitBtn;

    private PatientFormStep currentStep = PatientFormStep.PERSONAL_INFO;

    public void initialize() {
        Arrays.stream(Gender.values())
              .map(Gender::getLabel)
              .forEach(genderCombo.getItems()::add);

        Arrays.stream(BloodGroup.values())
              .map(BloodGroup::getLabel)
              .forEach(bloodGroupCombo.getItems()::add);

        assignedDoctorCombo.getItems().addAll("Dr. Smith", "Dr. Williams", "Dr. Johnson", "Dr. Brown");

        showStep(PatientFormStep.PERSONAL_INFO);
    }

    @FXML
    private void handleNext() {
        if (!validateStep(currentStep)) return;
        showStep(currentStep.next());
    }

    @FXML
    private void handleBack() {
        showStep(currentStep.previous());
    }

    @FXML
    private void handleSubmit() {
        if (!validateStep(currentStep)) return;
        validationMessage.setText("");
        System.out.println("Patient registered: " + firstNameField.getText() + " " + lastNameField.getText());
    }

    private boolean validateStep(PatientFormStep step) {
        if (step == PatientFormStep.PERSONAL_INFO
                && (firstNameField.getText().isBlank() || lastNameField.getText().isBlank())) {
            validationMessage.setText("First and last name are required.");
            return false;
        }
        if (step == PatientFormStep.CONTACT_INFO && phoneField.getText().isBlank()) {
            validationMessage.setText("Phone number is required.");
            return false;
        }
        validationMessage.setText("");
        return true;
    }

    private void showStep(PatientFormStep step) {
        currentStep = step;

        step1Pane.setVisible(currentStep == PatientFormStep.PERSONAL_INFO);
        step1Pane.setManaged(currentStep == PatientFormStep.PERSONAL_INFO);
        step2Pane.setVisible(currentStep == PatientFormStep.CONTACT_INFO);
        step2Pane.setManaged(currentStep == PatientFormStep.CONTACT_INFO);
        step3Pane.setVisible(currentStep == PatientFormStep.MEDICAL_INFO);
        step3Pane.setManaged(currentStep == PatientFormStep.MEDICAL_INFO);

        step1Indicator.getStyleClass().remove("active");
        step2Indicator.getStyleClass().remove("active");
        step3Indicator.getStyleClass().remove("active");
        switch (currentStep) {
            case PERSONAL_INFO -> step1Indicator.getStyleClass().add("active");
            case CONTACT_INFO  -> step2Indicator.getStyleClass().add("active");
            case MEDICAL_INFO  -> step3Indicator.getStyleClass().add("active");
        }

        backBtn.setVisible(!currentStep.isFirst());
        backBtn.setManaged(!currentStep.isFirst());
        nextBtn.setVisible(!currentStep.isLast());
        nextBtn.setManaged(!currentStep.isLast());
        submitBtn.setVisible(currentStep.isLast());
        submitBtn.setManaged(currentStep.isLast());
    }
}