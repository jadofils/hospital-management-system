package hospital.management.pages;

import hospital.management.pages.components.SidebarController;
import hospital.management.backend.model.finance.Bill;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class BillingPageController {

    @FXML private SidebarController sidebarController;

    @FXML private Label totalRevenueLabel;
    @FXML private Label paidLabel;
    @FXML private Label pendingLabel;

    @FXML private TableView<Bill> billingTable;
    @FXML private TableColumn<Bill, String> billIdCol;
    @FXML private TableColumn<Bill, String> billPatientCol;
    @FXML private TableColumn<Bill, String> billDateCol;
    @FXML private TableColumn<Bill, Double> billAmountCol;
    @FXML private TableColumn<Bill, String> billStatusCol;
    @FXML private TableColumn<Bill, String> billDescCol;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem("billing");

        billIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        billPatientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        billDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        billAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        billStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        billDescCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        billingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        var bills = FXCollections.observableArrayList(
            new Bill("INV-001", "Alice Johnson", "2026-07-28", 1_200.00, "Paid",    "Consultation + Lab"),
            new Bill("INV-002", "Bob Smith",     "2026-07-27",   850.00, "Paid",    "X-Ray + Medication"),
            new Bill("INV-003", "Clara Davis",   "2026-07-26", 3_400.00, "Pending", "Surgery prep"),
            new Bill("INV-004", "Daniel Brown",  "2026-07-25",   620.00, "Pending", "Cardiology visit"),
            new Bill("INV-005", "Eva Martinez",  "2026-07-24", 2_100.00, "Paid",    "ICU - 2 days")
        );

        billingTable.setItems(bills);

        double total   = bills.stream().mapToDouble(Bill::getAmount).sum();
        double paid    = bills.stream().filter(b -> "Paid".equals(b.getStatus())).mapToDouble(Bill::getAmount).sum();
        double pending = total - paid;

        totalRevenueLabel.setText(String.format("$%,.2f", total));
        paidLabel.setText(String.format("$%,.2f", paid));
        pendingLabel.setText(String.format("$%,.2f", pending));
    }
}