package hospital.management.pages.components;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class PatientFormController {

    @FXML private Label step1Indicator;
    @FXML private Label step2Indicator;
    @FXML private Label step3Indicator;

    @FXML private VBox step1Pane;
    @FXML private VBox step2Pane;
    @FXML private VBox step3Pane;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<String> genderCombo;

    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;

    @FXML private ComboBox<String> bloodGroupCombo;
    @FXML private ComboBox<String> assignedDoctorCombo;
    @FXML private TextArea allergiesArea;

    @FXML private Label validationMessage;
    @FXML private Button backBtn;
    @FXML private Button nextBtn;
    @FXML private Button submitBtn;

    private int currentStep = 1;

    public void initialize() {
        genderCombo.getItems().addAll("Male", "Female", "Other");
        bloodGroupCombo.getItems().addAll("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        assignedDoctorCombo.getItems().addAll("Dr. Smith", "Dr. Williams", "Dr. Johnson", "Dr. Brown");
        showStep(1);
    }

    @FXML
    private void handleNext() {
        if (!validateStep(currentStep)) return;
        showStep(currentStep + 1);
    }

    @FXML
    private void handleBack() {
        showStep(currentStep - 1);
    }

    @FXML
    private void handleSubmit() {
        if (!validateStep(currentStep)) return;
        validationMessage.setText("");
        System.out.println("Patient registered: " + firstNameField.getText() + " " + lastNameField.getText());
    }

    private boolean validateStep(int step) {
        if (step == 1 && (firstNameField.getText().isBlank() || lastNameField.getText().isBlank())) {
            validationMessage.setText("First and last name are required.");
            return false;
        }
        if (step == 2 && phoneField.getText().isBlank()) {
            validationMessage.setText("Phone number is required.");
            return false;
        }
        validationMessage.setText("");
        return true;
    }

    private void showStep(int step) {
        currentStep = Math.max(1, Math.min(3, step));

        step1Pane.setVisible(currentStep == 1);
        step1Pane.setManaged(currentStep == 1);
        step2Pane.setVisible(currentStep == 2);
        step2Pane.setManaged(currentStep == 2);
        step3Pane.setVisible(currentStep == 3);
        step3Pane.setManaged(currentStep == 3);

        step1Indicator.getStyleClass().removeAll("active");
        step2Indicator.getStyleClass().removeAll("active");
        step3Indicator.getStyleClass().removeAll("active");
        switch (currentStep) {
            case 1 -> step1Indicator.getStyleClass().add("active");
            case 2 -> step2Indicator.getStyleClass().add("active");
            case 3 -> step3Indicator.getStyleClass().add("active");
        }

        backBtn.setVisible(currentStep > 1);
        backBtn.setManaged(currentStep > 1);
        nextBtn.setVisible(currentStep < 3);
        nextBtn.setManaged(currentStep < 3);
        submitBtn.setVisible(currentStep == 3);
        submitBtn.setManaged(currentStep == 3);
    }
}
