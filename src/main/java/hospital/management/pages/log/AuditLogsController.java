package hospital.management.pages.log;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.log.AuditLogDAOImpl;
import hospital.management.backend.dto.log.AuditLogDTO;
import hospital.management.backend.service.log.AuditServiceImpl;
import hospital.management.backend.service.log.interfaces.AuditService;
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.log.AuditLogTableController;
import hospital.management.pages.components.shared.sort.SortBarController;
import hospital.management.pages.utils.CsvUiIO;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only audit log viewer. Audit logs are immutable — there is no
 * add/edit/delete flow on this page, so it never touches the shared
 * form-dialog/confirm-modal pieces from {@link BasePageController}.
 */
public class AuditLogsController extends BasePageController {

    private static final int FETCH_SIZE = 500;

    private final AuditService auditService = new AuditServiceImpl(new AuditLogDAOImpl());

    @FXML private AuditLogTableController auditLogTableController;
    @FXML private SortBarController sortBarController;

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> actionFilter;
    @FXML private DatePicker   fromDatePicker;
    @FXML private DatePicker   toDatePicker;
    @FXML private Button       exportBtn;

    private List<AuditLogDTO> auditLogs = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.AUDIT_LOGS);

        actionFilter.getItems().addAll("All Actions", "CREATE", "READ", "UPDATE", "DELETE");
        actionFilter.setValue("All Actions");

        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        actionFilter.setOnAction(e -> applyFilter());
        FxFormValidator.attachDateRange(fromDatePicker, toDatePicker, null);
        exportBtn.setOnAction(e -> withSpinner(exportBtn, this::exportCsv));

        if (sortBarController != null) {
            sortBarController.setOnSort((field, asc) -> auditLogTableController.applySort(field, asc));
            sortBarController.addOptions(auditLogTableController.getSortOptionLabels());
        }

        refreshTable();
    }

    private void refreshTable() {
        try {
            auditLogs = auditService.findAll(CursorPagination.firstPage(FETCH_SIZE)).getItems();
            auditLogTableController.setItems(auditLogs);
        } catch (Exception e) {
            toastError("Failed to load audit logs: " + e.getMessage());
        }
    }

    /** Combines the free-text search with the action dropdown into the single filter query
     *  the shared {@code PaginatedTableController.filter(String)} predicate supports. */
    private void applyFilter() {
        String query = searchField.getText();
        String action = actionFilter.getValue();
        if ((query == null || query.isBlank()) && action != null && !"All Actions".equals(action)) {
            query = action;
        }
        auditLogTableController.filter(query);
    }

    private void exportCsv() {
        try {
            if (auditLogs.isEmpty()) {
                toastError("No audit logs available to export.");
                return;
            }

            List<AuditLogDTO> source = chooseExportSource();
            if (source == null || source.isEmpty()) {
                return;
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (AuditLogDTO log : source) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("log_id", log.getLogId());
                row.put("user_id", log.getUserId());
                row.put("action", log.getAction());
                row.put("table_affected", log.getTableAffected());
                row.put("record_id", log.getRecordId());
                row.put("created_at", log.getCreatedAt());
                rows.add(row);
            }

            boolean saved = CsvUiIO.exportRows(exportBtn.getScene().getWindow(), "audit-logs.csv", rows);
            if (saved) {
                toastSuccess("Audit logs exported successfully.");
            }
        } catch (Exception e) {
            toastError("Failed to export audit logs: " + e.getMessage());
        }
    }

    private List<AuditLogDTO> chooseExportSource() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("All loaded rows", "All loaded rows", "Current table view");
        dialog.setTitle("Export Audit Logs");
        dialog.setHeaderText("Choose what to export");
        dialog.setContentText("Export scope:");
        String choice = dialog.showAndWait().orElse(null);
        if (choice == null) {
            return List.of();
        }
        if ("Current table view".equals(choice)) {
            return new ArrayList<>(auditLogTableController.getTable().getItems());
        }
        return auditLogs;
    }
}
