package hospital.management.pages.components.log;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.log.AuditLogDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Read-only audit log table. Audit logs are immutable, so unlike the other
 * entity tables there is no actions column and no row edit/delete wiring.
 */
public class AuditLogTableController extends PaginatedTableController<AuditLogDTO> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private TableColumn<AuditLogDTO, String> idColumn;
    @FXML private TableColumn<AuditLogDTO, String> userIdColumn;
    @FXML private TableColumn<AuditLogDTO, String> actionColumn;
    @FXML private TableColumn<AuditLogDTO, String> tableColumn;
    @FXML private TableColumn<AuditLogDTO, String> recordIdColumn;
    @FXML private TableColumn<AuditLogDTO, String> createdAtColumn;

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("logId"));
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        tableColumn.setCellValueFactory(new PropertyValueFactory<>("tableAffected"));
        recordIdColumn.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        createdAtColumn.setCellValueFactory(cell -> {
            LocalDateTime createdAt = cell.getValue().getCreatedAt();
            return new SimpleStringProperty(
                    createdAt != null ? createdAt.format(TIMESTAMP_FORMAT) : "");
        });
        // No actions column — audit logs are immutable, read-only records.
    }

    @Override
    protected boolean matches(AuditLogDTO log, String lowerQuery) {
        boolean actionMatch = log.getAction() != null
                && log.getAction().toLowerCase().contains(lowerQuery);
        boolean tableMatch = log.getTableAffected() != null
                && log.getTableAffected().toLowerCase().contains(lowerQuery);
        return actionMatch || tableMatch;
    }
}
