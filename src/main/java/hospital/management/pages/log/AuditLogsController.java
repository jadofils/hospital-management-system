package hospital.management.pages.log;

import hospital.management.pages.BasePageController;
import hospital.management.backend.dao.log.AuditLogDAOImpl;
import hospital.management.backend.dto.log.AuditLogDTO;
import hospital.management.backend.service.log.AuditServiceImpl;
import hospital.management.backend.service.log.interfaces.AuditService;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.log.AuditLogTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only audit log viewer. Audit logs are immutable — there is no
 * add/edit/delete flow on this page, so it never touches the shared
 * form-dialog/confirm-modal pieces from {@link BasePageController}.
 */
public class AuditLogsController extends BasePageController {

    private static final int FETCH_SIZE = 500;

    private final AuditService auditService = new AuditServiceImpl(new AuditLogDAOImpl());

    @FXML private AuditLogTableController auditLogTableController;

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
        exportBtn.setOnAction(e -> toast("Export not yet implemented.", NotificationType.INFO));

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
}
