package hospital.management.pages.components;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class UserProfileController {

    @FXML private Label avatarInitials;
    @FXML private Label profileName;
    @FXML private Label profileRole;
    @FXML private Label roleBadge;

    public void setUser(String name, String role) {
        profileName.setText(name);
        profileRole.setText(role);
        roleBadge.setText(role.toUpperCase());

        String[] parts = name.trim().split("\\s+");
        String initials = parts.length >= 2
                ? "" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)
                : name.substring(0, Math.min(2, name.length()));
        avatarInitials.setText(initials.toUpperCase());
    }

    @FXML
    private void handleEditProfile() {
        System.out.println("Edit Profile clicked");
    }

    @FXML
    private void handleSettings() {
        System.out.println("Settings clicked");
    }

    @FXML
    private void handleLogout() {
        System.out.println("Logout clicked");
    }
}
