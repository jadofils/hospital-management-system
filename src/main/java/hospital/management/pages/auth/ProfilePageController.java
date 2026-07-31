package hospital.management.pages.auth;

import hospital.management.pages.BasePageController;
import hospital.management.backend.model.user.UserSession;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.auth.UserSessionTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProfilePageController extends BasePageController {

    // Summary labels
    @FXML private Label displayNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label emailLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Button changePhotoBtn;

    // Edit form
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private Button    saveProfileBtn;

    // Password change
    @FXML private PasswordField currentPassField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;
    @FXML private Label         passValidationMsg;
    @FXML private Button        changePassBtn;

    // Sessions table
    @FXML private UserSessionTableController sessionsTableController;
    @FXML private Button revokeAllBtn;

    private final List<UserSession> sessions = new ArrayList<>();

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PROFILE);

        seedSessions();
        refreshSessionsTable();

        sessionsTableController.setOnRevoke(this::confirmRevokeSession);

        saveProfileBtn.setOnAction(e -> handleSaveProfile());
        changePassBtn.setOnAction(e -> handleChangePassword());
        revokeAllBtn.setOnAction(e -> confirmRevokeAllSessions());
        changePhotoBtn.setOnAction(e -> toast("Photo upload not yet implemented.", NotificationType.INFO));
    }

    private void seedSessions() {
        LocalDateTime now = LocalDateTime.now();
        UserSession current = new UserSession();
        current.setSessionId(UUID.randomUUID().toString());
        current.setLoginAt(now.minusHours(2));
        current.setExpiresAt(now.plusHours(22));
        current.setIpAddress("127.0.0.1");
        current.setUserAgent("This device");
        current.setIsActive(true);
        sessions.add(current);
    }

    private void refreshSessionsTable() {
        sessionsTableController.setItems(sessions);
    }

    private void confirmRevokeAllSessions() {
        confirm("Revoke All Sessions",
                "This will sign you out of every other device. Continue?",
                () -> {
                    UserSession current = sessions.isEmpty() ? null : sessions.get(0);
                    sessions.clear();
                    if (current != null) sessions.add(current);
                    refreshSessionsTable();
                    toastSuccess("All other sessions revoked.");
                });
    }

    private void confirmRevokeSession(UserSession session) {
        confirm("Revoke Session",
                "Sign out this device?",
                () -> {
                    sessions.remove(session);
                    refreshSessionsTable();
                    toastSuccess("Session revoked.");
                });
    }

    private void handleSaveProfile() {
        if (usernameField.getText().isBlank() || emailField.getText().isBlank()) {
            return;
        }
        toastSuccess("Profile updated.");
    }

    private void handleChangePassword() {
        if (!newPassField.getText().equals(confirmPassField.getText())) {
            passValidationMsg.setText("Passwords do not match.");
            return;
        }
        if (newPassField.getText().length() < 8) {
            passValidationMsg.setText("Password must be at least 8 characters.");
            return;
        }
        passValidationMsg.setText("");
        toastSuccess("Password changed.");
    }
}
