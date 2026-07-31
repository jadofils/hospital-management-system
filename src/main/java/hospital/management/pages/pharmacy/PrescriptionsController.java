package hospital.management.pages.pharmacy;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrescriptionsController extends BasePageController {

    @FXML private PrescriptionTableController prescriptionTableController;

    @FXML private TextField  searchField;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button     newPrescriptionBtn;

    private final List<Prescription> prescriptions = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PRESCRIPTIONS);

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());

        newPrescriptionBtn.setOnAction(e -> openPrescriptionDialog(null));
        prescriptionTableController.setRowActions(this::openPrescriptionDialog, this::confirmDeletePrescription);

        refreshTable();
    }

    private void applyFilter() {
        prescriptionTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        prescriptionTableController.setItems(prescriptions);
    }

    private void confirmDeletePrescription(Prescription prescription) {
        confirm("Delete Prescription",
                "Are you sure you want to delete prescription " + prescription.getPrescriptionId() + "? This cannot be undone.",
                () -> {
                    prescriptions.remove(prescription);
                    refreshTable();
                    toastSuccess("Prescription deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (prescription == null) or Update mode. */
    private void openPrescriptionDialog(Prescription prescription) {
        boolean addMode = prescription == null;

        TextField appointmentId = new TextField();
        DatePicker dateIssued = new DatePicker();

        appointmentId.getStyleClass().add("form-input");
        dateIssued.getStyleClass().add("form-date-picker");

        if (!addMode) {
            appointmentId.setText(prescription.getAppointmentId());
            dateIssued.setValue(prescription.getDateIssued());
        }

        formDialogController.open(addMode ? "New Prescription" : "Update Prescription", "fas-prescription", addMode, v -> {
            String apptId = appointmentId.getText() == null ? "" : appointmentId.getText().trim();
            if (apptId.isEmpty() || dateIssued.getValue() == null) {
                formDialogController.setError("Appointment ID and date issued are required.");
                formDialogController.setLoading(false);
                return;
            }

            Prescription target = addMode ? new Prescription() : prescription;
            if (addMode) target.setPrescriptionId(UUID.randomUUID().toString());
            target.setAppointmentId(apptId);
            target.setDateIssued(dateIssued.getValue());

            if (addMode) prescriptions.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Prescription added." : "Prescription updated.");
        });

        formDialogController.addField("Appointment Id", "fas-calendar-check", appointmentId);
        formDialogController.addField("Date Issued", "fas-calendar", dateIssued);
    }
}
