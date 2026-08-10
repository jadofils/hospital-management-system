package hospital.management.pages.status;

import hospital.management.backend.config.AppConfig;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dao.auth.UserSessionDAOImpl;
import hospital.management.backend.dao.log.AuditLogDAOImpl;
import hospital.management.backend.service.auth.AuthServiceImpl;
import hospital.management.backend.service.auth.interfaces.AuthService;
import hospital.management.backend.service.maintenance.MaintenanceGate;
import hospital.management.backend.service.maintenance.MaintenanceMode;
import hospital.management.backend.service.maintenance.MaintenanceModeStore;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.enums.PageRoute;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Shown instead of the Dashboard right after login when {@link MaintenanceGate}
 * determines the current user is blocked (either a blanket maintenance switch
 * or a standing per-user revoke — see {@link MaintenanceMode}). Renders one of
 * three data-driven variants picked by the admin on the Developer Dashboard's
 * Maintenance tab; the copy/icon differ, the block/allow decision does not.
 */
public class SystemStatusController {

    private final AuthService authService = new AuthServiceImpl(
        new UserDAOImpl(), new UserSessionDAOImpl(), new UserRoleDAOImpl(),
        new RoleDAOImpl(), new AuditLogDAOImpl());

    @FXML private FontIcon statusIcon;
    @FXML private Label statusTitle;
    @FXML private Label statusMessage;
    @FXML private Button retryBtn;
    @FXML private Button logoutBtn;

    public void initialize() {
        MaintenanceMode mode = MaintenanceModeStore.load();
        applyVariant(mode);

        retryBtn.setOnAction(e -> retry());
        logoutBtn.setOnAction(e -> logout());
    }

    private void applyVariant(MaintenanceMode mode) {
        switch (mode.getStatusPage()) {
            case ERROR_502 -> {
                statusIcon.setIconLiteral("fas-plug");
                statusTitle.setText("502 Bad Gateway");
            }
            case ERROR_503 -> {
                statusIcon.setIconLiteral("fas-exclamation-triangle");
                statusTitle.setText("503 Service Unavailable");
            }
            default -> {
                statusIcon.setIconLiteral("fas-tools");
                statusTitle.setText("Under Maintenance");
            }
        }
        statusMessage.setText(mode.getMessage());
    }

    /** Re-checks the gate; if no longer blocked, proceeds into the real Dashboard. */
    private void retry() {
        try {
            boolean stillBlocked = MaintenanceGate.isBlocked(
                SessionManager.getCurrentUserId(), SessionManager.getCurrentRole());
            if (!stillBlocked) {
                navigateTo(PageRoute.DASHBOARD.getFxmlPath());
                return;
            }
        } catch (Exception ignored) {
            // not logged in any more — fall through to the same "still blocked" message
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Still Unavailable");
        alert.setHeaderText(null);
        alert.setContentText("The system is still under maintenance. Please try again shortly.");
        alert.initOwner(retryBtn.getScene().getWindow());
        alert.showAndWait();
    }

    private void logout() {
        String sessionId = SessionManager.peekCurrentSessionId();
        SessionManager.logout();

        if (sessionId != null) {
            AsyncJobRunner.submit(() -> {
                authService.logout(sessionId);
                return Boolean.TRUE;
            }, ok -> {}, err -> { /* best-effort — local session is already cleared */ });
        }
        navigateTo(PageRoute.HOME.getFxmlPath());
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = retryBtn.getScene();
            Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
            newScene.getStylesheets().add(
                getClass().getResource(AppConfig.CSS_PATH).toExternalForm());
            ((Stage) scene.getWindow()).setScene(newScene);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to load page: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
