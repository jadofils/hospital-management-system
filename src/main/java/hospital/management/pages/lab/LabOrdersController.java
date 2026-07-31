package hospital.management.pages.lab;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.lab.LabOrder;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.lab.LabOrderTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LabOrdersController extends BasePageController {

    @FXML private LabOrderTableController labOrderTableController;

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button       newOrderBtn;

    private final List<LabOrder> labOrders = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.LAB_ORDERS);

        statusFilter.getItems().addAll("All", "ORDERED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        newOrderBtn.setOnAction(e -> openLabOrderDialog(null));
        labOrderTableController.setRowActions(this::openLabOrderDialog, this::confirmDeleteLabOrder);

        refreshTable();
    }

    private void applyFilter() {
        labOrderTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        labOrderTableController.setItems(labOrders);
    }

    private void confirmDeleteLabOrder(LabOrder labOrder) {
        confirm("Delete Lab Order",
                "Are you sure you want to delete the order for " + labOrder.getTestName() + "? This cannot be undone.",
                () -> {
                    labOrders.remove(labOrder);
                    refreshTable();
                    toastSuccess("Lab order deleted.");
                });
    }

    /** Opens the shared form dialog in Add mode (labOrder == null) or Update mode. */
    private void openLabOrderDialog(LabOrder labOrder) {
        boolean addMode = labOrder == null;

        TextField appointmentId = new TextField();
        TextField doctorId      = new TextField();
        TextField testName      = new TextField();
        ComboBox<String> status = new ComboBox<>();

        List.of(appointmentId, doctorId, testName).forEach(f -> f.getStyleClass().add("form-input"));
        status.getStyleClass().add("form-combo");
        status.getItems().addAll("Pending", "In Progress", "Completed", "Cancelled");

        if (!addMode) {
            appointmentId.setText(labOrder.getAppointmentId());
            doctorId.setText(labOrder.getDoctorId());
            testName.setText(labOrder.getTestName());
            status.setValue(labOrder.getStatus());
        }

        formDialogController.open(addMode ? "Add Lab Order" : "Update Lab Order", "fas-flask", addMode, v -> {
            String apptId = appointmentId.getText() == null ? "" : appointmentId.getText().trim();
            String docId  = doctorId.getText() == null ? "" : doctorId.getText().trim();
            String test   = testName.getText() == null ? "" : testName.getText().trim();
            if (apptId.isEmpty() || docId.isEmpty() || test.isEmpty() || status.getValue() == null) {
                formDialogController.setError("Appointment ID, doctor ID, test name and status are required.");
                formDialogController.setLoading(false);
                return;
            }

            LabOrder target = addMode ? new LabOrder() : labOrder;
            if (addMode) target.setLabOrderId(UUID.randomUUID().toString());
            target.setAppointmentId(apptId);
            target.setDoctorId(docId);
            target.setTestName(test);
            target.setStatus(status.getValue());

            if (addMode) labOrders.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Lab order added." : "Lab order updated.");
        });

        formDialogController.addField("Appointment Id", "fas-calendar-check", appointmentId);
        formDialogController.addField("Doctor Id", "fas-user-md", doctorId);
        formDialogController.addField("Test Name", "fas-vial", testName);
        formDialogController.addField("Status", "fas-list", status);
    }
}
