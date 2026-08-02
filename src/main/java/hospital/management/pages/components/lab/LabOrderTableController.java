package hospital.management.pages.components.lab;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.lab.LabOrder;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class LabOrderTableController extends PaginatedTableController<LabOrder> {

    private static final DateTimeFormatter ORDERED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<LabOrder, String> idColumn;
    @FXML private TableColumn<LabOrder, String> doctorIdColumn;
    @FXML private TableColumn<LabOrder, String> testNameColumn;
    @FXML private TableColumn<LabOrder, String> statusColumn;
    @FXML private TableColumn<LabOrder, Void>   changeStatusColumn;
    @FXML private TableColumn<LabOrder, String> orderedAtColumn;
    @FXML private TableColumn<LabOrder, Void>   actionsColumn;

    private Consumer<LabOrder> onChangeStatus;

    public void setOnChangeStatus(Consumer<LabOrder> onChangeStatus) {
        this.onChangeStatus = onChangeStatus;
    }

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("labOrderId"));
        doctorIdColumn.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        testNameColumn.setCellValueFactory(new PropertyValueFactory<>("testName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        wireSingleActionColumn(changeStatusColumn, "fas-flag",
                item -> { if (onChangeStatus != null) onChangeStatus.accept(item); });
        orderedAtColumn.setCellValueFactory(cell -> {
            var orderedAt = cell.getValue().getOrderedAt();
            return new SimpleStringProperty(orderedAt == null ? "" : orderedAt.format(ORDERED_AT_FORMAT));
        });
        wireActionsColumn(actionsColumn);
    }

    @Override
    protected boolean matches(LabOrder order, String lowerQuery) {
        String testName = order.getTestName();
        String status = order.getStatus();
        return (testName != null && testName.toLowerCase().contains(lowerQuery))
                || (status != null && status.toLowerCase().contains(lowerQuery));
    }
}
