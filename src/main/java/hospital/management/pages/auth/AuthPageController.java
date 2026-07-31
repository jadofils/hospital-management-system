package hospital.management.pages.auth;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AuthPageController {

    // Login tab
    @FXML private TextField     loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label         loginError;

    // Reset tab
    @FXML private TextField resetEmail;
    @FXML private Label     resetMessage;

    public void initialize() {
        loginError.setText("");
        resetMessage.setText("");
    }

    @FXML
    private void handleLogin() {
        String email    = loginEmail.getText().trim();
        String password = loginPassword.getText();
        if (email.isEmpty() || password.isEmpty()) {
            loginError.setText("Email and password are required.");
            return;
        }
        loginError.setText("");
        navigateToDashboard();
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
        }
    }
}