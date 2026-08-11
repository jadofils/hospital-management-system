package hospital.management.pages.components.doctor;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Map;

public class DoctorScheduleTableController extends PaginatedTableController<DoctorScheduleDTO> {

    @FXML private TableColumn<DoctorScheduleDTO, String> doctorNameColumn;
    @FXML private TableColumn<DoctorScheduleDTO, String> dayOfWeekColumn;
    @FXML private TableColumn<DoctorScheduleDTO, String> startTimeColumn;
    @FXML private TableColumn<DoctorScheduleDTO, String> endTimeColumn;
    @FXML private TableColumn<DoctorScheduleDTO, String> availableColumn;
    @FXML private TableColumn<DoctorScheduleDTO, Void>   actionsColumn;

    /** Resolves a doctorId to a display name for the "All Doctors" view; empty until the page sets it. */
    private Map<String, String> doctorNameById = Map.of();

    /** Called by the page once its doctor list has loaded, so the Doctor column can resolve names. */
    public void setDoctorNames(Map<String, String> doctorNameById) {
        this.doctorNameById = doctorNameById == null ? Map.of() : doctorNameById;
    }

    /** Shows/hides the Doctor column — relevant only when browsing every doctor's schedule at once. */
    public void setDoctorColumnVisible(boolean visible) {
        doctorNameColumn.setVisible(visible);
    }

    @Override
    protected void configureColumns() {
        doctorNameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(doctorNameById.getOrDefault(cell.getValue().getDoctorId(), "—")));
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
        addSortOption("Doctor", doctorNameColumn);
        addSortOption("Day", dayOfWeekColumn);
        addSortOption("Start", startTimeColumn);
        addSortOption("End", endTimeColumn);
        addSortOption("Available", availableColumn);
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(DoctorScheduleDTO schedule, String lowerQuery) {
        return schedule.getDayOfWeek() != null && schedule.getDayOfWeek().toLowerCase().contains(lowerQuery);
    }
}
