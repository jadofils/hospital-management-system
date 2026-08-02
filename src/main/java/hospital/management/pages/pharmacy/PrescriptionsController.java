package hospital.management.pages.pharmacy;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.pharmacy.Prescription;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.pharmacy.PrescriptionTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PrescriptionsController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

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
        prescriptionTableController.setRowActions(this::openPrescriptionDialog, this::confirmDeletePrescription, this::viewPrescriptionDetail);

        refreshTable();
    }

    private void applyFilter() {
        prescriptionTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        prescriptionTableController.setItems(prescriptions);
    }

    private void viewPrescriptionDetail(Prescription prescription) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Appointment", entityLookupService.appointmentLabel(prescription.getAppointmentId()));
        } catch (Exception ex) {
            toastError("Failed to resolve appointment: " + ex.getMessage());
        }
        fields.put("Date Issued", prescription.getDateIssued() == null ? null : prescription.getDateIssued().toString());
        detailViewController.show("Prescription Details", "fas-prescription", fields);
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

        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        DatePicker dateIssued = new DatePicker();

        appointmentId.getStyleClass().add("form-combo");
        dateIssued.getStyleClass().add("form-date-picker");

        List<Control> otherFields = List.of(dateIssued);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
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

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Date Issued", "fas-calendar", dateIssued);

        loadPrescriptionDropdown(appointmentIdField, otherFields, addMode ? null : prescription);
    }

    /** Loads the appointment dropdown options asynchronously, showing its own spinner while
     *  data is in flight and keeping the rest of the form disabled until it finishes loading. */
    private void loadPrescriptionDropdown(LoadingIdComboBox appointmentIdField, List<Control> otherFields, Prescription existing) {
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();

        appointmentIdField.setLoading(true);
        formDialogController.setLoading(true);

        AsyncJobRunner.submit(
            () -> appointmentService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                appointmentId.setOptions(items.stream()
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
                if (existing != null) appointmentId.selectById(existing.getAppointmentId());
                appointmentIdField.setLoading(false);
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            },
            ex -> {
                appointmentIdField.setLoading(false);
                toastError("Failed to load appointments: " + ex.getMessage());
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            });
    }
}
