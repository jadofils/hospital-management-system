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
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.lab.LabOrderTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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

        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        LoadingIdComboBox doctorIdField      = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox doctorId      = doctorIdField.getComboBox();
        TextField testName      = new TextField();

        testName.getStyleClass().add("form-input");
        List.of(appointmentId, doctorId).forEach(f -> f.getStyleClass().add("form-combo"));

        List<Control> otherFields = List.of(testName);
        otherFields.forEach(f -> f.setDisable(true));

        if (!addMode) {
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

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Doctor", "fas-user-md", doctorIdField);
        formDialogController.addField("Test Name", "fas-vial", testName);

        loadLabOrderDropdowns(appointmentIdField, doctorIdField, otherFields, addMode ? null : labOrder);
    }

    /** Loads the appointment/doctor dropdown options asynchronously, showing each dropdown's own
     *  spinner while its data is in flight and keeping the rest of the form disabled until
     *  both have finished loading. */
    private void loadLabOrderDropdowns(LoadingIdComboBox appointmentIdField, LoadingIdComboBox doctorIdField,
                                        List<Control> otherFields, LabOrder existing) {
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox doctorId = doctorIdField.getComboBox();

        appointmentIdField.setLoading(true);
        doctorIdField.setLoading(true);
        formDialogController.setLoading(true);

        AtomicInteger pending = new AtomicInteger(2);
        Runnable onOneLoaded = () -> {
            if (pending.decrementAndGet() == 0) {
                otherFields.forEach(f -> f.setDisable(false));
                formDialogController.setLoading(false);
            }
        };

        AsyncJobRunner.submit(
            () -> appointmentService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                appointmentId.setOptions(items.stream()
                        .map(a -> new EntityIdComboBox.Option(a.getAppointmentId(),
                                a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate()))
                        .toList());
                if (existing != null) appointmentId.selectById(existing.getAppointmentId());
                appointmentIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                appointmentIdField.setLoading(false);
                toastError("Failed to load appointments: " + ex.getMessage());
                onOneLoaded.run();
            });

        AsyncJobRunner.submit(
            () -> doctorService.findAll(CursorPagination.firstPage(1000)).getItems(),
            items -> {
                doctorId.setOptions(items.stream()
                        .map(d -> new EntityIdComboBox.Option(d.getDoctorId(), d.getFullName())).toList());
                if (existing != null) doctorId.selectById(existing.getDoctorId());
                doctorIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                doctorIdField.setLoading(false);
                toastError("Failed to load doctors: " + ex.getMessage());
                onOneLoaded.run();
            });
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
