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
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.pages.BasePageController;
import hospital.management.backend.config.AppConfig;
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
    @FXML private javafx.scene.image.ImageView avatarImage;

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
        changePhotoBtn.setOnAction(e -> handleChangePhoto());

        // Real-time email validation
        FxFormValidator.attachEmail(emailField, null);

        // Real-time password strength and confirmation matching
        FxFormValidator.attachPasswordStrength(newPassField, passValidationMsg);
        FxFormValidator.attachPasswordMatch(newPassField, confirmPassField, passValidationMsg);

        // Enter key on confirm field submits password change
        confirmPassField.setOnAction(e -> handleChangePassword());

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
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isBlank()) {
            FxFormValidator.applyStyle(emailField, false);
            toastError("Email is required.");
            return;
        }
        if (!ValidatorUtils.isValidEmail(email)) {
            FxFormValidator.applyStyle(emailField, false);
            toastError("Please enter a valid email address.");
            return;
        }
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
        String currentPass = currentPassField.getText();
        String newPass = newPassField.getText();
        String confirmPass = confirmPassField.getText();

        if (currentPass == null || currentPass.isBlank()) {
            passValidationMsg.getStyleClass().removeAll("text-success");
            passValidationMsg.getStyleClass().add("text-danger");
            passValidationMsg.setText("Current password is required.");
            FxFormValidator.applyStyle(currentPassField, false);
            return;
        }
        if (newPass == null || newPass.length() < 8) {
            passValidationMsg.getStyleClass().removeAll("text-success");
            passValidationMsg.getStyleClass().add("text-danger");
            passValidationMsg.setText("New password must be at least 8 characters.");
            FxFormValidator.applyStyle(newPassField, false);
            return;
        }
        if (!ValidatorUtils.isPasswordStrong(newPass)) {
            passValidationMsg.getStyleClass().removeAll("text-success");
            passValidationMsg.getStyleClass().add("text-danger");
            passValidationMsg.setText("Password must contain uppercase, lowercase, digit, and special character.");
            FxFormValidator.applyStyle(newPassField, false);
            return;
        }
        if (!newPass.equals(confirmPass)) {
            passValidationMsg.getStyleClass().removeAll("text-success");
            passValidationMsg.getStyleClass().add("text-danger");
            passValidationMsg.setText("Passwords do not match.");
            FxFormValidator.applyStyle(confirmPassField, false);
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

    private void handleChangePhoto() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Select profile image");
        chooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        java.io.File f = chooser.showOpenDialog(changePhotoBtn.getScene().getWindow());
        if (f == null) return;
        if (f.length() > AppConfig.getMaxUploadSizeBytes()) {
            toastError("File exceeds maximum allowed size.");
            return;
        }

        withSpinner(changePhotoBtn, () -> {
            try {
                com.cloudinary.Cloudinary cloud = hospital.management.backend.config.CloudinaryConfig.get();
                java.util.Map uploadResult = cloud.uploader().upload(f, java.util.Collections.emptyMap());
                String url = (String) uploadResult.get("secure_url");
                if (url != null && !url.isBlank()) {
                    avatarImage.setImage(new javafx.scene.image.Image(url, true));
                    toastSuccess("Profile photo uploaded.");
                } else {
                    toastError("Upload failed: no url returned.");
                }
            } catch (Exception ex) {
                toastError("Failed to upload photo: " + ex.getMessage());
            }
        });
    }
}
