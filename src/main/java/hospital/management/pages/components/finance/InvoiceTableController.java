package hospital.management.pages.components.finance;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.finance.InvoiceDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class InvoiceTableController extends PaginatedTableController<InvoiceDTO> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<InvoiceDTO, String>     invoiceIdColumn;
    @FXML private TableColumn<InvoiceDTO, String>     patientIdColumn;
    @FXML private TableColumn<InvoiceDTO, String>     appointmentIdColumn;
    @FXML private TableColumn<InvoiceDTO, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<InvoiceDTO, String>     paymentStatusColumn;
    @FXML private TableColumn<InvoiceDTO, Void>       changeStatusColumn;
    @FXML private TableColumn<InvoiceDTO, String>     issuedAtColumn;
    @FXML private TableColumn<InvoiceDTO, Void>       actionsColumn;

    private Consumer<InvoiceDTO> onChangeStatus;

    public void setOnChangeStatus(Consumer<InvoiceDTO> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
    protected void configureColumns() {
        invoiceIdColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        patientIdColumn.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        appointmentIdColumn.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        wireSingleActionColumn(changeStatusColumn, "fas-flag",
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        issuedAtColumn.setCellValueFactory(cell -> {
            LocalDateTime issuedAt = cell.getValue().getIssuedAt();
            return new SimpleStringProperty(issuedAt != null ? issuedAt.format(DATE_FORMAT) : "");
        });
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(InvoiceDTO invoice, String lowerQuery) {
        String patientId = invoice.getPatientId();
        String paymentStatus = invoice.getPaymentStatus();
        return (patientId != null && patientId.toLowerCase().contains(lowerQuery))
                || (paymentStatus != null && paymentStatus.toLowerCase().contains(lowerQuery));
    }
}
