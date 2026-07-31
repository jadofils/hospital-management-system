package hospital.management.pages.doctor;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.doctor.Referral;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.doctor.ReferralTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReferralsController extends BasePageController {

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
        referralTableController.setRowActions(this::openReferralDialog, this::confirmDeleteReferral);

        refreshTable();
    }

    private void applyFilter() {
        referralTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        referralTableController.setItems(referrals);
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

        TextField appointmentId       = new TextField();
        TextField referringDoctorId   = new TextField();
        TextField referredToDoctorId  = new TextField();
        TextField reason              = new TextField();
        ComboBox<String> status       = new ComboBox<>();

        List.of(appointmentId, referringDoctorId, referredToDoctorId, reason)
                .forEach(f -> f.getStyleClass().add("form-input"));
        status.getStyleClass().add("form-combo");
        status.getItems().addAll("Pending", "Accepted", "Completed", "Declined");

        if (!addMode) {
            appointmentId.setText(referral.getAppointmentId());
            referringDoctorId.setText(referral.getReferringDoctorId());
            referredToDoctorId.setText(referral.getReferredToDoctorId());
            reason.setText(referral.getReason());
            status.setValue(referral.getStatus());
        }

        formDialogController.open(addMode ? "Add Referral" : "Update Referral", "fas-exchange-alt", addMode, v -> {
            String appt = appointmentId.getText() == null ? "" : appointmentId.getText().trim();
            String fromDoc = referringDoctorId.getText() == null ? "" : referringDoctorId.getText().trim();
            String toDoc = referredToDoctorId.getText() == null ? "" : referredToDoctorId.getText().trim();
            String reasonText = reason.getText() == null ? "" : reason.getText().trim();
            if (appt.isEmpty() || fromDoc.isEmpty() || toDoc.isEmpty() || reasonText.isEmpty() || status.getValue() == null) {
                formDialogController.setError("Appointment, referring doctor, referred-to doctor, reason and status are required.");
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
            } else {
                target.setUpdatedAt(LocalDateTime.now());
            }
            target.setAppointmentId(appt);
            target.setReferringDoctorId(fromDoc);
            target.setReferredToDoctorId(toDoc);
            target.setReason(reasonText);
            target.setStatus(status.getValue());

            if (addMode) referrals.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Referral added." : "Referral updated.");
        });

        formDialogController.addField("Appointment Id", "fas-calendar-check", appointmentId);
        formDialogController.addField("Referring Doctor Id", "fas-user-md", referringDoctorId);
        formDialogController.addField("Referred-To Doctor Id", "fas-user-md", referredToDoctorId);
        formDialogController.addField("Reason", "fas-notes-medical", reason);
        formDialogController.addField("Status", "fas-flag", status);
    }
}
