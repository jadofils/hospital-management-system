package hospital.management.pages.doctor;

import hospital.management.backend.utils.FxFormValidator;
import hospital.management.pages.BasePageController;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.department.ReferralDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.doctor.CreateReferralDTO;
import hospital.management.backend.dto.doctor.ReferralDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.enums.ReferralStatus;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.department.ReferralServiceImpl;
import hospital.management.backend.service.department.interfaces.ReferralService;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.doctor.ReferralTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import hospital.management.pages.components.shared.sort.SortBarController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ReferralsController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final DoctorServiceImpl doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final ReferralService referralService = new ReferralServiceImpl(new ReferralDAOImpl());
    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private ReferralTableController referralTableController;
    @FXML private SortBarController sortBarController;

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> directionFilter;
    @FXML private Button       newReferralBtn;
    @FXML private Button       continueBtn;

    private final List<ReferralDTO> referrals = new ArrayList<>();

    /** Non-null only when the logged-in account is linked to a doctor profile — "Sent"/"Received"
     *  only has meaning relative to a specific doctor, so the filter is hidden for admin viewers. */
    private String ownDoctorId;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.REFERRALS);

        try {
            UserDTO user = userService.findById(SessionManager.getCurrentUserId());
            ownDoctorId = (user.getDoctorId() == null || user.getDoctorId().isBlank()) ? null : user.getDoctorId();
        } catch (Exception e) {
            ownDoctorId = null;
        }

        statusFilter.getItems().addAll("All", "PENDING", "SCHEDULED", "COMPLETED");
        statusFilter.setValue("All");
        directionFilter.getItems().addAll("All", "Sent", "Received");
        directionFilter.setValue("All");
        directionFilter.setVisible(ownDoctorId != null);
        directionFilter.setManaged(ownDoctorId != null);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());
        directionFilter.setOnAction(e -> applyFilter());

        applyCreateVisibility(newReferralBtn, PageRoute.REFERRALS);
        newReferralBtn.setOnAction(e -> openReferralDialog(null));
        setupContinueButton(continueBtn, PageRoute.REFERRALS);
        referralTableController.setRowActions(
            allowUpdate(PageRoute.REFERRALS, this::openReferralDialog),
            allowDelete(PageRoute.REFERRALS, this::confirmDeleteReferral),
            allowRead(PageRoute.REFERRALS, this::viewReferralDetail));
        referralTableController.setOnChangeStatus(canUpdate(PageRoute.REFERRALS) ? this::openReferralStatusDialog : null);

        if (sortBarController != null) {
            sortBarController.setOnSort((field, asc) -> referralTableController.applySort(field, asc));
            sortBarController.addOptions(referralTableController.getSortOptionLabels());
        }

        refreshTable();
    }

    /** Combines the free-text search with the status and (doctor-only) direction dropdowns —
     *  status/direction narrow the underlying item set, then the shared search box further
     *  filters whatever's currently visible. */
    private void applyFilter() {
        String status = statusFilter.getValue();
        boolean statusAll = status == null || "All".equals(status);

        String direction = directionFilter.getValue();
        boolean directionAll = ownDoctorId == null || direction == null || "All".equals(direction);

        List<ReferralDTO> visible = referrals.stream()
                .filter(r -> statusAll || status.equalsIgnoreCase(r.getStatus()))
                .filter(r -> directionAll
                        || ("Sent".equals(direction) && ownDoctorId.equals(r.getReferringDoctorId()))
                        || ("Received".equals(direction) && ownDoctorId.equals(r.getReferredToDoctorId())))
                .toList();

        referralTableController.setItems(visible);
        referralTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            referrals.clear();
            List<AppointmentSummaryDTO> appointments =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO appointment : appointments) {
                try {
                    referrals.addAll(referralService.findByAppointment(appointment.getAppointmentId()));
                } catch (ResourceNotFoundException ignored) {
                }
            }
            applyFilter();
        } catch (Exception e) {
            toastError("Failed to load referrals: " + e.getMessage());
        }
    }

    private void viewReferralDetail(ReferralDTO referral) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Appointment", entityLookupService.appointmentLabel(referral.getAppointmentId()));
            fields.put("Referring Doctor", entityLookupService.doctorLabel(referral.getReferringDoctorId()));
            fields.put("Referred-To Doctor", entityLookupService.doctorLabel(referral.getReferredToDoctorId()));
        } catch (Exception ex) {
            toastError("Failed to resolve referral details: " + ex.getMessage());
        }
        fields.put("Reason", referral.getReason());
        fields.put("Status", referral.getStatus());
        fields.put("Created At", referral.getCreatedAt() == null ? null : referral.getCreatedAt().toString());
        detailViewController.show("Referral Details", "fas-exchange-alt", fields);
    }

    private void confirmDeleteReferral(ReferralDTO referral) {
        confirm("Delete Referral",
                "Are you sure you want to delete referral " + referral.getReferralId() + "? This cannot be undone.",
                () -> {
                    try {
                        referralService.delete(referral.getReferralId());
                        refreshTable();
                        toastSuccess("Referral deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete referral: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog in Add mode (referral == null) or Update mode. */
    private void openReferralDialog(ReferralDTO referral) {
        boolean addMode = referral == null;

        LoadingIdComboBox appointmentIdField      = new LoadingIdComboBox();
        LoadingIdComboBox referringDoctorIdField  = new LoadingIdComboBox();
        LoadingIdComboBox referredToDoctorIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId       = appointmentIdField.getComboBox();
        EntityIdComboBox referringDoctorId   = referringDoctorIdField.getComboBox();
        EntityIdComboBox referredToDoctorId  = referredToDoctorIdField.getComboBox();
        TextField reason              = new TextField();

        reason.setPromptText("e.g. Specialist consultation for chronic headaches");
        reason.getStyleClass().add("form-input");
        List.of(appointmentId, referringDoctorId, referredToDoctorId).forEach(f -> f.getStyleClass().add("form-combo"));

        FxFormValidator.attachRequired(reason, null, "Reason");
        FxFormValidator.attachMaxLength(reason, null, 500, "Reason");

        List<Control> otherFields = List.of(reason);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            reason.setText(referral.getReason());
            FxFormValidator.applyStyle(reason, referral.getReason() != null && !referral.getReason().isBlank());
        }

        formDialogController.open(addMode ? "Add Referral" : "Update Referral", "fas-exchange-alt", addMode, v -> {
            String appt = appointmentId.getSelectedId();
            String fromDoc = referringDoctorId.getSelectedId();
            String toDoc = referredToDoctorId.getSelectedId();
            String reasonText = reason.getText() == null ? "" : reason.getText().trim();
            if (appt == null) {
                formDialogController.setError("Appointment is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (fromDoc == null) {
                formDialogController.setError("Referring doctor is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (toDoc == null) {
                formDialogController.setError("Referred-to doctor is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (reasonText.isEmpty()) {
                formDialogController.setError("Reason is required.");
                FxFormValidator.applyStyle(reason, false);
                formDialogController.setLoading(false);
                return;
            }
            if (fromDoc.equals(toDoc)) {
                formDialogController.setError("Referring doctor and referred-to doctor must differ.");
                formDialogController.setLoading(false);
                return;
            }

            if (!addMode) {
                formDialogController.setError("Referral details cannot be edited after creation; use the status action on the row to update it.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                referralService.create(new CreateReferralDTO(appt, fromDoc, toDoc, reasonText));
                refreshTable();
                formDialogController.close();
                toastSuccess("Referral added.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to save referral: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Referring Doctor", "fas-user-md", referringDoctorIdField);
        formDialogController.addField("Referred-To Doctor", "fas-user-md", referredToDoctorIdField);
        formDialogController.addField("Reason", "fas-notes-medical", reason);

        loadReferralDropdowns(appointmentIdField, referringDoctorIdField, referredToDoctorIdField,
                otherFields, addMode ? null : referral);
    }

    /** Loads the appointment/doctor dropdown options asynchronously, showing each dropdown's own
     *  spinner while its data is in flight and keeping the rest of the form disabled until
     *  both the appointment list and the (shared) doctor list have finished loading. */
    private void loadReferralDropdowns(LoadingIdComboBox appointmentIdField, LoadingIdComboBox referringDoctorIdField,
                                        LoadingIdComboBox referredToDoctorIdField, List<Control> otherFields, ReferralDTO existing) {
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox referringDoctorId = referringDoctorIdField.getComboBox();
        EntityIdComboBox referredToDoctorId = referredToDoctorIdField.getComboBox();

        appointmentIdField.setLoading(true);
        referringDoctorIdField.setLoading(true);
        referredToDoctorIdField.setLoading(true);
        formDialogController.setLoading(true);

        // Map each appointment to the doctor who holds it, so selecting an
        // appointment auto-fills the referring doctor with the appointment's doctor.
        Map<String, String> appointmentDoctorIds = new LinkedHashMap<>();
        List<EntityIdComboBox.Option> allDoctorOptions = new ArrayList<>();

        // Applies the appointment-driven doctor rules:
        //  - referring doctor = the appointment's doctor (read-only),
        //  - referred-to doctor list excludes the referring doctor (no self-referral).
        Runnable applyAppointmentDoctor = () -> {
            String selectedAppointmentId = appointmentId.getSelectedId();
            if (selectedAppointmentId == null) return;
            String referringDocId = appointmentDoctorIds.get(selectedAppointmentId);
            if (referringDocId == null) return;

            referringDoctorId.selectById(referringDocId);
            referringDoctorId.setEditable(false);

            List<EntityIdComboBox.Option> eligible = allDoctorOptions.stream()
                    .filter(o -> !referringDocId.equals(o.id()))
                    .toList();
            referredToDoctorId.setOptions(eligible);
            if (referringDocId.equals(referredToDoctorId.getSelectedId())) {
                referredToDoctorId.setValue(null);
            }
        };

        AtomicInteger pending = new AtomicInteger(2);
        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

        AsyncJobRunner.submit(
            () -> appointmentService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                List<EntityIdComboBox.Option> options = items.stream()
                        .map(a -> {
                            appointmentDoctorIds.put(a.getAppointmentId(), a.getDoctorId());
                            return new EntityIdComboBox.Option(a.getAppointmentId(),
                                    a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate());
                        })
                        .toList();
                appointmentId.setOptions(options);
                appointmentId.valueProperty().addListener((obs, oldVal, newVal) -> applyAppointmentDoctor.run());
                if (existing != null) appointmentId.selectById(existing.getAppointmentId());
                appointmentIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                appointmentIdField.setLoading(false);
                toastError("Failed to load appointments: " + ex.getMessage());
                onOneLoaded.run();
            });

        AsyncJobRunner.submit(
            () -> doctorService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                allDoctorOptions.clear();
                items.stream()
                        .map(d -> new EntityIdComboBox.Option(d.getDoctorId(), d.getFullName()))
                        .forEach(allDoctorOptions::add);
                referringDoctorId.setOptions(allDoctorOptions);
                referringDoctorIdField.setLoading(false);
                referredToDoctorIdField.setLoading(false);
                applyAppointmentDoctor.run();
                if (existing != null) {
                    referredToDoctorId.selectById(existing.getReferredToDoctorId());
                }
                onOneLoaded.run();
            },
            ex -> {
                referringDoctorIdField.setLoading(false);
                referredToDoctorIdField.setLoading(false);
                toastError("Failed to load doctors: " + ex.getMessage());
                onOneLoaded.run();
            });
    }

    /** Minimal single-field dialog for changing an existing referral's status, kept out of the main Add/Edit form. */
    private void openReferralStatusDialog(ReferralDTO referral) {
        ComboBox<String> status = new ComboBox<>();
        status.getStyleClass().add("form-combo");
        status.getItems().addAll("Pending", "Scheduled", "Completed");
        status.setValue(referral.getStatus() == null ? null : ReferralStatus.fromDbValue(referral.getStatus()).getLabel());

        formDialogController.open("Change Status", "fas-flag", false, v -> {
            if (status.getValue() == null) {
                formDialogController.setError("Status is required.");
                formDialogController.setLoading(false);
                return;
            }
            try {
                referralService.updateStatus(referral.getReferralId(), status.getValue());
                refreshTable();
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
}
