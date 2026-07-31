package hospital.management.pages.components.pharmacy;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.pharmacy.Prescription;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class PrescriptionTableController extends PaginatedTableController<Prescription> {

    @FXML private TableColumn<Prescription, String> prescriptionIdColumn;
    @FXML private TableColumn<Prescription, String> appointmentIdColumn;
    @FXML private TableColumn<Prescription, java.time.LocalDate> dateIssuedColumn;
    @FXML private TableColumn<Prescription, Void>   actionsColumn;

    @Override
    protected void configureColumns() {
        prescriptionIdColumn.setCellValueFactory(new PropertyValueFactory<>("prescriptionId"));
        appointmentIdColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        dateIssuedColumn.setCellValueFactory(new PropertyValueFactory<>("dateIssued"));
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(Prescription prescription, String lowerQuery) {
        return (prescription.getPrescriptionId() != null && prescription.getPrescriptionId().toLowerCase().contains(lowerQuery))
                || (prescription.getAppointmentId() != null && prescription.getAppointmentId().toLowerCase().contains(lowerQuery));
    }
}
