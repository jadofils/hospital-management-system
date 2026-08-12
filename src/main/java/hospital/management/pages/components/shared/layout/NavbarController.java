package hospital.management.pages.components.shared.layout;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.security.PermissionGate;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.service.notification.NotificationService;
import hospital.management.backend.service.notification.NotificationServiceImpl;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.enums.PageRoute;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class NavbarController {

    private static final AppLogger logger = AppLogger.getLogger(NavbarController.class);

    @FXML private Button dashboardBtn;
    @FXML private Button profileBtn;
    @FXML private Label  notifBadge;

    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    private final NotificationService notificationService =
        new NotificationServiceImpl(new UserServiceImpl(new UserDAOImpl()));

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
    // The right sidebar already has a full Notifications panel (persisted,
    // mark-as-read, mark-all-read) — the bell just opens/closes that panel
    // instead of duplicating it in a separate popup.

    @FXML
    private void handleNotifications() {
        if (!SessionManager.isLoggedIn()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Log in to see your notifications.");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        RightSidebarController sidebar = RightSidebarController.getActive();
        if (sidebar == null) return;
        if (sidebar.isExpanded()) {
            sidebar.collapse();
        } else {
            sidebar.expand();
        }
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
