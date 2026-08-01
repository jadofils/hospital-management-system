package hospital.management.pages.auth;

import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.RolePermissionDAOImpl;
import hospital.management.backend.dao.auth.PermissionDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dao.auth.UserSessionDAOImpl;
import hospital.management.backend.dao.log.AuditLogDAOImpl;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.dto.auth.UpdateUserDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dto.auth.UserSessionDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.auth.AuthServiceImpl;
import hospital.management.backend.service.auth.RoleServiceImpl;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.AuthService;
import hospital.management.backend.service.auth.interfaces.RoleService;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.pages.BasePageController;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import hospital.management.pages.components.auth.UserSessionTableController;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProfilePageController extends BasePageController {

    private static final DateTimeFormatter MEMBER_SINCE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    private final RoleService roleService = new RoleServiceImpl(
        new RoleDAOImpl(), new UserRoleDAOImpl(), new RolePermissionDAOImpl(), new PermissionDAOImpl());
    private final AuthService authService = new AuthServiceImpl(
        new UserDAOImpl(), new UserSessionDAOImpl(), new UserRoleDAOImpl(),
        new RoleDAOImpl(), new AuditLogDAOImpl());

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

    private UserDTO currentUser;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.PROFILE);

        // The backend has no username-change endpoint (UpdateUserDTO only carries
        // email/isActive) — disable the field rather than pretend an edit here persists.
        usernameField.setDisable(true);

        sessionsTableController.setOnRevoke(this::confirmRevokeSession);
        saveProfileBtn.setOnAction(e -> handleSaveProfile());
        changePassBtn.setOnAction(e -> handleChangePassword());
        revokeAllBtn.setOnAction(e -> confirmRevokeAllSessions());
        changePhotoBtn.setOnAction(e -> toast("Photo upload not yet implemented.", NotificationType.INFO));

        loadProfile();
    }

    private void loadProfile() {
        try {
            String userId = SessionManager.getCurrentUserId();
            currentUser = userService.findById(userId);

            List<RoleDTO> roles = roleService.findRolesForUser(userId);
            String roleName = roles.isEmpty() ? "—" : roles.get(0).getRoleName();

            displayNameLabel.setText(currentUser.getUsername());
            usernameLabel.setText("@" + currentUser.getUsername());
            roleLabel.setText("Role: " + roleName);
            emailLabel.setText("Email: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "—"));
            memberSinceLabel.setText("Member since: " + (currentUser.getCreatedAt() != null
                ? currentUser.getCreatedAt().format(MEMBER_SINCE_FMT) : "—"));

            usernameField.setText(currentUser.getUsername());
            emailField.setText(currentUser.getEmail());

            refreshSessions();
        } catch (Exception e) {
            toastError("Failed to load profile: " + e.getMessage());
        }
    }

    private void refreshSessions() {
        try {
            List<UserSessionDTO> sessions = authService.findActiveSessions(currentUser.getUserId());
            sessionsTableController.setItems(sessions);
        } catch (Exception e) {
            toastError("Failed to load sessions: " + e.getMessage());
        }
    }

    private void confirmRevokeAllSessions() {
        confirm("Revoke All Sessions",
                "This will sign you out of every device, including this one, at next check. Continue?",
                () -> {
                    try {
                        authService.logoutAllSessions(currentUser.getUserId());
                        refreshSessions();
                        toastSuccess("All sessions revoked.");
                    } catch (Exception e) {
                        toastError("Failed to revoke sessions: " + e.getMessage());
                    }
                });
    }

    private void confirmRevokeSession(UserSessionDTO session) {
        confirm("Revoke Session",
                "Sign out this device?",
                () -> {
                    try {
                        authService.logout(session.getSessionId());
                        refreshSessions();
                        toastSuccess("Session revoked.");
                    } catch (Exception e) {
                        toastError("Failed to revoke session: " + e.getMessage());
                    }
                });
    }

    private void handleSaveProfile() {
        String email = emailField.getText().trim();
        if (email.isBlank()) return;
        withSpinner(saveProfileBtn, () -> {
            try {
                UpdateUserDTO dto = new UpdateUserDTO(currentUser.getUserId(), email, currentUser.getIsActive());
                currentUser = userService.update(dto);
                emailLabel.setText("Email: " + currentUser.getEmail());
                toastSuccess("Profile updated.");
            } catch (AppException ex) {
                toastError(ex.getMessage());
            } catch (Exception e) {
                toastError("Failed to update profile: " + e.getMessage());
            }
        });
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
        withSpinner(changePassBtn, () -> {
            try {
                authService.changePassword(currentUser.getUserId(), currentPassField.getText(), newPassField.getText());
                passValidationMsg.setText("");
                currentPassField.clear();
                newPassField.clear();
                confirmPassField.clear();
                toastSuccess("Password changed.");
            } catch (AppException ex) {
                passValidationMsg.setText(ex.getMessage());
                toastError(ex.getMessage());
            } catch (Exception e) {
                passValidationMsg.setText("Failed to change password.");
                toastError("Failed to change password: " + e.getMessage());
            }
        });
    }
}
