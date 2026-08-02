package hospital.management.pages.doctor;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.doctor.DoctorSchedule;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.doctor.DoctorScheduleTableController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScheduleController extends BasePageController {

    @FXML private DoctorScheduleTableController scheduleTableController;

    @FXML private Button addSlotBtn;

    // Day columns for weekly grid — layout/wiring intentionally left untouched.
    @FXML private VBox monCol;
    @FXML private VBox tueCol;
    @FXML private VBox wedCol;
    @FXML private VBox thuCol;
    @FXML private VBox friCol;
    @FXML private VBox satCol;
    @FXML private VBox sunCol;

    private final List<DoctorSchedule> schedules = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.MY_SCHEDULE);

        addSlotBtn.setOnAction(e -> openScheduleDialog(null));
        scheduleTableController.setRowActions(this::openScheduleDialog, this::confirmDeleteSchedule, this::viewScheduleDetail);

        refreshTable();
    }

    private void refreshTable() {
        scheduleTableController.setItems(schedules);
    }

    private void viewScheduleDetail(DoctorSchedule schedule) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Day of Week", schedule.getDayOfWeek());
        fields.put("Start Time", schedule.getStartTime() == null ? null : schedule.getStartTime().toString());
        fields.put("End Time", schedule.getEndTime() == null ? null : schedule.getEndTime().toString());
        fields.put("Available", Boolean.TRUE.equals(schedule.isIsAvailable()) ? "Yes" : "No");
        detailViewController.show("Schedule Slot Details", "fas-calendar-alt", fields);
    }

    private void confirmDeleteSchedule(DoctorSchedule schedule) {
        confirm("Delete Schedule Slot",
                "Are you sure you want to delete the " + schedule.getDayOfWeek() + " slot? This cannot be undone.",
                () -> {
                    schedules.remove(schedule);
                    refreshTable();
                    toastSuccess("Schedule slot deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (schedule == null) or Update mode. */
    private void openScheduleDialog(DoctorSchedule schedule) {
        boolean addMode = schedule == null;

        ComboBox<String> dayOfWeek = new ComboBox<>();
        dayOfWeek.getItems().addAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        dayOfWeek.getStyleClass().add("form-combo");

        TextField startTime = new TextField();
        startTime.setPromptText("HH:mm, e.g. 09:00");
        TextField endTime = new TextField();
        endTime.setPromptText("HH:mm, e.g. 17:00");
        List.of(startTime, endTime).forEach(f -> f.getStyleClass().add("form-input"));

        ComboBox<String> available = new ComboBox<>();
        available.getItems().addAll("Yes", "No");
        available.getStyleClass().add("form-combo");

        if (!addMode) {
            dayOfWeek.setValue(schedule.getDayOfWeek());
            startTime.setText(schedule.getStartTime() == null ? "" : schedule.getStartTime().toString());
            endTime.setText(schedule.getEndTime() == null ? "" : schedule.getEndTime().toString());
            available.setValue(Boolean.TRUE.equals(schedule.isIsAvailable()) ? "Yes" : "No");
        }

        formDialogController.open(addMode ? "Add Availability" : "Update Availability", "fas-calendar-alt", addMode, v -> {
            String day = dayOfWeek.getValue();
            String startText = startTime.getText() == null ? "" : startTime.getText().trim();
            String endText = endTime.getText() == null ? "" : endTime.getText().trim();
            String availableValue = available.getValue();

            if (day == null || startText.isEmpty() || endText.isEmpty() || availableValue == null) {
                formDialogController.setError("Day of week, start time, end time and availability are required.");
                formDialogController.setLoading(false);
                return;
            }

            LocalTime start;
            LocalTime end;
            try {
                start = LocalTime.parse(startText);
                end = LocalTime.parse(endText);
            } catch (DateTimeParseException ex) {
                formDialogController.setError("Start/End time must be in HH:mm format, e.g. 09:00.");
                formDialogController.setLoading(false);
                return;
            }

            if (!end.isAfter(start)) {
                formDialogController.setError("End time must be after start time.");
                formDialogController.setLoading(false);
                return;
            }

            DoctorSchedule target = addMode ? new DoctorSchedule() : schedule;
            if (addMode) target.setScheduleId(UUID.randomUUID().toString());
            target.setDayOfWeek(day);
            target.setStartTime(start);
            target.setEndTime(end);
            target.setIsAvailable("Yes".equals(availableValue));

            if (addMode) schedules.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Schedule slot added." : "Schedule slot updated.");
        });

        formDialogController.addField("Day of Week", "fas-calendar-day", dayOfWeek);
        formDialogController.addField("Start Time", "fas-clock", startTime);
        formDialogController.addField("End Time", "fas-clock", endTime);
        formDialogController.addField("Available", "fas-check-circle", available);
    }
}
