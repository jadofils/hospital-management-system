package hospital.management.pages.components.lab;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.lab.LabOrderDTO;
import hospital.management.backend.model.enums.LabOrderStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class LabOrderTableController extends PaginatedTableController<LabOrderDTO> {

    private static final DateTimeFormatter ORDERED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<LabOrderDTO, String> idColumn;
    @FXML private TableColumn<LabOrderDTO, String> doctorIdColumn;
    @FXML private TableColumn<LabOrderDTO, String> testNameColumn;
    @FXML private TableColumn<LabOrderDTO, String> statusColumn;
    @FXML private TableColumn<LabOrderDTO, Void>   changeStatusColumn;
    @FXML private TableColumn<LabOrderDTO, String> orderedAtColumn;
    @FXML private TableColumn<LabOrderDTO, Void>   actionsColumn;

    private Consumer<LabOrderDTO> onChangeStatus;

    public void setOnChangeStatus(Consumer<LabOrderDTO> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("labOrderId"));
        doctorIdColumn.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        testNameColumn.setCellValueFactory(new PropertyValueFactory<>("testName"));
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(statusLabel(cell.getValue().getStatus())));
        wireSingleActionColumn(changeStatusColumn, "fas-flag",
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        orderedAtColumn.setCellValueFactory(cell -> {
            var orderedAt = cell.getValue().getOrderedAt();
            return new SimpleStringProperty(orderedAt == null ? "" : orderedAt.format(ORDERED_AT_FORMAT));
        });
        wireActionsColumn(actionsColumn);
    }

    private static String statusLabel(String status) {
        try {
            return LabOrderStatus.fromDbValue(status).getLabel();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }

    @Override
    protected boolean matches(LabOrderDTO order, String lowerQuery) {
        String dateStr = order.getOrderedAt() != null ? order.getOrderedAt().format(ORDERED_AT_FORMAT) : "";
        return safe(order.getTestName()).contains(lowerQuery)
            || safe(statusLabel(order.getStatus())).contains(lowerQuery)
            || safe(order.getDoctorId()).contains(lowerQuery)
            || safe(order.getLabOrderId()).contains(lowerQuery)
            || dateStr.contains(lowerQuery);
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(); }
}
