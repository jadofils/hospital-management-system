package hospital.management.pages.components;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class SidebarController {

    @FXML private Button dashboardBtn;
    @FXML private Button patientsBtn;
    @FXML private Button appointmentsBtn;
    @FXML private Button billingBtn;

    public void setActiveItem(String key) {
        dashboardBtn.getStyleClass().remove("active");
        patientsBtn.getStyleClass().remove("active");
        appointmentsBtn.getStyleClass().remove("active");
        billingBtn.getStyleClass().remove("active");

        Button target = switch (key) {
            case "dashboard" -> dashboardBtn;
            case "patients" -> patientsBtn;
            case "appointments" -> appointmentsBtn;
            case "billing" -> billingBtn;
            default -> null;
        };
        if (target != null) {
            target.getStyleClass().add("active");
        }
    }

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
    private void handleLogout() {
        navigate("/hospital/management/frontend/pages/home-page.fxml");
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
