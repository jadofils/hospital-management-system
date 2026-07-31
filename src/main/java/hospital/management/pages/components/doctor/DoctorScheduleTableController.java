package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.doctor.DoctorSchedule;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class DoctorScheduleTableController extends PaginatedTableController<DoctorSchedule> {

    @FXML private TableColumn<DoctorSchedule, String> dayOfWeekColumn;
    @FXML private TableColumn<DoctorSchedule, String> startTimeColumn;
    @FXML private TableColumn<DoctorSchedule, String> endTimeColumn;
    @FXML private TableColumn<DoctorSchedule, String> availableColumn;
    @FXML private TableColumn<DoctorSchedule, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        dayOfWeekColumn.setCellValueFactory(new PropertyValueFactory<>("dayOfWeek"));
        // LocalTime#toString() renders as "HH:mm" (ISO_LOCAL_TIME), which is exactly the format
        // the Add/Edit dialog's start/end time TextFields parse back on submit.
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        // The `isAvailable` field is a Boolean whose real getter is isIsAvailable() (not
        // getIsAvailable()/isAvailable()), which PropertyValueFactory cannot resolve reliably for
        // that name. Call the getter directly and expose a plain Yes/No string instead.
        availableColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(Boolean.TRUE.equals(cell.getValue().isIsAvailable()) ? "Yes" : "No"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(DoctorSchedule schedule, String lowerQuery) {
        return schedule.getDayOfWeek() != null && schedule.getDayOfWeek().toLowerCase().contains(lowerQuery);
    }
}
