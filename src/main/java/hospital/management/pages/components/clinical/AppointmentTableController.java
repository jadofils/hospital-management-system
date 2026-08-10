package hospital.management.pages.components.clinical;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class AppointmentTableController extends PaginatedTableController<AppointmentDTO> {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<AppointmentDTO, String> patientIdColumn;
    @FXML private TableColumn<AppointmentDTO, String> doctorIdColumn;
    @FXML private TableColumn<AppointmentDTO, String> dateColumn;
    @FXML private TableColumn<AppointmentDTO, String> statusColumn;
    @FXML private TableColumn<AppointmentDTO, Void>   changeStatusColumn;
    @FXML private TableColumn<AppointmentDTO, String> reasonColumn;
    @FXML private TableColumn<AppointmentDTO, Void>   actionsColumn;

    private Consumer<AppointmentDTO> onChangeStatus;

    /** Registers the row-level "change status" callback used by the changeStatusColumn button. */
    public void setOnChangeStatus(Consumer<AppointmentDTO> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
    protected void configureColumns() {
        patientIdColumn.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        doctorIdColumn.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        dateColumn.setCellValueFactory(cell -> {
            var date = cell.getValue().getAppointmentDate();
            return new SimpleStringProperty(date != null ? date.format(DISPLAY_FMT) : "");
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        wireSingleActionColumn(changeStatusColumn, "fas-flag",
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(AppointmentDTO appointment, String lowerQuery) {
        String dateStr = appointment.getAppointmentDate() != null
            ? appointment.getAppointmentDate().format(DISPLAY_FMT) : "";
        return safe(appointment.getPatientId()).contains(lowerQuery)
            || safe(appointment.getDoctorId()).contains(lowerQuery)
            || safe(appointment.getStatus()).contains(lowerQuery)
            || safe(appointment.getReason()).contains(lowerQuery)
            || dateStr.contains(lowerQuery);
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(); }
}
