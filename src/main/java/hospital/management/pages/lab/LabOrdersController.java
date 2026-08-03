package hospital.management.pages.lab;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.lab.LabOrderDAOImpl;
import hospital.management.backend.dao.lab.LabResultDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.lab.CreateLabOrderDTO;
import hospital.management.backend.dto.lab.CreateLabResultDTO;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.lab.LabServiceImpl;
import hospital.management.backend.service.lab.interfaces.LabService;
import hospital.management.backend.service.lookup.EntityLookupService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.lab.LabOrderTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LabOrdersController extends BasePageController {

    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());
    private final DoctorServiceImpl doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final LabService labService = new LabServiceImpl(new LabOrderDAOImpl(), new LabResultDAOImpl());
    private final EntityLookupService entityLookupService = new EntityLookupService();

    @FXML private LabOrderTableController labOrderTableController;

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button       newOrderBtn;

    private final List<LabOrderDTO> labOrders = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.LAB_ORDERS);

        statusFilter.getItems().addAll("All", "ORDERED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        newOrderBtn.setOnAction(e -> openLabOrderDialog());
        labOrderTableController.setRowActions(
                o -> toast("Lab orders can't be edited once placed.", NotificationType.INFO),
                this::confirmDeleteLabOrder, this::viewLabOrderDetail);
        labOrderTableController.setOnChangeStatus(this::openRecordResultDialog);

        refreshTable();
    }

    private void applyFilter() {
        String selected = statusFilter.getValue();
        List<LabOrderDTO> visible = labOrders;
        if (selected != null && !"All".equals(selected)) {
            visible = labOrders.stream()
                    .filter(o -> selected.equalsIgnoreCase(o.getStatus()))
                    .toList();
        }
        labOrderTableController.setItems(visible);
        labOrderTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            labOrders.clear();
            List<AppointmentSummaryDTO> appointments =
                    appointmentService.findAll(CursorPagination.firstPage(500)).getItems();
            for (AppointmentSummaryDTO appointment : appointments) {
                labOrders.addAll(labService.findOrdersByAppointment(appointment.getAppointmentId()));
            }
            labOrders.sort(Comparator.comparing(LabOrderDTO::getOrderedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            applyFilter();
        } catch (Exception e) {
            toastError("Failed to load lab orders: " + e.getMessage());
        }
    }

    private void viewLabOrderDetail(LabOrderDTO labOrder) {
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

    private void confirmDeleteLabOrder(LabOrderDTO labOrder) {
        confirm("Delete Lab Order",
                "Are you sure you want to delete the order for " + labOrder.getTestName() + "? This cannot be undone.",
                () -> {
                    try {
                        labService.deleteOrder(labOrder.getLabOrderId());
                        refreshTable();
                        toastSuccess("Lab order deleted.");
                    } catch (Exception e) {
                        toastError("Failed to delete lab order: " + e.getMessage());
                    }
                });
    }

    /** Opens the shared form dialog to order a new lab test. */
    private void openLabOrderDialog() {
        LoadingIdComboBox appointmentIdField = new LoadingIdComboBox();
        LoadingIdComboBox doctorIdField      = new LoadingIdComboBox();
        EntityIdComboBox appointmentId = appointmentIdField.getComboBox();
        EntityIdComboBox doctorId      = doctorIdField.getComboBox();
        TextField testName      = new TextField();

        testName.getStyleClass().add("form-input");
        List.of(appointmentId, doctorId).forEach(f -> f.getStyleClass().add("form-combo"));

        List<Control> otherFields = List.of(testName);
        otherFields.forEach(f -> f.setDisable(true));

        formDialogController.open("Add Lab Order", "fas-flask", true, v -> {
            String apptId = appointmentId.getSelectedId();
            String docId  = doctorId.getSelectedId();
            String test   = testName.getText() == null ? "" : testName.getText().trim();
            if (apptId == null || docId == null || test.isEmpty()) {
                formDialogController.setError("Appointment, doctor and test name are required.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                labService.orderTest(new CreateLabOrderDTO(apptId, docId, test));
                refreshTable();
                formDialogController.close();
                toastSuccess("Lab order added.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to add lab order: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Appointment", "fas-calendar-check", appointmentIdField);
        formDialogController.addField("Doctor", "fas-user-md", doctorIdField);
        formDialogController.addField("Test Name", "fas-vial", testName);

        loadLabOrderDropdowns(appointmentIdField, doctorIdField, otherFields);
    }

    /** Loads the appointment/doctor dropdown options asynchronously, showing each dropdown's own
     *  spinner while its data is in flight and keeping the rest of the form disabled until
     *  both have finished loading. */
    private void loadLabOrderDropdowns(LoadingIdComboBox appointmentIdField, LoadingIdComboBox doctorIdField,
                                        List<Control> otherFields) {
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
                doctorIdField.setLoading(false);
                onOneLoaded.run();
            },
            ex -> {
                doctorIdField.setLoading(false);
                toastError("Failed to load doctors: " + ex.getMessage());
                onOneLoaded.run();
            });
    }

    /** Recording a result is the only status transition the backend supports for a lab order —
     *  it flips the order to Completed in the same transaction that persists the result. */
    private void openRecordResultDialog(LabOrderDTO labOrder) {
        if ("completed".equalsIgnoreCase(labOrder.getStatus())) {
            toast("This lab order already has a result.", NotificationType.INFO);
            return;
        }

        TextField resultValue = new TextField();
        TextField unit        = new TextField();
        TextField referenceRange = new TextField();
        ComboBox<String> abnormal = new ComboBox<>();

        resultValue.getStyleClass().add("form-input");
        unit.getStyleClass().add("form-input");
        referenceRange.getStyleClass().add("form-input");
        abnormal.getStyleClass().add("form-combo");
        abnormal.getItems().addAll("Normal", "Abnormal");
        abnormal.setValue("Normal");

        formDialogController.open("Record Result", "fas-flask", false, v -> {
            String value = resultValue.getText() == null ? "" : resultValue.getText().trim();
            if (value.isEmpty()) {
                formDialogController.setError("Result value is required.");
                formDialogController.setLoading(false);
                return;
            }

            try {
                labService.recordResult(new CreateLabResultDTO(
                        labOrder.getLabOrderId(), value, unit.getText(), referenceRange.getText(),
                        "Abnormal".equals(abnormal.getValue()), LocalDateTime.now()));
                refreshTable();
                formDialogController.close();
                toastSuccess("Lab result recorded — order completed.");
            } catch (AppException ex) {
                formDialogController.setError(ex.getMessage());
                formDialogController.setLoading(false);
            } catch (Exception ex) {
                formDialogController.setError("Failed to record result: " + ex.getMessage());
                formDialogController.setLoading(false);
            }
        });

        formDialogController.addField("Result Value", "fas-chart-line", resultValue);
        formDialogController.addField("Unit", "fas-flask", unit);
        formDialogController.addField("Reference Range", "fas-arrows-alt-h", referenceRange);
        formDialogController.addField("Result", "fas-exclamation-circle", abnormal);
    }
}
