package hospital.management.pages.components;

import hospital.management.enums.PageRoute;
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

    public void setActiveItem(PageRoute route) {
        dashboardBtn.getStyleClass().remove("active");
        patientsBtn.getStyleClass().remove("active");
        appointmentsBtn.getStyleClass().remove("active");
        billingBtn.getStyleClass().remove("active");

        Button target = switch (route) {
            case DASHBOARD    -> dashboardBtn;
            case PATIENTS     -> patientsBtn;
            case APPOINTMENTS -> appointmentsBtn;
            case BILLING      -> billingBtn;
            default           -> null;
        };
        if (target != null) target.getStyleClass().add("active");
    }

    @FXML private void handleDashboard()    { navigate(PageRoute.DASHBOARD); }
    @FXML private void handlePatients()     { navigate(PageRoute.PATIENTS); }
    @FXML private void handleAppointments() { navigate(PageRoute.APPOINTMENTS); }
    @FXML private void handleBilling()      { navigate(PageRoute.BILLING); }
    @FXML private void handleLogout()       { navigate(PageRoute.AUTH); }

    private void navigate(PageRoute route) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(route.getFxmlPath()));
            Parent root = loader.load();
            Scene scene = dashboardBtn.getScene();
            Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
            newScene.getStylesheets().add(
                getClass().getResource("/hospital/management/css/global.css").toExternalForm()
            );
            ((Stage) scene.getWindow()).setScene(newScene);
        } catch (Exception e) {
            System.err.println("Navigation to " + route.getFxmlPath() + " failed: " + e.getMessage());
        }
    }
}