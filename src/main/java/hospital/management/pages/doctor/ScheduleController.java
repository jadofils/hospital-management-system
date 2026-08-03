package hospital.management.pages.doctor;

import hospital.management.pages.BasePageController;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.department.DoctorScheduleDAOImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dto.doctor.CreateDoctorScheduleDTO;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.service.department.DoctorScheduleServiceImpl;
import hospital.management.backend.service.department.interfaces.DoctorScheduleService;
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

public class ScheduleController extends BasePageController {

    private final DoctorScheduleService scheduleService = new DoctorScheduleServiceImpl(new DoctorScheduleDAOImpl());
    private final UserService userService = new UserServiceImpl(new UserDAOImpl());

    private static final Map<String, String> DAY_ABBREVIATIONS = Map.of(
            "Monday", "Mon", "Tuesday", "Tue", "Wednesday", "Wed",
            "Thursday", "Thu", "Friday", "Fri", "Saturday", "Sat", "Sunday", "Sun");
    private static final Map<String, String> DAY_NAMES = Map.of(
            "Mon", "Monday", "Tue", "Tuesday", "Wed", "Wednesday",
            "Thu", "Thursday", "Fri", "Friday", "Sat", "Saturday", "Sun", "Sunday");

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

    private final List<DoctorScheduleDTO> schedules = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.MY_SCHEDULE);

        applyCreateVisibility(addSlotBtn, PageRoute.MY_SCHEDULE);
        addSlotBtn.setOnAction(e -> openScheduleDialog(null));
        scheduleTableController.setRowActions(
            allowUpdate(PageRoute.MY_SCHEDULE, this::openScheduleDialog),
            allowDelete(PageRoute.MY_SCHEDULE, this::confirmDeleteSchedule),
            allowRead(PageRoute.MY_SCHEDULE, this::viewScheduleDetail));

        refreshTable();
    }

    private void refreshTable() {
        try {
            schedules.clear();
            schedules.addAll(scheduleService.findByDoctor(currentDoctorId()));
            scheduleTableController.setItems(schedules);
        } catch (Exception e) {
            toastError("Failed to load schedules: " + e.getMessage());
        }
    }

    private String currentDoctorId() throws Exception {
        UserDTO user = userService.findById(SessionManager.getCurrentUserId());
        String doctorId = user.getDoctorId();
        if (doctorId == null || doctorId.isBlank()) {
            throw new AppException("Your account is not linked to a doctor profile.");
        }
        return doctorId;
    }

    private void viewScheduleDetail(DoctorScheduleDTO schedule) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Day of Week", schedule.getDayOfWeek());
        fields.put("Start Time", schedule.getStartTime() == null ? null : schedule.getStartTime().toString());
        fields.put("End Time", schedule.getEndTime() == null ? null : schedule.getEndTime().toString());
        fields.put("Available", Boolean.TRUE.equals(schedule.getIsAvailable()) ? "Yes" : "No");
        detailViewController.show("Schedule Slot Details", "fas-calendar-alt", fields);
    }

    private void confirmDeleteSchedule(DoctorScheduleDTO schedule) {
        confirm("Delete Schedule Slot",
                "Are you sure you want to delete the " + schedule.getDayOfWeek() + " slot? This cannot be undone.",
                () -> {
                    try {
                        scheduleService.delete(schedule.getScheduleId());
                        refreshTable();
                        toastSuccess("Schedule slot deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete schedule slot: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog in Add mode (schedule == null) or Update mode. */
    private void openScheduleDialog(DoctorScheduleDTO schedule) {
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
            dayOfWeek.setValue(DAY_NAMES.getOrDefault(schedule.getDayOfWeek(), schedule.getDayOfWeek()));
            startTime.setText(schedule.getStartTime() == null ? "" : schedule.getStartTime().toString());
            endTime.setText(schedule.getEndTime() == null ? "" : schedule.getEndTime().toString());
            available.setValue(Boolean.TRUE.equals(schedule.getIsAvailable()) ? "Yes" : "No");
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

            try {
                CreateDoctorScheduleDTO dto = new CreateDoctorScheduleDTO(
                        currentDoctorId(), DAY_ABBREVIATIONS.getOrDefault(day, day), start, end, "Yes".equals(availableValue));
                if (addMode) {
                    scheduleService.create(dto);
                } else {
                    scheduleService.update(schedule.getScheduleId(), dto);
                }
                refreshTable();
                formDialogController.close();
                toastSuccess(addMode ? "Schedule slot added." : "Schedule slot updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save schedule slot: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Day of Week", "fas-calendar-day", dayOfWeek);
        formDialogController.addField("Start Time", "fas-clock", startTime);
        formDialogController.addField("End Time", "fas-clock", endTime);
        formDialogController.addField("Available", "fas-check-circle", available);
    }
}
