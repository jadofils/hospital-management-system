package hospital.management.pages.doctor;

import hospital.management.pages.BasePageController;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.department.DoctorScheduleDAOImpl;
import hospital.management.backend.dao.department.ReferralDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.doctor.CreateDoctorScheduleDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;
import hospital.management.backend.dto.doctor.DoctorSummaryDTO;
import hospital.management.backend.dto.doctor.ReferralDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.model.enums.ReferralStatus;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.clinical.interfaces.AppointmentService;
import hospital.management.backend.service.department.DoctorScheduleServiceImpl;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.department.ReferralServiceImpl;
import hospital.management.backend.service.department.interfaces.DoctorScheduleService;
import hospital.management.backend.service.department.interfaces.DoctorService;
import hospital.management.backend.service.department.interfaces.ReferralService;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.service.patient.interfaces.PatientService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.doctor.DoctorScheduleTableController;
import hospital.management.pages.components.doctor.ReferralTableController;
import hospital.management.pages.components.patient.PatientTableController;
import hospital.management.pages.components.shared.sort.SortBarController;
import hospital.management.pages.components.shared.widgets.TimeField;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScheduleController extends BasePageController {

    private final DoctorScheduleService scheduleService = new DoctorScheduleServiceImpl(new DoctorScheduleDAOImpl());
    private final DoctorService doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    private final ReferralService referralService = new ReferralServiceImpl(new ReferralDAOImpl());
    private final AppointmentService appointmentService = new AppointmentServiceImpl(
            new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final PatientService patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    private static final Map<String, String> DAY_ABBREVIATIONS = Map.of(
            "Monday", "Mon", "Tuesday", "Tue", "Wednesday", "Wed",
            "Thursday", "Thu", "Friday", "Fri", "Saturday", "Sat", "Sunday", "Sun");
    private static final Map<String, String> DAY_NAMES = Map.of(
            "Mon", "Monday", "Tue", "Tuesday", "Wed", "Wednesday",
            "Thu", "Thursday", "Fri", "Friday", "Sat", "Saturday", "Sun", "Sunday");

    @FXML private DoctorScheduleTableController scheduleTableController;
    @FXML private SortBarController sortBarController;

    @FXML private Label pageTitleLabel;
    @FXML private HBox doctorSelectorBox;
    @FXML private ComboBox<String> doctorSelector;
    @FXML private Button addSlotBtn;
    @FXML private Button continueBtn;

    // Doctor-only dashboard tabs — added to/removed from scheduleTabPane in code,
    // since Tab has no visible/managed property to gate on FXML alone.
    @FXML private TabPane scheduleTabPane;
    @FXML private Tab incomingReferralsTab;
    @FXML private Tab myPatientsTab;
    @FXML private Tab myDepartmentTab;
    @FXML private ReferralTableController incomingReferralsTableController;
    @FXML private PatientTableController myPatientsTableController;
    @FXML private Label departmentNameLabel;
    @FXML private TableView<DoctorSummaryDTO> colleagueTable;
    @FXML private TableColumn<DoctorSummaryDTO, String> colleagueNameColumn;
    @FXML private TableColumn<DoctorSummaryDTO, String> colleagueSpecializationColumn;
    @FXML private TableColumn<DoctorSummaryDTO, String> colleagueDepartmentColumn;
    @FXML private PatientTableController departmentPatientsTableController;

    private static final String ALL_DOCTORS_LABEL = "All Doctors";

    /** Non-null only when the logged-in account is linked to a doctor profile (real doctor users). */
    private String ownDoctorId;
    private final Map<String, String> doctorIdByLabel = new LinkedHashMap<>();
    private final Map<String, String> doctorNameById = new LinkedHashMap<>();

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
        setupContinueButton(continueBtn, PageRoute.MY_SCHEDULE);
        scheduleTableController.setRowActions(
            allowUpdate(PageRoute.MY_SCHEDULE, this::openScheduleDialog),
            allowDelete(PageRoute.MY_SCHEDULE, this::confirmDeleteSchedule),
            allowRead(PageRoute.MY_SCHEDULE, this::viewScheduleDetail));

        colleagueNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colleagueSpecializationColumn.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        colleagueDepartmentColumn.setCellValueFactory(new PropertyValueFactory<>("departmentName"));

        try {
            UserDTO user = userService.findById(SessionManager.getCurrentUserId());
            ownDoctorId = (user.getDoctorId() == null || user.getDoctorId().isBlank()) ? null : user.getDoctorId();
        } catch (Exception e) {
            ownDoctorId = null;
        }

        boolean actingForOthers = ownDoctorId == null;
        doctorSelectorBox.setVisible(actingForOthers);
        doctorSelectorBox.setManaged(actingForOthers);
        pageTitleLabel.setText(actingForOthers ? "Doctor Schedules" : "My Schedule");

        if (actingForOthers) {
            loadDoctorSelector();
            doctorSelector.setOnAction(e -> refreshTable());
            // These tabs only make sense when the viewer IS the doctor — an admin/receptionist
            // picking between doctors' schedules has no "mine" to show.
            scheduleTabPane.getTabs().removeAll(incomingReferralsTab, myPatientsTab, myDepartmentTab);
        } else {
            // Quick-glance tabs: no view/edit/delete — the doctor can act on a referral's
            // status here, but patient records/roster edits belong on their dedicated pages.
            incomingReferralsTableController.setOnChangeStatus(this::openIncomingReferralStatusDialog);
            myPatientsTableController.hideChangeStatusColumn();
            departmentPatientsTableController.hideChangeStatusColumn();
            loadDoctorDashboardTabs();
        }

        if (sortBarController != null) {
            sortBarController.setOnSort((field, asc) -> scheduleTableController.applySort(field, asc));
            sortBarController.addOptions(scheduleTableController.getSortOptionLabels());
        }

        refreshTable();
    }

    /** Populates the three doctor-only tabs: referrals sent to this doctor, patients this
     *  doctor has appointments with, and this doctor's department colleagues/patients. */
    private void loadDoctorDashboardTabs() {
        try {
            incomingReferralsTableController.setItems(referralService.findByReferredToDoctor(ownDoctorId));
        } catch (Exception e) {
            toastError("Failed to load incoming referrals: " + e.getMessage());
        }

        try {
            myPatientsTableController.setItems(patientsForDoctors(List.of(ownDoctorId)));
        } catch (Exception e) {
            toastError("Failed to load your patients: " + e.getMessage());
        }

        try {
            DoctorDTO ownDoctor = doctorService.findById(ownDoctorId);
            String departmentId = ownDoctor.getDepartmentId();
            if (departmentId == null || departmentId.isBlank()) {
                departmentNameLabel.setText("Not assigned to a department.");
                colleagueTable.setItems(javafx.collections.FXCollections.observableArrayList());
                departmentPatientsTableController.setItems(List.of());
                return;
            }
            departmentNameLabel.setText(entityLookupService.departmentLabel(departmentId));

            List<DoctorSummaryDTO> departmentDoctors = doctorService.findByDepartment(departmentId);
            List<DoctorSummaryDTO> colleagues = departmentDoctors.stream()
                    .filter(d -> !ownDoctorId.equals(d.getDoctorId()))
                    .toList();
            colleagueTable.setItems(javafx.collections.FXCollections.observableArrayList(colleagues));

            List<String> departmentDoctorIds = departmentDoctors.stream().map(DoctorSummaryDTO::getDoctorId).toList();
            departmentPatientsTableController.setItems(patientsForDoctors(departmentDoctorIds));
        } catch (Exception e) {
            toastError("Failed to load department info: " + e.getMessage());
        }
    }

    /** Every distinct patient with at least one appointment against any of the given doctors. */
    private List<PatientDTO> patientsForDoctors(List<String> doctorIds) throws Exception {
        Set<String> patientIds = new LinkedHashSet<>();
        for (String doctorId : doctorIds) {
            for (AppointmentDTO appointment : appointmentService.findByDoctor(doctorId)) {
                patientIds.add(appointment.getPatientId());
            }
        }
        List<PatientDTO> patients = new ArrayList<>();
        for (String patientId : patientIds) {
            try {
                patients.add(patientService.findById(patientId));
            } catch (Exception ignored) {
                // A patient looked up via a stale appointment reference — skip rather than fail the whole tab.
            }
        }
        return patients;
    }

    /** Minimal single-field dialog for changing an incoming referral's status, mirroring
     *  ReferralsController's own status dialog for the full referrals page. */
    private void openIncomingReferralStatusDialog(ReferralDTO referral) {
        ComboBox<String> status = new ComboBox<>();
        status.getStyleClass().add("form-combo");
        status.getItems().addAll("Pending", "Scheduled", "Completed");
        status.setValue(referral.getStatus() == null ? null : ReferralStatus.fromDbValue(referral.getStatus()).getLabel());

        formDialogController.open("Change Referral Status", "fas-flag", false, v -> {
            if (status.getValue() == null) {
                formDialogController.setError("Status is required.");
                formDialogController.setLoading(false);
                return;
            }
            try {
                referralService.updateStatus(referral.getReferralId(), status.getValue());
                incomingReferralsTableController.setItems(referralService.findByReferredToDoctor(ownDoctorId));
                formDialogController.close();
                toastSuccess("Referral status updated.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to update referral status: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Status", "fas-flag", status);
    }

    /** Populates the doctor picker used by non-doctor accounts (e.g. admin) to manage any doctor's availability.
     *  Defaults to "All Doctors" so the page shows every schedule instead of an empty table. */
    private void loadDoctorSelector() {
        try {
            doctorIdByLabel.clear();
            doctorNameById.clear();
            List<DoctorDTO> doctors = doctorService.findAll(CursorPagination.firstPage(500)).getItems();
            for (DoctorDTO doctor : doctors) {
                doctorIdByLabel.put(doctor.getFullName(), doctor.getDoctorId());
                doctorNameById.put(doctor.getDoctorId(), doctor.getFullName());
            }
            scheduleTableController.setDoctorNames(doctorNameById);
            doctorSelector.getItems().clear();
            doctorSelector.getItems().add(ALL_DOCTORS_LABEL);
            doctorSelector.getItems().addAll(doctorIdByLabel.keySet());
            doctorSelector.setValue(ALL_DOCTORS_LABEL);
        } catch (Exception e) {
            toastError("Failed to load doctors: " + e.getMessage());
        }
    }

    private void refreshTable() {
        try {
            schedules.clear();
            if (ownDoctorId != null) {
                schedules.addAll(scheduleService.findByDoctor(ownDoctorId));
                scheduleTableController.setDoctorColumnVisible(false);
            } else {
                String doctorId = currentDoctorId();
                if (doctorId == null) {
                    schedules.addAll(scheduleService.findAll());
                    scheduleTableController.setDoctorColumnVisible(true);
                } else {
                    schedules.addAll(scheduleService.findByDoctor(doctorId));
                    scheduleTableController.setDoctorColumnVisible(false);
                }
            }
            scheduleTableController.setItems(schedules);
        } catch (Exception e) {
            toastError("Failed to load schedules: " + e.getMessage());
        }
    }

    /** The doctor whose schedule is being viewed/edited: the logged-in doctor themselves, or the
     *  admin's current selection. Returns null when an admin is viewing "All Doctors" (or hasn't picked one yet). */
    private String currentDoctorId() {
        if (ownDoctorId != null) {
            return ownDoctorId;
        }
        String selectedLabel = doctorSelector.getValue();
        if (selectedLabel == null || ALL_DOCTORS_LABEL.equals(selectedLabel)) return null;
        return doctorIdByLabel.get(selectedLabel);
    }

    private String requireDoctorId() throws Exception {
        String doctorId = currentDoctorId();
        if (doctorId == null) {
            throw new AppException("Select a doctor first.");
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
        if (addMode && currentDoctorId() == null) {
            toastError("Select a specific doctor first.");
            return;
        }

        ComboBox<String> dayOfWeek = new ComboBox<>();
        dayOfWeek.getItems().addAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        dayOfWeek.getStyleClass().add("form-combo");

        TimeField startTime = new TimeField();
        TimeField endTime = new TimeField();

        ComboBox<String> available = new ComboBox<>();
        available.getItems().addAll("Yes", "No");
        available.getStyleClass().add("form-combo");

        if (!addMode) {
            dayOfWeek.setValue(DAY_NAMES.getOrDefault(schedule.getDayOfWeek(), schedule.getDayOfWeek()));
            startTime.setTime(schedule.getStartTime());
            endTime.setTime(schedule.getEndTime());
            available.setValue(Boolean.TRUE.equals(schedule.getIsAvailable()) ? "Yes" : "No");
        }

        formDialogController.open(addMode ? "Add Availability" : "Update Availability", "fas-calendar-alt", addMode, v -> {
            String day = dayOfWeek.getValue();
            String availableValue = available.getValue();

            if (day == null || availableValue == null) {
                formDialogController.setError("Day of week and availability are required.");
                formDialogController.setLoading(false);
                return;
            }

            LocalTime start = startTime.getTime();
            LocalTime end = endTime.getTime();

            if (!end.isAfter(start)) {
                formDialogController.setError("End time must be after start time.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                String doctorId = addMode ? requireDoctorId() : schedule.getDoctorId();
                CreateDoctorScheduleDTO dto = new CreateDoctorScheduleDTO(
                        doctorId, DAY_ABBREVIATIONS.getOrDefault(day, day), start, end, "Yes".equals(availableValue));
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

