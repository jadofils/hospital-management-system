package hospital.management.pages.components.shared.layout;

import hospital.management.backend.config.security.PermissionGate;
import hospital.management.enums.PageRoute;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class NavbarController {

    @FXML private Button dashboardBtn;

    @FXML private void handleDashboard()    { navigate(PageRoute.DASHBOARD); }
    @FXML private void handlePatients()     { navigate(PageRoute.PATIENTS); }
    @FXML private void handleAppointments() { navigate(PageRoute.APPOINTMENTS); }
    @FXML private void handleBilling()      { navigate(PageRoute.BILLING); }
    @FXML private void handleProfile()      { navigate(PageRoute.PROFILE); }

    @FXML
    private void handleNotifications() {
        // No notification feed is wired up to the top navbar yet (the right sidebar owns
        // its own separate notification panel) — an honest placeholder beats a silent no-op.
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "No new notifications.");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void navigate(PageRoute route) {
        if (!PermissionGate.isAllowed(route)) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "You don't have permission to access this page.");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

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
            Alert alert = new Alert(Alert.AlertType.ERROR, "Couldn't open that page. Please try again.");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }
}
