package hospital.management.pages.log;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.log.SystemLogDAOImpl;
import hospital.management.backend.dto.log.SystemLogDTO;
import hospital.management.backend.service.log.SystemLogServiceImpl;
import hospital.management.backend.service.log.interfaces.SystemLogService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.log.SystemLogTableController;
import hospital.management.pages.utils.CsvUiIO;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SystemLogsController extends BasePageController {

    private static final int FETCH_SIZE = 500;

    private final SystemLogService systemLogService = new SystemLogServiceImpl(new SystemLogDAOImpl());

    @FXML private SystemLogTableController systemLogTableController;

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> levelFilter;
    @FXML private DatePicker   fromDatePicker;
    @FXML private DatePicker   toDatePicker;
    @FXML private Button       purgeBtn;
    @FXML private Button       exportBtn;

    private List<SystemLogDTO> logs = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.SYSTEM_LOGS);

        levelFilter.getItems().addAll("All Levels", "DEBUG", "INFO", "WARNING", "ERROR");
        levelFilter.setValue("All Levels");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        levelFilter.setOnAction(e -> refreshTable());

        purgeBtn.setOnAction(e -> confirmPurgeLogs());
        exportBtn.setOnAction(e -> withSpinner(exportBtn, this::exportCsv));

        refreshTable();
    }

    private void applyFilter() {
        systemLogTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        try {
            String level = levelFilter.getValue();
            if (level != null && !"All Levels".equals(level)) {
                logs = systemLogService.findByLevel(level);
            } else {
                logs = systemLogService.findAll(CursorPagination.firstPage(FETCH_SIZE)).getItems();
            }
            systemLogTableController.setItems(logs);
        } catch (Exception e) {
            toastError("Failed to load system logs: " + e.getMessage());
        }
    }

    private void confirmPurgeLogs() {
        confirm("Purge System Logs",
                "This will permanently delete all system log entries. This cannot be undone.",
                () -> {
                    try {
                        int purged = systemLogService.purgeOlderThanDays(0);
                        refreshTable();
                        toastSuccess(purged + " system log entries purged.");
                    } catch (Exception e) {
                        toastError("Failed to purge system logs: " + e.getMessage());
                    }
                });
    }

    private void exportCsv() {
        try {
            if (logs.isEmpty()) {
                toastError("No system logs available to export.");
                return;
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (SystemLogDTO log : logs) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("log_id", log.getLogId());
                row.put("log_level", log.getLogLevel());
                row.put("source", log.getSource());
                row.put("message", log.getMessage());
                row.put("user_id", log.getUserId());
                row.put("created_at", log.getCreatedAt());
                rows.add(row);
            }

            boolean saved = CsvUiIO.exportRows(exportBtn.getScene().getWindow(), "system-logs.csv", rows);
            if (saved) {
                toastSuccess("System logs exported successfully.");
            }
        } catch (Exception e) {
            toastError("Failed to export system logs: " + e.getMessage());
        }
    }
}
