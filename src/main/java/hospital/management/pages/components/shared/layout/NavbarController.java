package hospital.management.pages.components.shared.layout;

import hospital.management.backend.config.security.PermissionGate;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.enums.PageRoute;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class NavbarController {

    @FXML private Button dashboardBtn;
    @FXML private Button profileBtn;
    
    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    
    @FXML
    private void initialize() {
        loadCurrentUserName();
    }

    @FXML private void handleDashboard()    { navigate(PageRoute.DASHBOARD); }
    @FXML private void handlePatients()     { navigate(PageRoute.PATIENTS); }
    @FXML private void handleAppointments() { navigate(PageRoute.APPOINTMENTS); }
    @FXML private void handleBilling()      { navigate(PageRoute.BILLING); }
    @FXML private void handleProfile()      { navigate(PageRoute.PROFILE); }

    private void loadCurrentUserName() {
        if (profileBtn == null) return;
        
        new Thread(() -> {
            try {
                String userId = SessionManager.getCurrentUserId();
                UserDTO user = userService.findById(userId);
                String displayName = user.getUsername();
                
                // Try to get a more friendly name if available
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String[] parts = user.getEmail().split("@");
                    if (parts.length > 0) {
                        String localPart = parts[0];
                        // Capitalize first letter
                        displayName = localPart.substring(0, 1).toUpperCase() + localPart.substring(1);
                    }
                }
                
                final String finalName = displayName;
                Platform.runLater(() -> profileBtn.setText(finalName));
            } catch (Exception e) {
                // Fallback to username from session if user lookup fails
                try {
                    String username = SessionManager.getCurrentUsername();
                    Platform.runLater(() -> profileBtn.setText(username));
                } catch (Exception ex) {
                    // Keep default text if all else fails
                }
            }
        }).start();
    }
    
    @FXML
    private void handleNotifications() {
        // Notifications are handled by the right sidebar panel
        Alert alert = new Alert(Alert.AlertType.INFORMATION, 
            "Notifications are available in the right sidebar panel.\n\n" +
            "Click the notification bell icon in the sidebar to view recent activity and alerts.");
        alert.setTitle("Notifications");
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
            Scene scene = profileBtn.getScene();
            Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
            newScene.getStylesheets().add(
                getClass().getResource("/hospital/management/css/global.css").toExternalForm()
            );
            ((Stage) scene.getWindow()).setScene(newScene);
        } catch (Exception e) {
            System.err.println("Navigation to " + route.getFxmlPath() + " failed: " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Couldn't open that page. Please try again.");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }
}
