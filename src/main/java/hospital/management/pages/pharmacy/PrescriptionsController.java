package hospital.management.pages.pharmacy;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrescriptionsController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());

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

        EntityIdComboBox appointmentId = new EntityIdComboBox();
        DatePicker dateIssued = new DatePicker();

        appointmentId.getStyleClass().add("form-combo");
        dateIssued.getStyleClass().add("form-date-picker");

        try {
            appointmentId.setOptions(appointmentService.findAll(CursorPagination.firstPage(1000)).getItems().stream()
                    .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                            a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                    .toList());
        } catch (Exception ex) {
            toastError("Failed to load appointments: " + ex.getMessage());
        }

        if (!addMode) {
            appointmentId.selectById(prescription.getAppointmentId());
            dateIssued.setValue(prescription.getDateIssued());
        }

        formDialogController.open(addMode ? "New Prescription" : "Update Prescription", "fas-prescription", addMode, v -> {
            String apptId = appointmentId.getSelectedId();
            if (apptId == null || dateIssued.getValue() == null) {
                formDialogController.setError("Appointment and date issued are required.");
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

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentId);
        formDialogController.addField("Date Issued", "fas-calendar", dateIssued);
    }
}
