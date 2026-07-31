package hospital.management.pages.components.clinical;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.patient.Appointment;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;

public class AppointmentTableController extends PaginatedTableController<Appointment> {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<Appointment, String> patientIdColumn;
    @FXML private TableColumn<Appointment, String> doctorIdColumn;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;
    @FXML private TableColumn<Appointment, String> reasonColumn;
    @FXML private TableColumn<Appointment, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        patientIdColumn.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        doctorIdColumn.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        dateColumn.setCellValueFactory(cell -> {
            var date = cell.getValue().getAppointmentDate();
            return new SimpleStringProperty(date != null ? date.format(DISPLAY_FMT) : "");
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(Appointment appointment, String lowerQuery) {
        return containsIgnoreCase(appointment.getPatientId(), lowerQuery)
                || containsIgnoreCase(appointment.getDoctorId(), lowerQuery)
                || containsIgnoreCase(appointment.getStatus(), lowerQuery)
                || containsIgnoreCase(appointment.getReason(), lowerQuery);
    }

    private boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }
}
