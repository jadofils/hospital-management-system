package hospital.management.pages.auth;

import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dao.auth.UserSessionDAOImpl;
import hospital.management.backend.dao.log.AuditLogDAOImpl;
import hospital.management.backend.dto.auth.LoginRequestDTO;
import hospital.management.backend.dto.auth.LoginResponseDTO;
import hospital.management.backend.exceptions.AppException;
import hospital.management.backend.service.auth.AuthServiceImpl;
import hospital.management.backend.service.auth.interfaces.AuthService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.model.user.UserRole;
import hospital.management.backend.model.user.Role;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthPageController {

    private final AuthService authService = new AuthServiceImpl(
        new UserDAOImpl(), new UserSessionDAOImpl(), new UserRoleDAOImpl(),
        new RoleDAOImpl(), new AuditLogDAOImpl());

    // Login tab
    @FXML private TextField     loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label         loginError;
    @FXML private Button        loginBtn;
    @FXML private ComboBox<String> userDropdown;

    private final ObservableList<String> allUsers = FXCollections.observableArrayList();

    // Reset tab
    @FXML private TextField resetEmail;
    @FXML private Label     resetMessage;

    public void initialize() {
        loginError.setText("");
        resetMessage.setText("");
        // start background load of demo users for quick selection
        loadUsersForDemo();
    }

    private void loadUsersForDemo() {
        Task<PageResult<UserDTO>> t = new Task<>() {
            @Override
            protected PageResult<UserDTO> call() throws Exception {
                UserServiceImpl uService = new UserServiceImpl(new UserDAOImpl());
                return uService.findAll(CursorPagination.firstPage(100));
            }
        };

        t.setOnSucceeded(evt -> {
            PageResult<UserDTO> res = t.getValue();
            List<String> entries = new ArrayList<>();
            UserRoleDAOImpl urDao = new UserRoleDAOImpl();
            RoleDAOImpl roleDao = new RoleDAOImpl();
            try {
                for (UserDTO u : res.getItems()) {
                    List<UserRole> urs = urDao.findByUserId(u.getUserId());
                    List<String> roleNames = new ArrayList<>();
                    for (UserRole ur : urs) {
                        Optional<Role> r = roleDao.findById(ur.getRoleId());
                        r.ifPresent(role -> roleNames.add(role.getRoleName()));
                    }
                    String label = u.getUsername() + ":" + (roleNames.isEmpty() ? "User" : String.join(",", roleNames));
                    entries.add(label);
                }
            } catch (Exception ex) {
                // ignore — best-effort demo population
            }

            Platform.runLater(() -> {
                allUsers.setAll(entries);
                if (userDropdown != null) {
                    userDropdown.setItems(allUsers);
                    userDropdown.setVisibleRowCount(8);
                    userDropdown.setEditable(true);
                    setupDropdownFiltering();
                    userDropdown.setOnAction(e -> handleUserSelection());
                }
            });
        });

        t.setOnFailed(evt -> {
            // silently ignore demo load failure
        });

        Thread th = new Thread(t, "hms-demo-user-loader");
        th.setDaemon(true);
        th.start();
    }

    private void setupDropdownFiltering() {
        if (userDropdown == null) return;
        TextField editor = userDropdown.getEditor();
        editor.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV == null ? "" : newV.toLowerCase();
            if (q.isEmpty()) {
                userDropdown.setItems(allUsers);
                return;
            }
            ObservableList<String> filtered = FXCollections.observableArrayList();
            for (String s : allUsers) {
                if (s.toLowerCase().contains(q)) filtered.add(s);
            }
            userDropdown.setItems(filtered);
            // keep editor text and show popup so the user sees matches
            Platform.runLater(() -> {
                userDropdown.show();
            });
        });
    }

    private void handleUserSelection() {
        if (userDropdown == null) return;
        String value = userDropdown.getValue();
        if (value == null || value.isBlank()) return;
        // expected format: username:RoleName[,Role2]
        String username = value.split(":")[0];
        loginEmail.setText(username);
        // For quick demo, always pre-fill password with the demo password.
        loginPassword.setText("Password@12");
    }

    @FXML
    private void handleLogin() {
        String username = loginEmail.getText().trim();
        String password = loginPassword.getText();
        if (username.isEmpty() || password.isEmpty()) {
            loginError.setText("Email and password are required.");
            return;
        }
        loginError.setText("");
        setLoading(true);

        Task<LoginResponseDTO> loginTask = new Task<>() {
            @Override
            protected LoginResponseDTO call() throws Exception {
                return authService.login(new LoginRequestDTO(username, password));
            }
        };

        loginTask.setOnSucceeded(e -> {
            LoginResponseDTO response = loginTask.getValue();
            SessionManager.login(response.getToken(), response.getSessionId());
            setLoading(false);
            // Show a short success alert so the user sees immediate feedback
            try {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Signed In");
                info.setHeaderText(null);
                info.setContentText("Welcome — signing you in now.");
                info.initOwner(loginBtn.getScene().getWindow());
                info.show();
            } catch (Exception ignore) {}
            navigateToDashboard();
        });

        loginTask.setOnFailed(e -> {
            Throwable ex = loginTask.getException();
            setLoading(false);
            String msg = ex instanceof AppException ? ex.getMessage() : "Login failed. Please try again.";
            loginError.setText(msg);
            try {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Sign In Failed");
                err.setHeaderText(null);
                err.setContentText(msg);
                err.initOwner(loginBtn.getScene().getWindow());
                err.show();
            } catch (Exception ignore) {}
        });

        Thread t = new Thread(loginTask, "hms-login-thread");
        t.setDaemon(true);
        t.start();
    }

    private void setLoading(boolean loading) {
        loginBtn.setDisable(loading);
        loginEmail.setDisable(loading);
        loginPassword.setDisable(loading);

        // Preserve original graphic so it can be restored later
        if (loginBtn.getUserData() == null) {
            loginBtn.setUserData(loginBtn.getGraphic());
        }

        if (loading) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(14, 14);
            loginBtn.setGraphic(spinner);
            if (!loginBtn.getStyleClass().contains("loading")) {
                loginBtn.getStyleClass().add("loading");
            }
        } else {
            Object saved = loginBtn.getUserData();
            if (saved instanceof Node savedGraphic) {
                loginBtn.setGraphic(savedGraphic);
            } else {
                loginBtn.setGraphic(null);
            }
            loginBtn.getStyleClass().removeAll("loading");
        }
    }

    @FXML
    private void handleResetPassword() {
        String email = resetEmail.getText();
        if (email == null || email.isBlank()) {
            resetMessage.setText("Please enter your email address.");
            resetMessage.getStyleClass().removeAll("text-success");
            resetMessage.getStyleClass().add("text-danger");
            return;
        }

        resetMessage.getStyleClass().removeAll("text-danger");
        resetMessage.getStyleClass().add("text-success");
        resetMessage.setText("Sending reset email...");

        // Run in background to avoid blocking UI
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    String temp = authService.resetPasswordByEmail(email);
                    // send email with temp password
                    hospital.management.backend.dto.notification.NotificationDTO dto = new hospital.management.backend.dto.notification.NotificationDTO();
                    dto.setType("password.reset");
                    dto.setActorUserId(null);
                    dto.setRecipients(List.of()); // will email directly below
                    dto.setPayload(java.util.Map.of("tempPassword", temp, "email", email));

                    // directly send an email using MailConfig — reuse NotificationServiceImpl to avoid duplicate templates
                    hospital.management.backend.service.notification.NotificationServiceImpl notif = new hospital.management.backend.service.notification.NotificationServiceImpl(new hospital.management.backend.service.auth.UserServiceImpl(new hospital.management.backend.dao.auth.UserDAOImpl()));
                    // build a minimal notification DTO for the recipient
                    hospital.management.backend.dto.notification.NotificationDTO emailDto = new hospital.management.backend.dto.notification.NotificationDTO();
                    emailDto.setType("password.reset");
                    emailDto.setRecipients(List.of());
                    emailDto.setPayload(java.util.Map.of("tempPassword", temp));

                    // send a plain email using MailConfig directly
                    jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(hospital.management.backend.config.MailConfig.getSession());
                    msg.setFrom(new jakarta.mail.internet.InternetAddress(hospital.management.backend.config.MailConfig.getFromAddress(), hospital.management.backend.config.MailConfig.getFromName()));
                    msg.setRecipient(jakarta.mail.Message.RecipientType.TO, new jakarta.mail.internet.InternetAddress(email));
                    msg.setSubject("Password reset for Hospital Management System");
                    String body = "Your temporary password is: " + temp + "\nPlease sign in and change your password.";
                    msg.setText(body);
                    jakarta.mail.Transport.send(msg);
                } catch (Exception e) {
                    throw e;
                }
                return null;
            }
        };

        t.setOnSucceeded(evt -> {
            resetMessage.setText("Reset email sent to " + email + ". Check your inbox.");
        });
        t.setOnFailed(evt -> {
            resetMessage.getStyleClass().removeAll("text-success");
            resetMessage.getStyleClass().add("text-danger");
            resetMessage.setText("Failed to send reset email: " + t.getException().getMessage());
        });

        Thread th = new Thread(t, "hms-reset-email");
        th.setDaemon(true);
        th.start();
    }

    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/hospital/management/frontend/pages/dashboard.fxml")
            );
            Parent root = loader.load();
            Scene scene = loginEmail.getScene();
            Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
            newScene.getStylesheets().add(
                getClass().getResource("/hospital/management/css/global.css").toExternalForm()
            );
            ((Stage) scene.getWindow()).setScene(newScene);
        } catch (Exception e) {
            // Log full stacktrace for diagnostics and show a helpful message to the user
            e.printStackTrace();
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            loginError.setText("Login succeeded, but the dashboard failed to load: " + msg);
            try {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Navigation Error");
                err.setHeaderText("Failed to open dashboard");
                err.setContentText(msg + "\nSee console for details.");
                err.initOwner(loginBtn.getScene().getWindow());
                err.showAndWait();
            } catch (Exception ignore) {}
        }
    }
}
