package hospital.management.pages.lab;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.model.lab.LabOrder;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.lab.LabOrderTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LabOrdersController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final DoctorServiceImpl doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

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
        labOrderTableController.setRowActions(this::openLabOrderDialog, this::confirmDeleteLabOrder, this::viewLabOrderDetail);
        labOrderTableController.setOnChangeStatus(this::openLabOrderStatusDialog);

        refreshTable();
    }

    private void applyFilter() {
        labOrderTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        labOrderTableController.setItems(labOrders);
    }

    private void viewLabOrderDetail(LabOrder labOrder) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            fields.put("Appointment", entityLookupService.appointmentLabel(labOrder.getAppointmentId()));
            fields.put("Doctor", entityLookupService.doctorLabel(labOrder.getDoctorId()));
        } catch (Exception ex) {
            toastError("Failed to resolve lab order details: " + ex.getMessage());
        }
        fields.put("Test Name", labOrder.getTestName());
        fields.put("Status", labOrder.getStatus());
        fields.put("Ordered At", labOrder.getOrderedAt() == null ? null : labOrder.getOrderedAt().toString());
        detailViewController.show("Lab Order Details", "fas-flask", fields);
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

        EntityIdComboBox appointmentId = new EntityIdComboBox();
        EntityIdComboBox doctorId      = new EntityIdComboBox();
        TextField testName      = new TextField();

        testName.getStyleClass().add("form-input");
        List.of(appointmentId, doctorId).forEach(f -> f.getStyleClass().add("form-combo"));

        try {
            appointmentId.setOptions(appointmentService.findAll(CursorPagination.firstPage(1000)).getItems().stream()
                    .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                            a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                    .toList());
            doctorId.setOptions(doctorService.findAll(CursorPagination.firstPage(1000)).getItems().stream()
                    .map(d -> new EntityIdComboBox.Option(d.getDoctorId(), d.getFullName())).toList());
        } catch (Exception ex) {
            toastError("Failed to load appointments/doctors: " + ex.getMessage());
        }

        if (!addMode) {
            appointmentId.selectById(labOrder.getAppointmentId());
            doctorId.selectById(labOrder.getDoctorId());
            testName.setText(labOrder.getTestName());
        }

        formDialogController.open(addMode ? "Add Lab Order" : "Update Lab Order", "fas-flask", addMode, v -> {
            String apptId = appointmentId.getSelectedId();
            String docId  = doctorId.getSelectedId();
            String test   = testName.getText() == null ? "" : testName.getText().trim();
            if (apptId == null || docId == null || test.isEmpty()) {
                formDialogController.setError("Appointment, doctor and test name are required.");
                formDialogController.setLoading(false);
                return;
            }

            LabOrder target = addMode ? new LabOrder() : labOrder;
            if (addMode) {
                target.setLabOrderId(UUID.randomUUID().toString());
                target.setStatus("Pending");
            }
            target.setAppointmentId(apptId);
            target.setDoctorId(docId);
            target.setTestName(test);

            if (addMode) labOrders.add(target);
            refreshTable();
            formDialogController.close();
            toastSuccess(addMode ? "Lab order added." : "Lab order updated.");
        });

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentId);
        formDialogController.addField("Doctor", "fas-user-md", doctorId);
        formDialogController.addField("Test Name", "fas-vial", testName);
    }

    /** Minimal single-field dialog for changing an existing lab order's status, kept out of the main Add/Edit form. */
    private void openLabOrderStatusDialog(LabOrder labOrder) {
        ComboBox<String> status = new ComboBox<>();
        status.getStyleClass().add("form-combo");
        status.getItems().addAll("Pending", "In Progress", "Completed", "Cancelled");
        status.setValue(labOrder.getStatus());

        formDialogController.open("Change Status", "fas-list", false, v -> {
            if (status.getValue() == null) {
                formDialogController.setError("Status is required.");
                formDialogController.setLoading(false);
                return;
            }
            labOrder.setStatus(status.getValue());
            refreshTable();
            formDialogController.close();
            toastSuccess("Lab order status updated.");
        });

        formDialogController.addField("Status", "fas-list", status);
    }
}
