package hospital.management.pages.components.log;

import hospital.management.pages.components.PaginatedTableController;
import hospital.management.backend.dto.log.SystemLogDTO;
import hospital.management.backend.service.lookup.EntityLookupService;
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
public class SystemLogTableController extends PaginatedTableController<SystemLogDTO> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EntityLookupService lookupService = new EntityLookupService();

    @FXML private TableColumn<SystemLogDTO, String> idColumn;
    @FXML private TableColumn<SystemLogDTO, String> userIdColumn;
    @FXML private TableColumn<SystemLogDTO, String> levelColumn;
    @FXML private TableColumn<SystemLogDTO, String> sourceColumn;
    @FXML private TableColumn<SystemLogDTO, String> messageColumn;
    @FXML private TableColumn<SystemLogDTO, String> timestampColumn;

    @Override
    protected void configureColumns() {
        idColumn.setVisible(false);
        userIdColumn.setText("User");
        userIdColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(resolveLabel(() -> lookupService.userLabel(cell.getValue().getUserId()))));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("logLevel"));
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        timestampColumn.setCellValueFactory(cell -> {
            LocalDateTime createdAt = cell.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt != null ? createdAt.format(DATE_FORMAT) : "");
        });
        addSortOption("User", userIdColumn);
        addSortOption("Level", levelColumn);
        addSortOption("Source", sourceColumn);
        addSortOption("Timestamp", timestampColumn);
    }

    @Override
    protected boolean matches(SystemLogDTO log, String lowerQuery) {
        String level = log.getLogLevel();
        String source = log.getSource();
        return (level != null && level.toLowerCase().contains(lowerQuery))
                || (source != null && source.toLowerCase().contains(lowerQuery));
    }
}
