package hospital.management.pages.components.shared.search;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.function.Consumer;

public class AdvancedSearchController {

    /** Everything a page hosting this component might want to filter by. */
    public record Criteria(String patientId, String doctorName, LocalDate appointmentDate, String status) {}

    @FXML private TextField patientIdField;
    @FXML private TextField doctorNameField;
    @FXML private DatePicker appointmentDatePicker;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button resetBtn;
    @FXML private Button searchBtn;

    private Consumer<Criteria> onSearch;
    private Runnable onReset;

    public void initialize() {
        statusFilter.getItems().addAll("All", "Confirmed", "Pending", "Cancelled");
        statusFilter.setValue("All");

        resetBtn.setOnAction(e -> resetFields());
        searchBtn.setOnAction(e -> performSearch());
    }

    /** The hosting page supplies how to actually apply the criteria (and show its own feedback). */
    public void setOnSearch(Consumer<Criteria> onSearch) {
        this.onSearch = onSearch;
    }

    /** Optional: notified after fields are cleared, in case the host needs to reset its own filter state too. */
    public void setOnReset(Runnable onReset) {
        this.onReset = onReset;
    }

    private void resetFields() {
        patientIdField.clear();
        doctorNameField.clear();
        appointmentDatePicker.setValue(null);
        statusFilter.setValue("All");
        if (onReset != null) onReset.run();
    }

    private void performSearch() {
        if (onSearch != null) {
            onSearch.accept(new Criteria(
                patientIdField.getText(),
                doctorNameField.getText(),
                appointmentDatePicker.getValue(),
                statusFilter.getValue()));
        }
    }
}
