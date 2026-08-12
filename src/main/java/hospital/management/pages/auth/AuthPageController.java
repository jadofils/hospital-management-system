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
import hospital.management.backend.service.maintenance.MaintenanceGate;
import hospital.management.enums.PageRoute;
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
import hospital.management.backend.utils.AlgorithmUtils;
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.LoginLookupIndex;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageResult;
import java.util.ArrayList;
import java.util.List;

public class AuthPageController {

    private static final int DEMO_USERS_PAGE_SIZE = 200;
    private static final hospital.management.backend.config.AppLogger logger =
        hospital.management.backend.config.AppLogger.getLogger(AuthPageController.class);
    private static final javafx.util.Duration LOGIN_TIMEOUT = javafx.util.Duration.seconds(3);

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

    /** Guards against the filtering listener re-running while a selection is applied programmatically. */
    private boolean suppressFilter;

    // Reset tab
    @FXML private TextField resetEmail;
    @FXML private Label     resetMessage;

    public void initialize() {
        loginError.setText("");
        resetMessage.setText("");

        // Real-time validation on login fields
        FxFormValidator.attachRequired(loginEmail, loginError, "Email / username");
        loginPassword.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isEmpty()) loginError.setText("");
        });

        // Real-time email format on reset tab
        FxFormValidator.attachEmail(resetEmail, resetMessage);

        // Enter key on password triggers login
        loginPassword.setOnAction(e -> handleLogin());
        resetEmail.setOnAction(e -> handleResetPassword());

        // start background load of demo users for quick selection
        loadUsersForDemo();
    }

    private void loadUsersForDemo() {
        Task<List<UserDTO>> t = new Task<>() {
            @Override
            protected List<UserDTO> call() throws Exception {
                UserServiceImpl uService = new UserServiceImpl(new UserDAOImpl());
                List<UserDTO> all = new ArrayList<>();
                PageResult<UserDTO> page = uService.findAll(CursorPagination.firstPage(DEMO_USERS_PAGE_SIZE));
                all.addAll(page.getItems());
                while (page.hasMore() && page.getNextCursor() != null) {
                    page = uService.findAll(CursorPagination.nextPage(page.getNextCursor(), DEMO_USERS_PAGE_SIZE));
                    all.addAll(page.getItems());
                }

                // Warm the login lookup index in the background so the first login
                // resolves the user + role in memory (merge sort + binary search)
                // instead of paying the DB queries on the critical path.
                try {
                    LoginLookupIndex.get().refresh(new UserDAOImpl(), new UserRoleDAOImpl(), new RoleDAOImpl());
                } catch (Exception ex) {
                    // best-effort — login falls back to direct DAO lookups
                }
                return all;
            }
        };

        t.setOnSucceeded(evt -> {
            List<UserDTO> users = t.getValue();
            List<String> entries = new ArrayList<>();
            LoginLookupIndex index = LoginLookupIndex.get();
            for (UserDTO u : users) {
                List<String> roleNames = index.findRoleNames(u.getUserId()).orElse(List.of());
                String label = u.getUsername() + ":" + (roleNames.isEmpty() ? "User" : String.join(",", roleNames));
                entries.add(label);
            }
            // Deterministic, stable ordering for the dropdown — merge sort, O(n log n).
            AlgorithmUtils.mergeSort(entries, String.CASE_INSENSITIVE_ORDER);

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
            if (suppressFilter) return;
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
            if (!filtered.isEmpty()) {
                Platform.runLater(() -> {
                    if (!suppressFilter) userDropdown.show();
                });
            }
        });
    }

    private void handleUserSelection() {
        if (userDropdown == null) return;
        String value = userDropdown.getValue();
        if (value == null || value.isBlank()) return;
        // expected format: username:RoleName[,Role2]
        String username = value.split(":")[0];

        suppressFilter = true;
        try {
            // Reset the backing list to the FULL roster so a wrong pick never
            // strands the dropdown showing only the chosen entry — every user
            // stays selectable without reloading the system.
            userDropdown.setItems(allUsers);
            TextField editor = userDropdown.getEditor();
            if (editor != null) editor.setText(username);
            loginEmail.setText(username);
            // For quick demo, always pre-fill password with the demo password.
            loginPassword.setText("Password@12");
        } finally {
            suppressFilter = false;
        }
    }

    @FXML
    private void handleLogin() {
        String username = loginEmail.getText().trim();
        String password = loginPassword.getText();
        if (username.isEmpty()) {
            loginError.setText("Email / username is required.");
            FxFormValidator.applyStyle(loginEmail, false);
            return;
        }
        if (password.isEmpty()) {
            loginError.setText("Password is required.");
            FxFormValidator.applyStyle(loginPassword, false);
            return;
        }
        loginError.setText("");
        FxFormValidator.clearStyle(loginEmail);
        FxFormValidator.clearStyle(loginPassword);
        setLoading(true);

        long loginStartNanos = System.nanoTime();
        // Login is sped up by the in-memory LoginLookupIndex warmed in loadUsersForDemo()
        // (merge sort + binary search over the user snapshot) — it resolves the user and
        // primary role in O(log n) without DB round-trips, falling back to the DAO on a
        // miss. What bounds the UI's wait to 3 seconds is this watcher: if
        // authService.login(...) hasn't finished by then, the user sees a timeout instead
        // of an indefinite spinner. The background thread itself isn't force-killed (Java
        // has no safe way to do that) — if it finishes after the timeout already fired,
        // timedOut short-circuits both onSucceeded/onFailed below so the UI is never
        // touched twice.
        java.util.concurrent.atomic.AtomicBoolean timedOut = new java.util.concurrent.atomic.AtomicBoolean(false);

        Task<LoginResponseDTO> loginTask = new Task<>() {
            @Override
            protected LoginResponseDTO call() throws Exception {
                return authService.login(new LoginRequestDTO(username, password));
            }
        };

        javafx.animation.PauseTransition timeoutWatcher = new javafx.animation.PauseTransition(LOGIN_TIMEOUT);
        timeoutWatcher.setOnFinished(e -> {
            if (loginTask.isDone()) return; // finished just as the watcher fired — let the real handler run
            timedOut.set(true);
            setLoading(false);
            loginError.setText("Login is taking longer than 3 seconds — please check your connection and try again.");
            logger.warn("Login for \"" + username + "\" exceeded the 3s timeout.");
            try {
                Alert err = new Alert(Alert.AlertType.WARNING);
                err.setTitle("Login Timeout");
                err.setHeaderText(null);
                err.setContentText("Sign-in didn't complete within 3 seconds. Please try again.");
                err.initOwner(loginBtn.getScene().getWindow());
                err.show();
            } catch (Exception ignore) {}
        });

        loginTask.setOnSucceeded(e -> {
            timeoutWatcher.stop();
            long elapsedMs = (System.nanoTime() - loginStartNanos) / 1_000_000;
            logger.info("Login for \"" + username + "\" completed in " + elapsedMs + "ms.");
            if (timedOut.get()) {
                // Arrived after the 3s timeout already told the user it failed — the
                // session this created is real but the UI already moved on; nothing
                // to do here except note it for diagnostics.
                logger.warn("Login for \"" + username + "\" succeeded after the timeout had already fired ("
                    + elapsedMs + "ms total).");
                return;
            }
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

            boolean blocked = MaintenanceGate.isBlocked(
                SessionManager.getCurrentUserId(), SessionManager.getCurrentRoles());
            if (blocked) {
                navigateToStatusPage();
            } else {
                navigateToDashboard();
            }
        });

        loginTask.setOnFailed(e -> {
            timeoutWatcher.stop();
            if (timedOut.get()) return; // already reported to the user by the watcher
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
        timeoutWatcher.play();
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
        String email = resetEmail.getText() == null ? "" : resetEmail.getText().trim();
        if (email.isBlank()) {
            resetMessage.setText("Email address is required.");
            resetMessage.getStyleClass().removeAll("text-success");
            resetMessage.getStyleClass().add("text-danger");
            FxFormValidator.applyStyle(resetEmail, false);
            return;
        }
        if (!ValidatorUtils.isValidEmail(email)) {
            resetMessage.setText("Please enter a valid email address (e.g. jane@hospital.com).");
            resetMessage.getStyleClass().removeAll("text-success");
            resetMessage.getStyleClass().add("text-danger");
            FxFormValidator.applyStyle(resetEmail, false);
            return;
        }
        FxFormValidator.applyStyle(resetEmail, true);

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

    /** Shown instead of the Dashboard when {@link MaintenanceGate} blocks this user. */
    private void navigateToStatusPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(PageRoute.SYSTEM_STATUS.getFxmlPath())
            );
            Parent root = loader.load();
            Scene scene = loginEmail.getScene();
            Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
            newScene.getStylesheets().add(
                getClass().getResource("/hospital/management/css/global.css").toExternalForm()
            );
            ((Stage) scene.getWindow()).setScene(newScene);
        } catch (Exception e) {
            e.printStackTrace();
            // Fail open rather than stranding the user on a blank/broken screen if the
            // status page itself fails to load — the Dashboard's own permission checks
            // still apply normally, this only skips the maintenance notice.
            navigateToDashboard();
        }
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
