package hospital.management.pages.components.shared.layout;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dto.notification.NotificationDTO;
import hospital.management.backend.dto.notification.NotificationTextFormatter;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.notification.NotificationService;
import hospital.management.backend.service.notification.NotificationServiceImpl;
import hospital.management.backend.utils.listeners.AppEvent;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import javafx.scene.control.Button;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controller for the collapsible right sidebar.
 *
 * The sidebar has three panels:
 *  1. Notifications  — persisted, per-logged-in-user (via NotificationService.listForUser)
 *  2. Quick Info     — context-sensitive; replaced per page via setQuickInfoContent()
 *  3. Recent Activity — append-only, app-wide feed of timestamped actions (pushed by application events)
 *
 * The whole content area collapses/expands via the strip toggle button.
 */
public class RightSidebarController {

    private static final AppLogger logger = AppLogger.getLogger(RightSidebarController.class);

    /**
     * Every business event worth surfacing in the app-wide Recent Activity feed —
     * the same breadth {@code NotificationEventListener} turns into persisted
     * notifications, minus the pure-infra ones (logging, session lifecycle,
     * scheduled ops) that were deliberately excluded there too. One label per
     * event keeps this a plain lookup instead of ~35 hand-written subscribe calls.
     */
    private static final Map<AppEventType, String> ACTIVITY_LABELS = Map.ofEntries(
        Map.entry(AppEventType.APPOINTMENT_BOOKED, "Appointment booked"),
        Map.entry(AppEventType.APPOINTMENT_UPDATED, "Appointment updated"),
        Map.entry(AppEventType.APPOINTMENT_CANCELLED, "Appointment cancelled"),
        Map.entry(AppEventType.PRESCRIPTION_CREATED, "Prescription created"),
        Map.entry(AppEventType.LAB_RESULT_READY, "Lab result ready"),
        Map.entry(AppEventType.MEDICAL_RECORD_CREATED, "Medical record created"),
        Map.entry(AppEventType.MEDICAL_RECORD_UPDATED, "Medical record updated"),
        Map.entry(AppEventType.INVOICE_CREATED, "Invoice created"),
        Map.entry(AppEventType.INVOICE_UPDATED, "Invoice updated"),
        Map.entry(AppEventType.INVOICE_PAID, "Invoice paid"),
        Map.entry(AppEventType.LAB_ORDER_CREATED, "Lab order created"),
        Map.entry(AppEventType.LAB_ORDER_UPDATED, "Lab order updated"),
        Map.entry(AppEventType.VITAL_SIGN_RECORDED, "Vital sign recorded"),
        Map.entry(AppEventType.PATIENT_FEEDBACK_SUBMITTED, "Patient feedback received"),
        Map.entry(AppEventType.REFERRAL_CREATED, "Referral created"),
        Map.entry(AppEventType.REFERRAL_UPDATED, "Referral updated"),
        Map.entry(AppEventType.DOCTOR_UPDATED, "Doctor profile updated"),
        Map.entry(AppEventType.DOCTOR_DELETED, "Doctor removed"),
        Map.entry(AppEventType.DOCTOR_SCHEDULE_UPDATED, "Doctor schedule updated"),
        Map.entry(AppEventType.PATIENT_CREATED, "Patient created"),
        Map.entry(AppEventType.PATIENT_UPDATED, "Patient updated"),
        Map.entry(AppEventType.PATIENT_DELETED, "Patient removed"),
        Map.entry(AppEventType.PATIENT_ALLERGY_ADDED, "Patient allergy added"),
        Map.entry(AppEventType.PATIENT_ALLERGY_REMOVED, "Patient allergy removed"),
        Map.entry(AppEventType.MEDICATION_CREATED, "Medication added"),
        Map.entry(AppEventType.INVENTORY_UPDATED, "Inventory updated"),
        Map.entry(AppEventType.INVENTORY_LOW_STOCK, "Inventory low stock"),
        Map.entry(AppEventType.USER_CREATED, "User created"),
        Map.entry(AppEventType.USER_UPDATED, "User updated"),
        Map.entry(AppEventType.USER_DELETED, "User removed"),
        Map.entry(AppEventType.ROLE_CREATED, "Role created"),
        Map.entry(AppEventType.ROLE_UPDATED, "Role updated"),
        Map.entry(AppEventType.ROLE_DELETED, "Role removed"),
        Map.entry(AppEventType.PERMISSION_CREATED, "Permission created"),
        Map.entry(AppEventType.PERMISSION_DELETED, "Permission removed"),
        Map.entry(AppEventType.DEPARTMENT_CREATED, "Department created"),
        Map.entry(AppEventType.DEPARTMENT_UPDATED, "Department updated"),
        Map.entry(AppEventType.DEPARTMENT_DELETED, "Department removed"),
        Map.entry(AppEventType.DB_OBJECT_CHANGED, "Database object changed"),
        Map.entry(AppEventType.MAINTENANCE_ACCESS_CHANGED, "Maintenance access changed"),
        Map.entry(AppEventType.BACKUP_COMPLETED, "Backup completed"),
        Map.entry(AppEventType.BACKUP_FAILED, "Backup failed")
    );

    private final NotificationService notificationService =
        new NotificationServiceImpl(new UserServiceImpl(new UserDAOImpl()));

    @FXML private HBox  rightSidebarRoot;
    @FXML private VBox  rightSidebarContent;
    @FXML private FontIcon toggleIcon;

    @FXML private VBox  notificationList;
    @FXML private Label unreadBadge;
    @FXML private Label emptyNotifLabel;
    @FXML private Button markAllReadBtn;

    @FXML private VBox quickInfoContent;
    @FXML private VBox activityFeed;

    private boolean expanded = false;   // starts collapsed — expand on demand

    public void initialize() {
        // Start collapsed; content hidden — strip toggle is always visible
        collapse();

        // Recent Activity stays a global broadcast feed — that's a different,
        // correctly app-wide concern from per-user Notifications below.
        ACTIVITY_LABELS.forEach((type, label) ->
            EventBus.subscribe(type, e -> logActivity(label + ": " + payloadId(e))));

        if (markAllReadBtn != null) markAllReadBtn.setOnAction(e -> markAllRead());
        refreshPersistedNotifications();
    }

    private String payloadId(AppEvent e) {
        return e.getPayload() == null ? "" : e.getPayload().toString();
    }

    // ── Collapse / expand ─────────────────────────────────────────────────

    @FXML
    private void handleToggle() {
        if (expanded) collapse(); else expand();
    }

    public void expand() {
        expanded = true;
        rightSidebarContent.setVisible(true);
        rightSidebarContent.setManaged(true);
        toggleIcon.setIconLiteral("fas-chevron-right");
        refreshPersistedNotifications();
    }

    public void collapse() {
        expanded = false;
        rightSidebarContent.setVisible(false);
        rightSidebarContent.setManaged(false);
        toggleIcon.setIconLiteral("fas-chevron-left");
    }

    public boolean isExpanded() { return expanded; }

    // ── Notifications ─────────────────────────────────────────────────────
    // Persisted, per-user — fetched via NotificationService.listForUser for
    // whoever is currently logged in. Refreshed on page load (initialize())
    // and whenever the bell is opened (expand()); deliberately NOT on a
    // persistent EventBus subscription — NavbarController.navigate() rebuilds
    // this whole controller on every page change with no teardown hook, so a
    // live subscription here would leak one more listener per navigation on
    // top of the pre-existing leak in the Recent Activity subscriptions above.

    private void refreshPersistedNotifications() {
        if (!SessionManager.isLoggedIn()) return;
        AsyncJobRunner.submit(
            () -> notificationService.listForUser(SessionManager.getCurrentUserId(), 20),
            this::renderPersistedNotifications,
            ex -> logger.warn("Failed to load notifications: " + ex.getMessage())
        );
    }

    private void renderPersistedNotifications(List<NotificationDTO> notifications) {
        notificationList.getChildren().clear();
        boolean empty = notifications == null || notifications.isEmpty();
        emptyNotifLabel.setVisible(empty);
        emptyNotifLabel.setManaged(empty);

        long unread = 0;
        if (!empty) {
            for (NotificationDTO dto : notifications) {
                notificationList.getChildren().add(buildNotificationItem(dto));
                if (!dto.isRead()) unread++;
            }
        }
        unreadBadge.setText(String.valueOf(unread));
        if (markAllReadBtn != null) markAllReadBtn.setDisable(unread == 0);
    }

    private Label buildNotificationItem(NotificationDTO dto) {
        String timestamp = NotificationTextFormatter.formatTimestamp(dto);
        Label item = new Label("● " + NotificationTextFormatter.describe(dto)
            + (timestamp.isBlank() ? "" : "  (" + timestamp + ")"));
        item.getStyleClass().add(dto.isRead() ? "rs-notification-item-read" : "rs-notification-item");
        item.setWrapText(true);
        // Click to mark read — a no-op (fast, idempotent UPDATE ... WHERE read_at IS NULL)
        // if it's already read.
        item.setOnMouseClicked(e -> markOneRead(dto));
        return item;
    }

    private void markOneRead(NotificationDTO dto) {
        if (dto.isRead() || !SessionManager.isLoggedIn()) return;
        String userId = SessionManager.getCurrentUserId();
        AsyncJobRunner.submit(
            () -> { notificationService.markAsRead(dto.getId(), userId); return null; },
            ignored -> refreshPersistedNotifications(),
            ex -> logger.warn("Failed to mark notification read: " + ex.getMessage())
        );
    }

    private void markAllRead() {
        if (!SessionManager.isLoggedIn()) return;
        String userId = SessionManager.getCurrentUserId();
        AsyncJobRunner.submit(
            () -> { notificationService.markAllAsRead(userId); return null; },
            ignored -> refreshPersistedNotifications(),
            ex -> logger.warn("Failed to mark all notifications read: " + ex.getMessage())
        );
    }

    // ── Quick info panel ──────────────────────────────────────────────────

    /** Replace the quick-info panel content with page-specific nodes. */
    public void setQuickInfoContent(List<Node> nodes) {
        quickInfoContent.getChildren().setAll(nodes);
    }

    public void clearQuickInfo() {
        quickInfoContent.getChildren().clear();
    }

    // ── Activity feed ──────────────────────────────────────────────────────

    public void logActivity(String action) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        HBox row = new HBox(8);
        row.getStyleClass().add("rs-activity-row");

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("rs-activity-time");

        Label actLabel = new Label(action);
        actLabel.getStyleClass().add("rs-activity-text");
        actLabel.setWrapText(true);
        HBox.setHgrow(actLabel, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(timeLabel, actLabel);
        activityFeed.getChildren().add(0, row);

        // Keep feed bounded to last 50 entries
        if (activityFeed.getChildren().size() > 50) {
            activityFeed.getChildren().remove(50, activityFeed.getChildren().size());
        }
    }
}