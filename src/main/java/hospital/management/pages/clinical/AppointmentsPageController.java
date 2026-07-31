package hospital.management.pages.clinical;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.clinical.AppointmentTableController;
import hospital.management.pages.components.shared.widgets.CalendarController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppointmentsPageController extends BasePageController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private CalendarController calendarController;
    @FXML private AppointmentTableController appointmentTableController;

    @FXML private Button addAppointmentBtn;

    private final List<Appointment> appointments = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.APPOINTMENTS);

        addAppointmentBtn.setOnAction(e -> openAppointmentDialog(null));
        appointmentTableController.setRowActions(this::openAppointmentDialog, this::confirmDeleteAppointment);

        if (calendarController != null) {
            calendarController.setOnDateSelected(this::loadAppointmentsForDate);
        }

        refreshTable();
    }

    private void loadAppointmentsForDate(LocalDate date) {
        // Backend service layer is stubbed; date-based filtering will be wired
        // once appointment lookups are backed by a real service.
        refreshTable();
    }

    private void refreshTable() {
        appointmentTableController.setItems(appointments);
    }

    private void confirmDeleteAppointment(Appointment appointment) {
        confirm("Delete Appointment",
                "Are you sure you want to delete this appointment? This cannot be undone.",
                () -> {
                    appointments.remove(appointment);
                    refreshTable();
                    toastSuccess("Appointment deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (appointment == null) or Update mode. */
    private void openAppointmentDialog(Appointment appointment) {
        boolean addMode = appointment == null;

        TextField patientId = new TextField();
        TextField doctorId  = new TextField();
        DatePicker appointmentDate = new DatePicker();
        TextField appointmentTime  = new TextField();
        appointmentTime.setPromptText("HH:mm");
        ComboBox<String> status = new ComboBox<>();
        TextField reason = new TextField();

        List.of(patientId, doctorId, appointmentTime, reason).forEach(f -> f.getStyleClass().add("form-input"));
        appointmentDate.getStyleClass().add("form-date-picker");
        status.getStyleClass().add("form-combo");
        status.getItems().addAll("Scheduled", "Completed", "Cancelled", "No-show");

        if (!addMode) {
            patientId.setText(appointment.getPatientId());
            doctorId.setText(appointment.getDoctorId());
            LocalDateTime existing = appointment.getAppointmentDate();
            if (existing != null) {
                appointmentDate.setValue(existing.toLocalDate());
                appointmentTime.setText(existing.toLocalTime().format(TIME_FMT));
            }
            status.setValue(appointment.getStatus());
            reason.setText(appointment.getReason());
        }

        formDialogController.open(addMode ? "Add Appointment" : "Update Appointment", "fas-calendar-check", addMode, v -> {
            String pId = patientId.getText() == null ? "" : patientId.getText().trim();
            String dId = doctorId.getText() == null ? "" : doctorId.getText().trim();
            String timeText = appointmentTime.getText() == null ? "" : appointmentTime.getText().trim();

            if (pId.isEmpty() || dId.isEmpty() || appointmentDate.getValue() == null
                    || timeText.isEmpty() || status.getValue() == null) {
                formDialogController.setError("Patient ID, doctor ID, date, time and status are required.");
                formDialogController.setLoading(false);
                return;
            }

            LocalTime time;
            try {
                time = LocalTime.parse(timeText, TIME_FMT);
            } catch (DateTimeParseException ex) {
                formDialogController.setError("Time must be in HH:mm format.");
                formDialogController.setLoading(false);
                return;
            }

            Appointment target = addMode ? new Appointment() : appointment;
            if (addMode) target.setAppointmentId(UUID.randomUUID().toString());
            target.setPatientId(pId);
            target.setDoctorId(dId);
            target.setAppointmentDate(LocalDateTime.of(appointmentDate.getValue(), time));
            target.setStatus(status.getValue());
            target.setReason(reason.getText());

            if (addMode) appointments.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Appointment added." : "Appointment updated.");
        });

        formDialogController.addField("Patient Id", "fas-user-injured", patientId);
        formDialogController.addField("Doctor Id", "fas-user-md", doctorId);
        formDialogController.addField("Appointment Date", "fas-calendar", appointmentDate);
        formDialogController.addField("Appointment Time", "fas-clock", appointmentTime);
        formDialogController.addField("Status", "fas-info-circle", status);
        formDialogController.addField("Reason", "fas-notes-medical", reason);
    }
}
