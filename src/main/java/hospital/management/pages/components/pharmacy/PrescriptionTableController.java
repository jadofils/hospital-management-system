package hospital.management.pages.components.pharmacy;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.pharmacy.PrescriptionDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class PrescriptionTableController extends PaginatedTableController<PrescriptionDTO> {

    @FXML private TableColumn<PrescriptionDTO, String> prescriptionIdColumn;
    @FXML private TableColumn<PrescriptionDTO, String> appointmentIdColumn;
    @FXML private TableColumn<PrescriptionDTO, java.time.LocalDate> dateIssuedColumn;
    @FXML private TableColumn<PrescriptionDTO, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        prescriptionIdColumn.setCellValueFactory(new PropertyValueFactory<>("prescriptionId"));
        appointmentIdColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        dateIssuedColumn.setCellValueFactory(new PropertyValueFactory<>("dateIssued"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(PrescriptionDTO prescription, String lowerQuery) {
        return (prescription.getPrescriptionId() != null && prescription.getPrescriptionId().toLowerCase().contains(lowerQuery))
                || (prescription.getAppointmentId() != null && prescription.getAppointmentId().toLowerCase().contains(lowerQuery));
    }
}
