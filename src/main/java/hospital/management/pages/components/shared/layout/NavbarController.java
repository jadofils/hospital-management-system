package hospital.management.pages.components.shared.layout;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.security.PermissionGate;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dto.notification.NotificationDTO;
import hospital.management.backend.dto.notification.NotificationTextFormatter;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.service.notification.NotificationService;
import hospital.management.backend.service.notification.NotificationServiceImpl;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.enums.PageRoute;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.util.List;

public class NavbarController {

    private static final AppLogger logger = AppLogger.getLogger(NavbarController.class);

    @FXML private Button dashboardBtn;
    @FXML private Button profileBtn;
    @FXML private Button notificationBellBtn;
    @FXML private Label  notifBadge;

    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    private final NotificationService notificationService =
        new NotificationServiceImpl(new UserServiceImpl(new UserDAOImpl()));

    private Popup notificationPopup;

    @FXML
    private void initialize() {
        loadCurrentUserName();
        refreshUnreadBadge();
    }

    @FXML private void handleDashboard()    { navigate(PageRoute.DASHBOARD); }
    @FXML private void handlePatients()     { navigate(PageRoute.PATIENTS); }
    @FXML private void handleAppointments() { navigate(PageRoute.APPOINTMENTS); }
    @FXML private void handleBilling()      { navigate(PageRoute.BILLING); }
    @FXML private void handleProfile()      { navigate(PageRoute.PROFILE); }

    private void loadCurrentUserName() {
        if (profileBtn == null) return;

        new Thread(() -> {
            try {
                String userId = SessionManager.getCurrentUserId();
                UserDTO user = userService.findById(userId);
                String displayName = user.getUsername();

                // Try to get a more friendly name if available
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String[] parts = user.getEmail().split("@");
                    if (parts.length > 0) {
                        String localPart = parts[0];
                        // Capitalize first letter
                        displayName = localPart.substring(0, 1).toUpperCase() + localPart.substring(1);
                    }
                }

                final String finalName = displayName;
                Platform.runLater(() -> profileBtn.setText(finalName));
            } catch (Exception e) {
                // Fallback to username from session if user lookup fails
                try {
                    String username = SessionManager.getCurrentUsername();
                    Platform.runLater(() -> profileBtn.setText(username));
                } catch (Exception ex) {
                    // Keep default text if all else fails
                }
            }
        }).start();
    }

    // ── Notifications ─────────────────────────────────────────────────────

    @FXML
    private void handleNotifications() {
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.hide();
            notificationPopup = null;
            return;
        }
        if (!SessionManager.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Log in to see your notifications.");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        AsyncJobRunner.submit(
            () -> notificationService.listForUser(SessionManager.getCurrentUserId(), 20),
            this::showNotificationPopup,
            ex -> logger.warn("Failed to load notifications: " + ex.getMessage())
        );
    }

    private void showNotificationPopup(List<NotificationDTO> notifications) {
        VBox content = new VBox(6);
        content.getStyleClass().add("navbar-notif-popup");
        content.setPadding(new Insets(8));

        HBox header = new HBox();
        header.setSpacing(8);
        Label title = new Label("Notifications");
        title.getStyleClass().add("navbar-notif-popup-header");
        HBox.setHgrow(title, Priority.ALWAYS);
        Button markAllBtn = new Button("Mark all read");
        markAllBtn.getStyleClass().add("rs-mark-all-read-btn");
        markAllBtn.setOnAction(e -> {
            AsyncJobRunner.submit(
                () -> { notificationService.markAllAsRead(SessionManager.getCurrentUserId()); return null; },
                ignored -> {
                    refreshUnreadBadge();
                    if (notificationPopup != null) notificationPopup.hide();
                    handleNotifications(); // reopens with the now-all-read list
                },
                ex -> logger.warn("Failed to mark all notifications read: " + ex.getMessage())
            );
        });
        header.getChildren().addAll(title, markAllBtn);
        content.getChildren().add(header);

        if (notifications == null || notifications.isEmpty()) {
            Label empty = new Label("No notifications yet.");
            empty.getStyleClass().add("navbar-notif-empty");
            content.getChildren().add(empty);
        } else {
            for (NotificationDTO dto : notifications) {
                content.getChildren().add(buildPopupItem(dto));
            }
        }

        notificationPopup = new Popup();
        notificationPopup.setAutoHide(true);
        notificationPopup.getContent().add(content);

        Node anchor = notificationBellBtn;
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        notificationPopup.show(anchor.getScene().getWindow(),
            bounds.getMinX() - 260, bounds.getMaxY() + 6);
    }

    private Label buildPopupItem(NotificationDTO dto) {
        String timestamp = NotificationTextFormatter.formatTimestamp(dto);
        Label item = new Label(NotificationTextFormatter.describe(dto)
            + (timestamp.isBlank() ? "" : "  (" + timestamp + ")"));
        item.getStyleClass().add(dto.isRead() ? "navbar-notif-item-read" : "navbar-notif-item");
        item.setWrapText(true);
        item.setMaxWidth(300);
        item.setOnMouseClicked(e -> {
            if (dto.isRead()) return;
            String userId = SessionManager.getCurrentUserId();
            AsyncJobRunner.submit(
                () -> { notificationService.markAsRead(dto.getId(), userId); return null; },
                ignored -> {
                    refreshUnreadBadge();
                    if (notificationPopup != null) notificationPopup.hide();
                    handleNotifications(); // reopen, now reflecting the updated read state
                },
                ex -> logger.warn("Failed to mark notification read: " + ex.getMessage())
            );
        });
        return item;
    }

    private void refreshUnreadBadge() {
        if (notifBadge == null || !SessionManager.isLoggedIn()) return;
        AsyncJobRunner.submit(
            () -> notificationService.countUnreadForUser(SessionManager.getCurrentUserId()),
            count -> {
                boolean any = count != null && count > 0;
                notifBadge.setText(any ? String.valueOf(count) : "");
                notifBadge.setVisible(any);
                notifBadge.setManaged(any);
            },
            ex -> logger.warn("Failed to load unread count: " + ex.getMessage())
        );
    }

    private void navigate(PageRoute route) {
        if (!PermissionGate.isAllowed(route)) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "You don't have permission to access this page.");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(route.getFxmlPath()));
            Parent root = loader.load();
            Scene scene = profileBtn.getScene();
            Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
            newScene.getStylesheets().add(
                getClass().getResource("/hospital/management/css/global.css").toExternalForm()
            );
            ((Stage) scene.getWindow()).setScene(newScene);
        } catch (Exception e) {
            System.err.println("Navigation to " + route.getFxmlPath() + " failed: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Couldn't open that page. Please try again.");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }
}
