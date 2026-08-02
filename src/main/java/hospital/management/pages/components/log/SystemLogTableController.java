package hospital.management.pages.components.log;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.model.user.SystemLog;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Read-only table controller for the System Logs page. Unlike the other
 * entity tables, system logs are append-only and never edited or deleted
 * row-by-row, so this deliberately has no actions column and never calls
 * {@link #wireActionsColumn(TableColumn)}.
 */
public class SystemLogTableController extends PaginatedTableController<SystemLog> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private TableColumn<SystemLog, String> idColumn;
    @FXML private TableColumn<SystemLog, String> userIdColumn;
    @FXML private TableColumn<SystemLog, String> levelColumn;
    @FXML private TableColumn<SystemLog, String> sourceColumn;
    @FXML private TableColumn<SystemLog, String> messageColumn;
    @FXML private TableColumn<SystemLog, String> timestampColumn;

    @Override
    protected void configureColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("logLevel"));
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        timestampColumn.setCellValueFactory(cell -> {
            LocalDateTime createdAt = cell.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt != null ? createdAt.format(DATE_FORMAT) : "");
        });
    }

    @Override
    protected boolean matches(SystemLog log, String lowerQuery) {
        String level = log.getLogLevel();
        String source = log.getSource();
        return (level != null && level.toLowerCase().contains(lowerQuery))
                || (source != null && source.toLowerCase().contains(lowerQuery));
    }
}
