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
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AuthPageController {

    private final AuthService authService = new AuthServiceImpl(
        new UserDAOImpl(), new UserSessionDAOImpl(), new UserRoleDAOImpl(),
        new RoleDAOImpl(), new AuditLogDAOImpl());

    // Login tab
    @FXML private TextField     loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label         loginError;
    @FXML private Button        loginBtn;

    // Reset tab
    @FXML private TextField resetEmail;
    @FXML private Label     resetMessage;

    public void initialize() {
        loginError.setText("");
        resetMessage.setText("");
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

        AsyncJobRunner.submit(
            () -> authService.login(new LoginRequestDTO(username, password)),
            response -> {
                SessionManager.login(response.getToken(), response.getSessionId());
                setLoading(false);
                navigateToDashboard();
            },
            ex -> {
                setLoading(false);
                loginError.setText(ex instanceof AppException ? ex.getMessage() : "Login failed. Please try again.");
            });
    }

    private void setLoading(boolean loading) {
        loginBtn.setDisable(loading);
        loginEmail.setDisable(loading);
        loginPassword.setDisable(loading);
        Node originalGraphic = loginBtn.getGraphic();
        if (loading) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(14, 14);
            loginBtn.setUserData(originalGraphic);
            loginBtn.setGraphic(spinner);
        } else if (loginBtn.getUserData() instanceof Node savedGraphic) {
            loginBtn.setGraphic(savedGraphic);
        }
    }

    @FXML
    private void handleResetPassword() {
        if (resetEmail.getText().isBlank()) {
            resetMessage.setText("Please enter your email address.");
            resetMessage.getStyleClass().removeAll("text-success");
            resetMessage.getStyleClass().add("text-danger");
            return;
        }
        resetMessage.getStyleClass().removeAll("text-danger");
        resetMessage.getStyleClass().add("text-success");
        resetMessage.setText("Reset link sent to " + resetEmail.getText());
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
            System.err.println("Navigation to dashboard failed: " + e.getMessage());
            loginError.setText("Login succeeded, but the dashboard failed to load. Please restart the app.");
        }
    }
}
