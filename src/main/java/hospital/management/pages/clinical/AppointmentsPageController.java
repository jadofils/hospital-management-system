package hospital.management.pages.clinical;

import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.department.DoctorScheduleDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.clinical.CreateAppointmentDTO;
import hospital.management.backend.dto.clinical.UpdateAppointmentDTO;
import hospital.management.backend.dao.finance.InvoiceDAOImpl;
import hospital.management.backend.dto.finance.InvoiceDTO;
import hospital.management.backend.service.finance.InvoiceServiceImpl;
import hospital.management.backend.service.finance.interfaces.InvoiceService;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.model.enums.AppointmentStatus;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.clinical.interfaces.AppointmentService;
import hospital.management.backend.service.department.DoctorScheduleServiceImpl;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.department.interfaces.DoctorScheduleService;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.clinical.AppointmentTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import hospital.management.pages.components.shared.sort.SortBarController;
import hospital.management.pages.components.shared.widgets.CalendarController;
import hospital.management.pages.components.shared.widgets.TimeField;
import hospital.management.pages.utils.CsvUiIO;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AppointmentsPageController extends BasePageController implements QuickAddCapable {

    private static final Map<DayOfWeek, String> DAY_ABBREVIATIONS = Map.of(
            DayOfWeek.MONDAY, "Mon", DayOfWeek.TUESDAY, "Tue", DayOfWeek.WEDNESDAY, "Wed",
            DayOfWeek.THURSDAY, "Thu", DayOfWeek.FRIDAY, "Fri", DayOfWeek.SATURDAY, "Sat", DayOfWeek.SUNDAY, "Sun");

    private final AppointmentService appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final DoctorServiceImpl doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final DoctorScheduleService scheduleService = new DoctorScheduleServiceImpl(new DoctorScheduleDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();
    private final InvoiceService invoiceService =
        new InvoiceServiceImpl(new InvoiceDAOImpl(), new PatientDAOImpl(), new AppointmentDAOImpl());

    @FXML private CalendarController calendarController;
    @FXML private AppointmentTableController appointmentTableController;
    @FXML private SortBarController sortBarController;

    @FXML private Button addAppointmentBtn;
    @FXML private Button importBtn;
    @FXML private Button exportBtn;
    @FXML private Button continueBtn;
    @FXML private ComboBox<String> billingFilter;

    private final List<AppointmentDTO> appointments = new ArrayList<>();
    private LocalDate selectedDate;

    /** Populated when the Add/Edit Appointment dialog opens; used to re-filter the doctor
     *  dropdown whenever the appointment date changes. */
    private List<DoctorDTO> dialogDoctors = List.of();
    private Map<String, List<DoctorScheduleDTO>> dialogSchedulesByDoctor = Map.of();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.APPOINTMENTS);

        applyCreateVisibility(addAppointmentBtn, PageRoute.APPOINTMENTS);
        applyCreateVisibility(importBtn, PageRoute.APPOINTMENTS);
        boolean canExport = canRead(PageRoute.APPOINTMENTS);
        exportBtn.setVisible(canExport);
        exportBtn.setManaged(canExport);

        addAppointmentBtn.setOnAction(e -> openAppointmentDialog(null));
        importBtn.setOnAction(e -> withSpinner(importBtn, this::importAppointments));
        exportBtn.setOnAction(e -> withSpinner(exportBtn, this::exportAppointments));
        setupContinueButton(continueBtn, PageRoute.APPOINTMENTS);
        appointmentTableController.setRowActions(
            allowUpdate(PageRoute.APPOINTMENTS, this::openAppointmentDialog),
            allowDelete(PageRoute.APPOINTMENTS, this::confirmDeleteAppointment),
            allowRead(PageRoute.APPOINTMENTS, this::viewAppointmentDetail));
        appointmentTableController.setOnChangeStatus(canUpdate(PageRoute.APPOINTMENTS) ? this::openAppointmentStatusDialog : null);

        if (sortBarController != null) {
            sortBarController.setOnSort((field, asc) -> appointmentTableController.applySort(field, asc));
            sortBarController.addOptions(appointmentTableController.getSortOptionLabels());
        }

        if (billingFilter != null) {
            billingFilter.getItems().setAll(FILTER_ALL, FILTER_NEEDS_BILLING, FILTER_PAID);
            billingFilter.setValue(FILTER_ALL);
            billingFilter.setOnAction(e -> applyFilter());
        }

        if (calendarController != null) {
            calendarController.setOnDateSelected(this::loadAppointmentsForDate);
        }

        refreshTable();
    }

    private void loadAppointmentsForDate(LocalDate date) {
        selectedDate = date;
        applyFilter();
    }

    private void refreshTable() {
        try {
            appointments.clear();
            List<AppointmentSummaryDTO> summaries =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO summary : summaries) {
                AppointmentDTO appointment = appointmentService.findById(summary.getAppointmentId());
                try {
                    appointment.setBillingStatus(invoiceService.findByAppointment(appointment.getAppointmentId())
                            .map(InvoiceDTO::getPaymentStatus).orElse(null));
                } catch (Exception ex) {
                    appointment.setBillingStatus(null);
                }
                appointments.add(appointment);
            }
            selectedDate = null;
            applyFilter();
        } catch (Exception e) {
            toastError("Failed to load appointments: " + e.getMessage());
        }
    }

    private void applyFilter() {
        List<AppointmentDTO> visible = appointments.stream()
                .filter(a -> selectedDate == null
                        || (a.getAppointmentDate() != null
                            && a.getAppointmentDate().toLocalDate().equals(selectedDate)))
                .filter(this::matchesBillingFilter)
                .toList();
        appointmentTableController.setItems(visible);
    }

    /** Applies the "All / Paid / Needs billing" dropdown. "Needs billing" = a completed
     *  appointment whose invoice is missing or not fully paid. */
    private boolean matchesBillingFilter(AppointmentDTO a) {
        String choice = billingFilter == null ? FILTER_ALL : billingFilter.getValue();
        if (choice == null || FILTER_ALL.equals(choice)) return true;
        if (FILTER_PAID.equals(choice)) return "paid".equalsIgnoreCase(a.getBillingStatus());
        boolean completed = AppointmentStatus.COMPLETED.getDbValue().equalsIgnoreCase(a.getStatus());
        return completed && !"paid".equalsIgnoreCase(a.getBillingStatus());
    }

    private static final String FILTER_ALL = "All";
    private static final String FILTER_PAID = "Paid";
    private static final String FILTER_NEEDS_BILLING = "Needs billing";

    private void exportAppointments() {
        try {
            if (appointments.isEmpty()) {
                toastError("No appointments available to export.");
                return;
            }

            List<AppointmentDTO> source = chooseAppointmentExportSource();
            if (source.isEmpty()) {
                return;
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (AppointmentDTO appointment : source) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("appointment_id", appointment.getAppointmentId());
                row.put("patient_id", appointment.getPatientId());
                row.put("doctor_id", appointment.getDoctorId());
                row.put("appointment_date", appointment.getAppointmentDate());
                row.put("status", appointment.getStatus());
                row.put("reason", appointment.getReason());
                rows.add(row);
            }

            boolean saved = CsvUiIO.exportRows(exportBtn.getScene().getWindow(), "appointments.csv", rows);
            if (saved) {
                toastSuccess("Appointments exported successfully.");
            }
        } catch (Exception e) {
            toastError("Failed to export appointments: " + e.getMessage());
        }
    }

    private List<AppointmentDTO> chooseAppointmentExportSource() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Current table view", "Current table view", "All loaded rows");
        dialog.setTitle("Export Appointments");
        dialog.setHeaderText("Choose what to export");
        dialog.setContentText("Export scope:");
        String choice = dialog.showAndWait().orElse(null);
        if (choice == null) {
            return List.of();
        }
        if ("Current table view".equals(choice)) {
            return new ArrayList<>(appointmentTableController.getTable().getItems());
        }
        return appointments;
    }

    private void importAppointments() {
        try {
            List<Map<String, String>> rows = CsvUiIO.importRows(importBtn.getScene().getWindow(), "Import Appointments");
            if (rows.isEmpty()) {
                return;
            }

            int ok = 0;
            int failed = 0;
            for (Map<String, String> row : rows) {
                try {
                    String patientId = value(row, "patient_id");
                    String doctorId = value(row, "doctor_id");
                    LocalDateTime dateTime = LocalDateTime.parse(value(row, "appointment_date", "appointment_datetime"));
                    String reason = value(row, "reason");
                    appointmentService.book(new CreateAppointmentDTO(patientId, doctorId, dateTime, reason));
                    ok++;
                } catch (Exception ex) {
                    failed++;
                }
            }

            refreshTable();
            if (failed == 0) {
                toastSuccess("Imported " + ok + " appointment(s).");
            } else {
                toastError("Imported " + ok + " appointment(s), failed " + failed + ".");
            }
        } catch (Exception e) {
            toastError("Failed to import appointments: " + e.getMessage());
        }
    }

    private String value(Map<String, String> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key).trim();
            }
        }
        return "";
    }

    private void viewAppointmentDetail(AppointmentDTO appointment) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Patient", entityLookupService.patientLabel(appointment.getPatientId()));
            fields.put("Doctor", entityLookupService.doctorLabel(appointment.getDoctorId()));
        } catch (Exception ex) {
            toastError("Failed to resolve appointment details: " + ex.getMessage());
        }
        fields.put("Date/Time", appointment.getAppointmentDate() == null ? null : appointment.getAppointmentDate().toString());
        fields.put("Status", statusLabel(appointment.getStatus()));
        fields.put("Reason", appointment.getReason());
        detailViewController.show("Appointment Details", "fas-calendar-check", fields);
    }

    private void confirmDeleteAppointment(AppointmentDTO appointment) {
        confirm("Cancel Appointment",
                "Are you sure you want to cancel this appointment? This cannot be undone.",
                () -> {
                    try {
                        appointmentService.cancel(appointment.getAppointmentId());
                        refreshTable();
                        toastSuccess("Appointment cancelled.");
                    } catch (Exception e) {
                        toastError("Failed to cancel appointment: " + e.getMessage());
                    }
                });
    }

    @Override
    public void openAddDialog() {
        openAppointmentDialog(null);
    }

    /** Opens the shared form dialog in Add mode (appointment == null) or Update mode. */
    private void openAppointmentDialog(AppointmentDTO appointment) {
        boolean addMode = appointment == null;

        LoadingIdComboBox patientIdField = new LoadingIdComboBox();
        LoadingIdComboBox doctorIdField  = new LoadingIdComboBox();
        EntityIdComboBox patientId = patientIdField.getComboBox();
        EntityIdComboBox doctorId  = doctorIdField.getComboBox();
        DatePicker appointmentDate = new DatePicker();
        TimeField appointmentTime  = new TimeField();
        TextField reason = new TextField();

        reason.setPromptText("e.g. Follow-up checkup, chest pain (optional)");
        reason.getStyleClass().add("form-input");
        List.of(patientId, doctorId).forEach(f -> f.getStyleClass().add("form-combo"));
        appointmentDate.getStyleClass().add("form-date-picker");

        // Real-time: date must not be in the past for new appointments
        if (addMode) {
            FxFormValidator.attachNotPastDate(appointmentDate, null, "Appointment date");
            FxFormValidator.disallowPastDates(appointmentDate);
        }
        FxFormValidator.attachMaxLength(reason, null, 500, "Reason");

        List<Node> otherFields = List.of(appointmentDate, appointmentTime, reason);
        otherFields.forEach(f -> f.setDisable(true));

        appointmentDate.valueProperty().addListener((obs, oldDate, newDate) -> filterDialogDoctors(doctorId, newDate));
        if (addMode) {
            doctorId.valueProperty().addListener((obs, oldDoctor, newDoctor) ->
                autofillFromDoctorAvailability(doctorId.getSelectedId(), appointmentDate, appointmentTime));
        }

        if (!addMode) {
            LocalDateTime existing = appointment.getAppointmentDate();
            if (existing != null) {
                appointmentDate.setValue(existing.toLocalDate());
                appointmentTime.setTime(existing.toLocalTime());
            }
            reason.setText(appointment.getReason());
        }

        formDialogController.open(addMode ? "Add Appointment" : "Update Appointment", "fas-calendar-check", addMode, v -> {
            String pId = patientId.getSelectedId();
            String dId = doctorId.getSelectedId();

            if (pId == null) {
                formDialogController.setError("Patient is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (dId == null) {
                formDialogController.setError("Doctor is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (appointmentDate.getValue() == null) {
                formDialogController.setError("Appointment date is required.");
                FxFormValidator.applyStyle(appointmentDate, false);
                formDialogController.setLoading(false);
                return;
            }
            if (addMode && appointmentDate.getValue().isBefore(java.time.LocalDate.now())) {
                formDialogController.setError("Appointment date must not be in the past.");
                FxFormValidator.applyStyle(appointmentDate, false);
                formDialogController.setLoading(false);
                return;
            }

            LocalTime time = appointmentTime.getTime();

            try {
                LocalDateTime dateTime = LocalDateTime.of(appointmentDate.getValue(), time);
                if (addMode) {
                    appointmentService.book(new CreateAppointmentDTO(pId, dId, dateTime, reason.getText()));
                } else {
                    appointmentService.update(new UpdateAppointmentDTO(
                            appointment.getAppointmentId(), dateTime, appointment.getStatus(), reason.getText()));
                }
                refreshTable();
                formDialogController.close();
                toastSuccess(addMode ? "Appointment added." : "Appointment updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save appointment: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addRequiredField("Patient", "fas-user-injured", patientIdField);
        formDialogController.addRequiredField("Doctor", "fas-user-md", doctorIdField);
        formDialogController.addRequiredField("Appointment Date", "fas-calendar", appointmentDate);
        formDialogController.addRequiredField("Appointment Time", "fas-clock", appointmentTime);
        formDialogController.addField("Reason", "fas-notes-medical", reason);

        loadAppointmentDropdowns(patientIdField, doctorIdField, otherFields, addMode ? null : appointment);
    }

    /** Narrows the doctor dropdown to doctors available on the given date's day-of-week; doctors
     *  with no schedule configured at all remain selectable on any date. Preserves the current
     *  selection when it's still valid, clears it otherwise. */
    private void filterDialogDoctors(EntityIdComboBox doctorId, LocalDate date) {
        String previouslySelected = doctorId.getSelectedId();
        List<DoctorDTO> filtered;
        if (date == null) {
            filtered = dialogDoctors;
        } else {
            String dayAbbrev = DAY_ABBREVIATIONS.get(date.getDayOfWeek());
            Set<String> availableForDay = new HashSet<>();
            for (Map.Entry<String, List<DoctorScheduleDTO>> entry : dialogSchedulesByDoctor.entrySet()) {
                boolean available = entry.getValue().stream()
                        .anyMatch(s -> dayAbbrev.equals(s.getDayOfWeek()) && Boolean.TRUE.equals(s.getIsAvailable()));
                if (available) availableForDay.add(entry.getKey());
            }
            filtered = dialogDoctors.stream()
                    .filter(d -> !dialogSchedulesByDoctor.containsKey(d.getDoctorId()) || availableForDay.contains(d.getDoctorId()))
                    .toList();
        }
        doctorId.setOptions(filtered.stream()
                .map(d -> new EntityIdComboBox.Option(d.getDoctorId(), d.getFullName())).toList());
        if (previouslySelected != null) doctorId.selectById(previouslySelected);
    }

    private void autofillFromDoctorAvailability(String doctorId, DatePicker appointmentDate, TimeField appointmentTime) {
        if (doctorId == null) return;
        List<DoctorScheduleDTO> schedules = dialogSchedulesByDoctor.getOrDefault(doctorId, List.of());
        Optional<SuggestedSlot> slot = suggestNextSlot(schedules);
        if (slot.isEmpty()) return;
        appointmentDate.setValue(slot.get().date());
        appointmentTime.setTime(slot.get().time());
    }

    private Optional<SuggestedSlot> suggestNextSlot(List<DoctorScheduleDTO> schedules) {
        if (schedules == null || schedules.isEmpty()) return Optional.empty();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 14; i++) {
            LocalDate candidateDate = today.plusDays(i);
            String dayAbbrev = DAY_ABBREVIATIONS.get(candidateDate.getDayOfWeek());
            LocalTime earliestStart = schedules.stream()
                    .filter(s -> dayAbbrev.equals(s.getDayOfWeek()))
                    .filter(s -> Boolean.TRUE.equals(s.getIsAvailable()))
                    .map(DoctorScheduleDTO::getStartTime)
                    .filter(t -> t != null)
                    .min(LocalTime::compareTo)
                    .orElse(null);
            if (earliestStart != null) {
                return Optional.of(new SuggestedSlot(candidateDate, earliestStart));
            }
        }
        return Optional.empty();
    }

    private record SuggestedSlot(LocalDate date, LocalTime time) {}

    /** Loads the patient/doctor dropdown options asynchronously, showing each dropdown's own
     *  spinner while its data is in flight and keeping the rest of the form disabled until
     *  both have finished loading. */
    private void loadAppointmentDropdowns(LoadingIdComboBox patientIdField, LoadingIdComboBox doctorIdField,
                                           List<Node> otherFields, AppointmentDTO existing) {
        EntityIdComboBox patientId = patientIdField.getComboBox();
        EntityIdComboBox doctorId = doctorIdField.getComboBox();

        patientIdField.setLoading(true);
        doctorIdField.setLoading(true);
        formDialogController.setLoading(true);

        AtomicInteger pending = new AtomicInteger(3);
        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

        AsyncJobRunner.submit(
            () -> patientService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                patientId.setOptions(items.stream()
                        .map(p -> new EntityIdComboBox.Option(p.getPatientId(), p.getFullName())).toList());
                if (existing != null) patientId.selectById(existing.getPatientId());
                patientIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                patientIdField.setLoading(false);
                toastError("Failed to load patients: " + ex.getMessage());
                onOneLoaded.run();
            });

        AsyncJobRunner.submit(
            () -> doctorService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                dialogDoctors = items;
                doctorId.setOptions(items.stream()
                        .map(d -> new EntityIdComboBox.Option(d.getDoctorId(), d.getFullName())).toList());
                if (existing != null) doctorId.selectById(existing.getDoctorId());
                doctorIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                doctorIdField.setLoading(false);
                toastError("Failed to load doctors: " + ex.getMessage());
                onOneLoaded.run();
            });

        AsyncJobRunner.submit(
            scheduleService::findAll,
            items -> {
                Map<String, List<DoctorScheduleDTO>> byDoctor = new LinkedHashMap<>();
                for (DoctorScheduleDTO schedule : items) {
                    byDoctor.computeIfAbsent(schedule.getDoctorId(), k -> new ArrayList<>()).add(schedule);
                }
                dialogSchedulesByDoctor = byDoctor;
                onOneLoaded.run();
            },
            ex -> {
                toastError("Failed to load doctor schedules: " + ex.getMessage());
                onOneLoaded.run();
            });
    }

    /** Minimal single-field dialog for changing an existing appointment's status, kept out of the main Add/Edit form. */
    private void openAppointmentStatusDialog(AppointmentDTO appointment) {
        ComboBox<String> status = new ComboBox<>();
        status.getStyleClass().add("form-combo");
        for (AppointmentStatus s : AppointmentStatus.values()) {
            status.getItems().add(s.getLabel());
        }
        status.setValue(statusLabel(appointment.getStatus()));

        formDialogController.open("Change Status", "fas-info-circle", false, v -> {
            if (status.getValue() == null) {
                formDialogController.setError("Status is required.");
                formDialogController.setLoading(false);
                return;
            }
            try {
                String dbValue = AppointmentStatus.fromDbValue(status.getValue()).getDbValue();
                appointmentService.update(new UpdateAppointmentDTO(
                        appointment.getAppointmentId(), null, dbValue, null));
                refreshTable();
                formDialogController.close();
                toastSuccess("Appointment status updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to update status: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Status", "fas-info-circle", status);
    }

    private static String statusLabel(String status) {
        try {
            return AppointmentStatus.fromDbValue(status).getLabel();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }
}
