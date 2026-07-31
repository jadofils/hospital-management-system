package hospital.management.pages.log;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.user.SystemLog;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.log.SystemLogTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

public class SystemLogsController extends BasePageController {

    @FXML private SystemLogTableController systemLogTableController;

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> levelFilter;
    @FXML private DatePicker   fromDatePicker;
    @FXML private DatePicker   toDatePicker;
    @FXML private Button       purgeBtn;
    @FXML private Button       exportBtn;

    private final List<SystemLog> logs = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.SYSTEM_LOGS);

        levelFilter.getItems().addAll("All Levels", "DEBUG", "INFO", "WARNING", "ERROR");
        levelFilter.setValue("All Levels");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        levelFilter.setOnAction(e -> applyFilter());

        purgeBtn.setOnAction(e -> confirmPurgeLogs());
        exportBtn.setOnAction(e -> toast("Export not yet implemented.", NotificationType.INFO));

        refreshTable();
    }

    private void applyFilter() {
        // searchField drives the actual predicate (matches logLevel or source);
        // levelFilter is wired to re-trigger it for a consistent filtering feel,
        // mirroring PatientsPageController's statusFilter (also not itself matched on).
        systemLogTableController.filter(searchField.getText());
    }

    private void refreshTable() {
        systemLogTableController.setItems(logs);
    }

    private void confirmPurgeLogs() {
        confirm("Purge System Logs",
                "This will permanently delete all system log entries. This cannot be undone.",
                () -> {
                    logs.clear();
                    refreshTable();
                    toastSuccess("System logs purged.");
                });
    }
}
