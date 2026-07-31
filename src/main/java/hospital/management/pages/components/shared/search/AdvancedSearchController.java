package hospital.management.pages.components.shared.search;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class AdvancedSearchController {

    @FXML private TextField patientIdField;
    @FXML private TextField doctorNameField;
    @FXML private DatePicker appointmentDatePicker;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button resetBtn;
    @FXML private Button searchBtn;

    public void initialize() {
        statusFilter.getItems().addAll("All", "Confirmed", "Pending", "Cancelled");
        statusFilter.setValue("All");

        resetBtn.setOnAction(e -> resetFields());
        searchBtn.setOnAction(e -> performSearch());
    }

    private void resetFields() {
        patientIdField.clear();
        doctorNameField.clear();
        appointmentDatePicker.setValue(null);
        statusFilter.setValue("All");
    }

    private void performSearch() {
        System.out.println("Advanced search: patientId=" + patientIdField.getText()
                + ", doctor=" + doctorNameField.getText()
                + ", date=" + appointmentDatePicker.getValue()
                + ", status=" + statusFilter.getValue());
        // TODO: integrate with patient/appointment DB or table filtering
    }
}
