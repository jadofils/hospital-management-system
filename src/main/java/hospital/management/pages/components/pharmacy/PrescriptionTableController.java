package hospital.management.pages.components.pharmacy;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import hospital.management.backend.service.lookup.EntityLookupService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class PrescriptionTableController extends PaginatedTableController<PrescriptionDTO> {

    private final EntityLookupService lookupService = new EntityLookupService();

    @FXML private TableColumn<PrescriptionDTO, String> prescriptionIdColumn;
    @FXML private TableColumn<PrescriptionDTO, String> appointmentIdColumn;
    @FXML private TableColumn<PrescriptionDTO, java.time.LocalDate> dateIssuedColumn;
    @FXML private TableColumn<PrescriptionDTO, String> statusColumn;
    @FXML private TableColumn<PrescriptionDTO, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        prescriptionIdColumn.setVisible(false);
        appointmentIdColumn.setText("Patient");
        appointmentIdColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(patientForAppointment(cell.getValue().getAppointmentId())));
        dateIssuedColumn.setCellValueFactory(new PropertyValueFactory<>("dateIssued"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label badge = new Label(status);
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add("status-pending");
                setGraphic(badge);
            }
        });
        addSortOption("Patient", appointmentIdColumn);
        addSortOption("Date Issued", dateIssuedColumn);
        addSortOption("Status", statusColumn);
        wireActionsColumn(actionsColumn);
    }

    private String patientForAppointment(String appointmentId) {
        try {
            var opt = lookupService.findById(EntityLookupService.APPOINTMENT, appointmentId);
            if (opt.isEmpty()) return "—";
            AppointmentDTO appointment = (AppointmentDTO) opt.get();
            return lookupService.patientLabel(appointment.getPatientId());
        } catch (Exception ex) {
            return "—";
        }
    }

    @Override
    protected boolean matches(PrescriptionDTO prescription, String lowerQuery) {
        return (prescription.getPrescriptionId() != null && prescription.getPrescriptionId().toLowerCase().contains(lowerQuery))
                || (prescription.getAppointmentId() != null && prescription.getAppointmentId().toLowerCase().contains(lowerQuery));
    }
}
