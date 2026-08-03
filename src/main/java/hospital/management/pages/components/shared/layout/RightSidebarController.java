package hospital.management.pages.components.shared.layout;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the collapsible right sidebar.
 *
 * The sidebar has three panels:
 *  1. Notifications  — pushed by application events
 *  2. Quick Info     — context-sensitive; replaced per page via setQuickInfoContent()
 *  3. Recent Activity — append-only feed of timestamped actions
 *
 * The whole content area collapses/expands via the strip toggle button.
 */
public class RightSidebarController {

    @FXML private HBox  rightSidebarRoot;
    @FXML private VBox  rightSidebarContent;
    @FXML private FontIcon toggleIcon;

    @FXML private VBox  notificationList;
    @FXML private Label unreadBadge;
    @FXML private Label emptyNotifLabel;

    @FXML private VBox quickInfoContent;
    @FXML private VBox activityFeed;

    private boolean expanded = false;   // starts collapsed — expand on demand
    private int     unreadCount = 0;

    public void initialize() {
        // Start collapsed; content hidden — strip toggle is always visible
        collapse();
        // Subscribe to important app events so notifications and activity feed show up
        hospital.management.backend.utils.listeners.EventBus.subscribe(AppEventType.PATIENT_FEEDBACK_SUBMITTED, e -> {
            String id = e.getPayload() == null ? "" : e.getPayload().toString();
            pushNotification("New patient feedback: " + id);
            logActivity("New patient feedback received: " + id);
        });
        hospital.management.backend.utils.listeners.EventBus.subscribe(AppEventType.APPOINTMENT_BOOKED, e -> {
            String id = e.getPayload() == null ? "" : e.getPayload().toString();
            pushNotification("Appointment booked: " + id);
            logActivity("Appointment booked: " + id);
        });
        hospital.management.backend.utils.listeners.EventBus.subscribe(AppEventType.PATIENT_CREATED, e -> {
            String id = e.getPayload() == null ? "" : e.getPayload().toString();
            pushNotification("Patient created: " + id);
            logActivity("Patient created: " + id);
        });
        hospital.management.backend.utils.listeners.EventBus.subscribe(AppEventType.APPOINTMENT_UPDATED, e -> {
            String id = e.getPayload() == null ? "" : e.getPayload().toString();
            pushNotification("Appointment updated: " + id);
            logActivity("Appointment updated: " + id);
        });
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
    }

    public void collapse() {
        expanded = false;
        rightSidebarContent.setVisible(false);
        rightSidebarContent.setManaged(false);
        toggleIcon.setIconLiteral("fas-chevron-left");
    }

    public boolean isExpanded() { return expanded; }

    // ── Notifications ──────────────────────────────────────────────────────

    public void pushNotification(String message) {
        emptyNotifLabel.setVisible(false);
        emptyNotifLabel.setManaged(false);

        Label item = new Label("● " + message);
        item.getStyleClass().add("rs-notification-item");
        item.setWrapText(true);
        notificationList.getChildren().add(0, item);

        unreadCount++;
        unreadBadge.setText(String.valueOf(unreadCount));
    }

    public void clearNotifications() {
        notificationList.getChildren().clear();
        unreadCount = 0;
        unreadBadge.setText("0");
        emptyNotifLabel.setVisible(true);
        emptyNotifLabel.setManaged(true);
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