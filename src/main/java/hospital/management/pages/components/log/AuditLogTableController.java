package hospital.management.pages.components.log;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.user.AuditLog;
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
public class AuditLogTableController extends PaginatedTableController<AuditLog> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private TableColumn<AuditLog, String> idColumn;
    @FXML private TableColumn<AuditLog, String> userIdColumn;
    @FXML private TableColumn<AuditLog, String> actionColumn;
    @FXML private TableColumn<AuditLog, String> tableColumn;
    @FXML private TableColumn<AuditLog, String> recordIdColumn;
    @FXML private TableColumn<AuditLog, String> createdAtColumn;

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
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
    protected boolean matches(AuditLog log, String lowerQuery) {
        boolean actionMatch = log.getAction() != null
                && log.getAction().toLowerCase().contains(lowerQuery);
        boolean tableMatch = log.getTableAffected() != null
                && log.getTableAffected().toLowerCase().contains(lowerQuery);
        return actionMatch || tableMatch;
    }
}
