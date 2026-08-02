package hospital.management.pages.doctor;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.doctor.Referral;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.doctor.ReferralTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ReferralsController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final DoctorServiceImpl doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private ReferralTableController referralTableController;

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> directionFilter;
    @FXML private Button       newReferralBtn;

    private final List<Referral> referrals = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.REFERRALS);

        statusFilter.getItems().addAll("All", "PENDING", "SCHEDULED", "COMPLETED");
        statusFilter.setValue("All");
        directionFilter.getItems().addAll("All", "Sent", "Received");
        directionFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        newReferralBtn.setOnAction(e -> openReferralDialog(null));
        referralTableController.setRowActions(this::openReferralDialog, this::confirmDeleteReferral, this::viewReferralDetail);
        referralTableController.setOnChangeStatus(this::openReferralStatusDialog);

        refreshTable();
    }

    private void applyFilter() {
        referralTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        referralTableController.setItems(referrals);
    }

    private void viewReferralDetail(Referral referral) {
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

    private void confirmDeleteReferral(Referral referral) {
        confirm("Delete Referral",
                "Are you sure you want to delete referral " + referral.getReferralId() + "? This cannot be undone.",
                () -> {
                    referrals.remove(referral);
                    refreshTable();
                    toastSuccess("Referral deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (referral == null) or Update mode. */
    private void openReferralDialog(Referral referral) {
        boolean addMode = referral == null;

        LoadingIdComboBox appointmentIdField      = new LoadingIdComboBox();
        LoadingIdComboBox referringDoctorIdField  = new LoadingIdComboBox();
        LoadingIdComboBox referredToDoctorIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId       = appointmentIdField.getComboBox();
        EntityIdComboBox referringDoctorId   = referringDoctorIdField.getComboBox();
        EntityIdComboBox referredToDoctorId  = referredToDoctorIdField.getComboBox();
        TextField reason              = new TextField();

        reason.getStyleClass().add("form-input");
        List.of(appointmentId, referringDoctorId, referredToDoctorId).forEach(f -> f.getStyleClass().add("form-combo"));

        List<Control> otherFields = List.of(reason);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
            reason.setText(referral.getReason());
        }

        formDialogController.open(addMode ? "Add Referral" : "Update Referral", "fas-exchange-alt", addMode, v -> {
            String appt = appointmentId.getSelectedId();
            String fromDoc = referringDoctorId.getSelectedId();
            String toDoc = referredToDoctorId.getSelectedId();
            String reasonText = reason.getText() == null ? "" : reason.getText().trim();
            if (appt == null || fromDoc == null || toDoc == null || reasonText.isEmpty()) {
                formDialogController.setError("Appointment, referring doctor, referred-to doctor and reason are required.");
                formDialogController.setLoading(false);
                return;
            }
            if (fromDoc.equals(toDoc)) {
                formDialogController.setError("Referring doctor and referred-to doctor must differ.");
                formDialogController.setLoading(false);
                return;
            }

            Referral target = addMode ? new Referral() : referral;
            if (addMode) {
                target.setReferralId(UUID.randomUUID().toString());
                target.setCreatedAt(LocalDateTime.now());
                target.setStatus("Pending");
            } else {
                target.setUpdatedAt(LocalDateTime.now());
            }
            target.setAppointmentId(appt);
            target.setReferringDoctorId(fromDoc);
            target.setReferredToDoctorId(toDoc);
            target.setReason(reasonText);

            if (addMode) referrals.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Referral added." : "Referral updated.");
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
                                        LoadingIdComboBox referredToDoctorIdField, List<Control> otherFields, Referral existing) {
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox referringDoctorId = referringDoctorIdField.getComboBox();
        EntityIdComboBox referredToDoctorId = referredToDoctorIdField.getComboBox();

        appointmentIdField.setLoading(true);
        referringDoctorIdField.setLoading(true);
        referredToDoctorIdField.setLoading(true);
        formDialogController.setLoading(true);

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
                appointmentId.setOptions(items.stream()
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
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
                List<EntityIdComboBox.Option> doctorOptions = items.stream()
                        .map(d -> new EntityIdComboBox.Option(d.getDoctorId(), d.getFullName())).toList();
                referringDoctorId.setOptions(doctorOptions);
                referredToDoctorId.setOptions(doctorOptions);
                if (existing != null) {
                    referringDoctorId.selectById(existing.getReferringDoctorId());
                    referredToDoctorId.selectById(existing.getReferredToDoctorId());
                }
                referringDoctorIdField.setLoading(false);
                referredToDoctorIdField.setLoading(false);
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
    private void openReferralStatusDialog(Referral referral) {
        ComboBox<String> status = new ComboBox<>();
        status.getStyleClass().add("form-combo");
        status.getItems().addAll("Pending", "Accepted", "Completed", "Declined");
        status.setValue(referral.getStatus());

        formDialogController.open("Change Status", "fas-flag", false, v -> {
            if (status.getValue() == null) {
                formDialogController.setError("Status is required.");
                formDialogController.setLoading(false);
                return;
            }
            referral.setStatus(status.getValue());
            referral.setUpdatedAt(LocalDateTime.now());
            refreshTable();
            formDialogController.close();
            toastSuccess("Referral status updated.");
        });

        formDialogController.addField("Status", "fas-flag", status);
    }
}
