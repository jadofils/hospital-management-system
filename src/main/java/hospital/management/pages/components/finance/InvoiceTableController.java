package hospital.management.pages.components.finance;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.finance.Invoice;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InvoiceTableController extends PaginatedTableController<Invoice> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<Invoice, String>     invoiceIdColumn;
    @FXML private TableColumn<Invoice, String>     patientIdColumn;
    @FXML private TableColumn<Invoice, String>     appointmentIdColumn;
    @FXML private TableColumn<Invoice, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<Invoice, String>     paymentStatusColumn;
    @FXML private TableColumn<Invoice, String>     issuedAtColumn;
    @FXML private TableColumn<Invoice, Void>       actionsColumn;

    @Override
    protected void configureColumns() {
        invoiceIdColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        patientIdColumn.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        appointmentIdColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        issuedAtColumn.setCellValueFactory(cell -> {
            LocalDateTime issuedAt = cell.getValue().getIssuedAt();
            return new SimpleStringProperty(issuedAt != null ? issuedAt.format(DATE_FORMAT) : "");
        });
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(Invoice invoice, String lowerQuery) {
        String patientId = invoice.getPatientId();
        String paymentStatus = invoice.getPaymentStatus();
        return (patientId != null && patientId.toLowerCase().contains(lowerQuery))
                || (paymentStatus != null && paymentStatus.toLowerCase().contains(lowerQuery));
    }
}
