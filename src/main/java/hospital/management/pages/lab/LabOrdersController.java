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
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.pages.components.lab.LabOrderTableController;
import hospital.management.pages.components.shared.search.EntityIdComboBox;
import hospital.management.pages.components.shared.search.LoadingIdComboBox;
import hospital.management.pages.components.shared.sort.SortBarController;
import hospital.management.pages.utils.CsvUiIO;
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
    @FXML private SortBarController sortBarController;

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button       newOrderBtn;
    @FXML private Button       importBtn;
    @FXML private Button       exportBtn;
    @FXML private Button       continueBtn;

    private final List<LabOrderDTO> labOrders = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.LAB_ORDERS);

        statusFilter.getItems().addAll("All", "ORDERED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        statusFilter.setValue("All");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.setOnAction(e -> applyFilter());

        applyCreateVisibility(newOrderBtn, PageRoute.LAB_ORDERS);
        applyCreateVisibility(importBtn, PageRoute.LAB_ORDERS);
        boolean canExport = canRead(PageRoute.LAB_ORDERS);
        exportBtn.setVisible(canExport);
        exportBtn.setManaged(canExport);

        newOrderBtn.setOnAction(e -> openLabOrderDialog());
        importBtn.setOnAction(e -> withSpinner(importBtn, this::importLabOrders));
        exportBtn.setOnAction(e -> withSpinner(exportBtn, this::exportLabOrders));
        setupContinueButton(continueBtn, PageRoute.LAB_ORDERS);
        labOrderTableController.setRowActions(
            canUpdate(PageRoute.LAB_ORDERS) ? o -> toast("Lab orders can't be edited once placed.", NotificationType.INFO) : null,
            allowDelete(PageRoute.LAB_ORDERS, this::confirmDeleteLabOrder),
            allowRead(PageRoute.LAB_ORDERS, this::viewLabOrderDetail));
        labOrderTableController.setOnChangeStatus(canUpdate(PageRoute.LAB_ORDERS) ? this::openRecordResultDialog : null);

        if (sortBarController != null) {
            sortBarController.setOnSort((field, asc) -> labOrderTableController.applySort(field, asc));
            sortBarController.addOptions(labOrderTableController.getSortOptionLabels());
        }

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

    private void exportLabOrders() {
        try {
            if (labOrders.isEmpty()) {
                toastError("No lab orders available to export.");
                return;
            }
            List<LabOrderDTO> source = chooseLabExportSource();
            if (source.isEmpty()) {
                return;
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (LabOrderDTO order : source) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("lab_order_id", order.getLabOrderId());
                row.put("appointment_id", order.getAppointmentId());
                row.put("doctor_id", order.getDoctorId());
                row.put("test_name", order.getTestName());
                row.put("status", order.getStatus());
                row.put("ordered_at", order.getOrderedAt());
                rows.add(row);
            }

            boolean saved = CsvUiIO.exportRows(exportBtn.getScene().getWindow(), "lab-orders.csv", rows);
            if (saved) {
                toastSuccess("Lab orders exported successfully.");
            }
        } catch (Exception e) {
            toastError("Failed to export lab orders: " + e.getMessage());
        }
    }

    private List<LabOrderDTO> chooseLabExportSource() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Current table view", "Current table view", "All loaded rows");
        dialog.setTitle("Export Lab Orders");
        dialog.setHeaderText("Choose what to export");
        dialog.setContentText("Export scope:");
        String choice = dialog.showAndWait().orElse(null);
        if (choice == null) {
            return List.of();
        }
        if ("Current table view".equals(choice)) {
            return new ArrayList<>(labOrderTableController.getTable().getItems());
        }
        return labOrders;
    }

    private void importLabOrders() {
        try {
            List<Map<String, String>> rows = CsvUiIO.importRows(importBtn.getScene().getWindow(), "Import Lab Orders");
            if (rows.isEmpty()) {
                return;
            }

            int ok = 0;
            int failed = 0;
            for (Map<String, String> row : rows) {
                try {
                    CreateLabOrderDTO dto = new CreateLabOrderDTO(
                            value(row, "appointment_id"),
                            value(row, "doctor_id"),
                            value(row, "test_name"));
                    labService.orderTest(dto);
                    ok++;
                } catch (Exception ex) {
                    failed++;
                }
            }

            refreshTable();
            if (failed == 0) {
                toastSuccess("Imported " + ok + " lab order(s).");
            } else {
                toastError("Imported " + ok + " lab order(s), failed " + failed + ".");
            }
        } catch (Exception e) {
            toastError("Failed to import lab orders: " + e.getMessage());
        }
    }

    private String value(Map<String, String> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key).trim();
            }
        }
        return "";
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

        testName.setPromptText("e.g. Complete Blood Count (CBC)");
        testName.getStyleClass().add("form-input");
        List.of(appointmentId, doctorId).forEach(f -> f.getStyleClass().add("form-combo"));

        FxFormValidator.attachRequired(testName, null, "Test name");
        FxFormValidator.attachMaxLength(testName, null, 200, "Test name");

        List<Control> otherFields = List.of(testName);
        otherFields.forEach(f -> f.setDisable(true));

        formDialogController.open("Add Lab Order", "fas-flask", true, v -> {
            String apptId = appointmentId.getSelectedId();
            String docId  = doctorId.getSelectedId();
            String test   = testName.getText() == null ? "" : testName.getText().trim();
            if (apptId == null) {
                formDialogController.setError("Appointment is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (docId == null) {
                formDialogController.setError("Doctor is required.");
                formDialogController.setLoading(false);
                return;
            }
            if (test.isEmpty()) {
                formDialogController.setError("Test name is required.");
                FxFormValidator.applyStyle(testName, false);
                formDialogController.setLoading(false);
                return;
            }
            if (test.length() > 200) {
                formDialogController.setError("Test name must not exceed 200 characters.");
                FxFormValidator.applyStyle(testName, false);
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

        // Map each appointment to the doctor who holds it, so selecting an
        // appointment auto-fills the doctor dropdown with the appointment's doctor.
        Map<String, String> appointmentDoctorIds = new LinkedHashMap<>();

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
                List<EntityIdComboBox.Option> options = items.stream()
                        .map(a -> {
                            appointmentDoctorIds.put(a.getAppointmentId(), a.getDoctorId());
                            return new EntityIdComboBox.Option(a.getAppointmentId(),
                                    a.getPatientName() + " with " + a.getDoctorName() + " — " + a.getAppointmentDate());
                        })
                        .toList();
                appointmentId.setOptions(options);
                appointmentId.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        String docId = appointmentDoctorIds.get(newVal.id());
                        if (docId != null) doctorId.selectById(docId);
                    }
                });
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
                String selectedAppointmentId = appointmentId.getSelectedId();
                if (selectedAppointmentId != null) {
                    String docId = appointmentDoctorIds.get(selectedAppointmentId);
                    if (docId != null) doctorId.selectById(docId);
                }
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
