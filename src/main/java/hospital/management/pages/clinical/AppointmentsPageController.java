package hospital.management.pages.clinical;

import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.clinical.AppointmentTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.widgets.CalendarController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AppointmentsPageController extends BasePageController implements QuickAddCapable {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final DoctorServiceImpl doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private CalendarController calendarController;
    @FXML private AppointmentTableController appointmentTableController;

    @FXML private Button addAppointmentBtn;

    private final List<Appointment> appointments = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.APPOINTMENTS);

        addAppointmentBtn.setOnAction(e -> openAppointmentDialog(null));
        appointmentTableController.setRowActions(this::openAppointmentDialog, this::confirmDeleteAppointment, this::viewAppointmentDetail);
        appointmentTableController.setOnChangeStatus(this::openAppointmentStatusDialog);

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

    private void viewAppointmentDetail(Appointment appointment) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Patient", entityLookupService.patientLabel(appointment.getPatientId()));
            fields.put("Doctor", entityLookupService.doctorLabel(appointment.getDoctorId()));
        } catch (Exception ex) {
            toastError("Failed to resolve appointment details: " + ex.getMessage());
        }
        fields.put("Date/Time", appointment.getAppointmentDate() == null ? null : appointment.getAppointmentDate().toString());
        fields.put("Status", appointment.getStatus());
        fields.put("Reason", appointment.getReason());
        detailViewController.show("Appointment Details", "fas-calendar-check", fields);
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

    @Override
    public void openAddDialog() {
        openAppointmentDialog(null);
    }

    /** Opens the shared form dialog in Add mode (appointment == null) or Update mode. */
    private void openAppointmentDialog(Appointment appointment) {
        boolean addMode = appointment == null;

        EntityIdComboBox patientId = new EntityIdComboBox();
        EntityIdComboBox doctorId  = new EntityIdComboBox();
        DatePicker appointmentDate = new DatePicker();
        TextField appointmentTime  = new TextField();
        appointmentTime.setPromptText("HH:mm");
        TextField reason = new TextField();

        List.of(appointmentTime, reason).forEach(f -> f.getStyleClass().add("form-input"));
        List.of(patientId, doctorId).forEach(f -> f.getStyleClass().add("form-combo"));
        appointmentDate.getStyleClass().add("form-date-picker");

        try {
            patientId.setOptions(patientService.findAll(CursorPagination.firstPage(1000)).getItems().stream()
                    .map(p -> new EntityIdComboBox.Option(p.getPatientId(), p.getFullName())).toList());
            doctorId.setOptions(doctorService.findAll(CursorPagination.firstPage(1000)).getItems().stream()
                    .map(d -> new EntityIdComboBox.Option(d.getDoctorId(), d.getFullName())).toList());
        } catch (Exception ex) {
            toastError("Failed to load patients/doctors: " + ex.getMessage());
        }

        if (!addMode) {
            patientId.selectById(appointment.getPatientId());
            doctorId.selectById(appointment.getDoctorId());
            LocalDateTime existing = appointment.getAppointmentDate();
            if (existing != null) {
                appointmentDate.setValue(existing.toLocalDate());
                appointmentTime.setText(existing.toLocalTime().format(TIME_FMT));
            }
            reason.setText(appointment.getReason());
        }

        formDialogController.open(addMode ? "Add Appointment" : "Update Appointment", "fas-calendar-check", addMode, v -> {
            String pId = patientId.getSelectedId();
            String dId = doctorId.getSelectedId();
            String timeText = appointmentTime.getText() == null ? "" : appointmentTime.getText().trim();

            if (pId == null || dId == null || appointmentDate.getValue() == null || timeText.isEmpty()) {
                formDialogController.setError("Patient, doctor, date and time are required.");
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
            if (addMode) {
                target.setAppointmentId(UUID.randomUUID().toString());
                target.setStatus("Scheduled");
            }
            target.setPatientId(pId);
            target.setDoctorId(dId);
            target.setAppointmentDate(LocalDateTime.of(appointmentDate.getValue(), time));
            target.setReason(reason.getText());

            if (addMode) appointments.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Appointment added." : "Appointment updated.");
        });

        formDialogController.addField("Patient", "fas-user-injured", patientId);
        formDialogController.addField("Doctor", "fas-user-md", doctorId);
        formDialogController.addField("Appointment Date", "fas-calendar", appointmentDate);
        formDialogController.addField("Appointment Time", "fas-clock", appointmentTime);
        formDialogController.addField("Reason", "fas-notes-medical", reason);
    }

    /** Minimal single-field dialog for changing an existing appointment's status, kept out of the main Add/Edit form. */
    private void openAppointmentStatusDialog(Appointment appointment) {
        ComboBox<String> status = new ComboBox<>();
        status.getStyleClass().add("form-combo");
        status.getItems().addAll("Scheduled", "Completed", "Cancelled", "No-show");
        status.setValue(appointment.getStatus());

        formDialogController.open("Change Status", "fas-info-circle", false, v -> {
            if (status.getValue() == null) {
                formDialogController.setError("Status is required.");
                formDialogController.setLoading(false);
                return;
            }
            appointment.setStatus(status.getValue());
            refreshTable();
            formDialogController.close();
            toastSuccess("Appointment status updated.");
        });

        formDialogController.addField("Status", "fas-info-circle", status);
    }
}
