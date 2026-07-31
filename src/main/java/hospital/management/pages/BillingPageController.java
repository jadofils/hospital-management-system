package hospital.management.pages;

import hospital.management.pages.components.SidebarController;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.model.enums.PaymentStatus;
import hospital.management.enums.PageRoute;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BillingPageController {

    @FXML private SidebarController sidebarController;

    @FXML private Label totalRevenueLabel;
    @FXML private Label paidLabel;
    @FXML private Label pendingLabel;

    @FXML private TableView<Invoice> billingTable;
    @FXML private TableColumn<Invoice, String>     billIdCol;
    @FXML private TableColumn<Invoice, String>     billPatientCol;
    @FXML private TableColumn<Invoice, LocalDateTime> billDateCol;
    @FXML private TableColumn<Invoice, BigDecimal> billAmountCol;
    @FXML private TableColumn<Invoice, String>     billStatusCol;
    @FXML private TableColumn<Invoice, String>     billDescCol;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.BILLING);

        billIdCol.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        billPatientCol.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        billDateCol.setCellValueFactory(new PropertyValueFactory<>("issuedAt"));
        billAmountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        billStatusCol.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));
        billDescCol.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));

        billingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        var invoices = FXCollections.observableArrayList(
            new Invoice("INV-001", "APT-001", "PAT-001", new BigDecimal("1200.00"), PaymentStatus.PAID.getDbValue(),          LocalDateTime.now().minusDays(3), LocalDateTime.now(), null),
            new Invoice("INV-002", "APT-002", "PAT-002", new BigDecimal("850.00"),  PaymentStatus.PAID.getDbValue(),          LocalDateTime.now().minusDays(4), LocalDateTime.now(), null),
            new Invoice("INV-003", "APT-003", "PAT-003", new BigDecimal("3400.00"), PaymentStatus.UNPAID.getDbValue(),        LocalDateTime.now().minusDays(5), LocalDateTime.now(), null),
            new Invoice("INV-004", "APT-004", "PAT-004", new BigDecimal("620.00"),  PaymentStatus.UNPAID.getDbValue(),        LocalDateTime.now().minusDays(6), LocalDateTime.now(), null),
            new Invoice("INV-005", "APT-005", "PAT-005", new BigDecimal("2100.00"), PaymentStatus.PAID.getDbValue(),          LocalDateTime.now().minusDays(7), LocalDateTime.now(), null)
        );

        billingTable.setItems(invoices);

        double total   = invoices.stream().mapToDouble(i -> i.getTotalAmount().doubleValue()).sum();
        double paid    = invoices.stream()
                                 .filter(i -> PaymentStatus.PAID.getDbValue().equals(i.getPaymentStatus()))
                                 .mapToDouble(i -> i.getTotalAmount().doubleValue())
                                 .sum();
        double pending = total - paid;

        totalRevenueLabel.setText(String.format("$%,.2f", total));
        paidLabel.setText(String.format("$%,.2f", paid));
        pendingLabel.setText(String.format("$%,.2f", pending));
    }
}