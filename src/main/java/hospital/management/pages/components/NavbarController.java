package hospital.management.pages.components;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class NavbarController {

    @FXML private Button dashboardBtn;

    @FXML
    private void handleDashboard() {
        navigate("/hospital/management/frontend/pages/dashboard.fxml");
    }

    @FXML
    private void handlePatients() {
        navigate("/hospital/management/frontend/pages/patients-page.fxml");
    }

    @FXML
    private void handleAppointments() {
        navigate("/hospital/management/frontend/pages/appointments-page.fxml");
    }

    @FXML
    private void handleBilling() {
        navigate("/hospital/management/frontend/pages/billing-page.fxml");
    }

    @FXML
    private void handleProfile() {
        System.out.println("Profile clicked");
    }

    private void navigate(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = dashboardBtn.getScene();
            Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
            newScene.getStylesheets().add(
                getClass().getResource("/hospital/management/css/global.css").toExternalForm()
            );
            ((Stage) scene.getWindow()).setScene(newScene);
        } catch (Exception e) {
            System.err.println("Navigation to " + fxmlPath + " failed: " + e.getMessage());
        }
    }
}
