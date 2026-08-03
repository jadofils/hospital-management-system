package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class DoctorScheduleTableController extends PaginatedTableController<DoctorScheduleDTO> {

    @FXML private TableColumn<DoctorScheduleDTO, String> dayOfWeekColumn;
    @FXML private TableColumn<DoctorScheduleDTO, String> startTimeColumn;
    @FXML private TableColumn<DoctorScheduleDTO, String> endTimeColumn;
    @FXML private TableColumn<DoctorScheduleDTO, String> availableColumn;
    @FXML private TableColumn<DoctorScheduleDTO, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        dayOfWeekColumn.setCellValueFactory(new PropertyValueFactory<>("dayOfWeek"));
        // LocalTime#toString() renders as "HH:mm" (ISO_LOCAL_TIME), which is exactly the format
        // the Add/Edit dialog's start/end time TextFields parse back on submit.
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        // The `isAvailable` field is a Boolean whose real getter is getIsAvailable() (not
        // isAvailable()), which PropertyValueFactory cannot resolve reliably for that
        // name. Call the getter directly and expose a plain Yes/No string instead.
        availableColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(Boolean.TRUE.equals(cell.getValue().getIsAvailable()) ? "Yes" : "No"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(DoctorScheduleDTO schedule, String lowerQuery) {
        return schedule.getDayOfWeek() != null && schedule.getDayOfWeek().toLowerCase().contains(lowerQuery);
    }
}
