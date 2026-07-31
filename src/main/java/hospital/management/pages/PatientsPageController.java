package hospital.management.pages;

import hospital.management.pages.components.SidebarController;
import hospital.management.pages.components.PatientTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class PatientsPageController {

    @FXML private SidebarController sidebarController;
    @FXML private PatientTableController patientTableController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button addPatientBtn;
    @FXML private Label totalLabel;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem("patients");

        statusFilter.getItems().addAll("All", "Admitted", "Discharged", "Pending", "Cancelled");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        addPatientBtn.setOnAction(e -> System.out.println("Open Add Patient form"));
        totalLabel.setText("Total: 7 patients");
    }

    private void applyFilter() {
        if (patientTableController != null) {
            patientTableController.filter(searchField.getText());
        }
    }
}