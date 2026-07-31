package hospital.management.pages.components;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class NotificationController {

    @FXML private Label unreadCount;
    @FXML private VBox notificationList;

    private int unread = 0;

    public void initialize() {
        addNotification("Lab results ready for Alice Johnson");
        addNotification("Dr. Smith requested a schedule change");
        addNotification("Emergency admission: Daniel Brown");
    }

    public void addNotification(String message) {
        Label entry = new Label(message);
        entry.getStyleClass().add("notification-item");
        entry.setWrapText(true);
        notificationList.getChildren().add(0, entry);

        unread++;
        unreadCount.setText(String.valueOf(unread));
    }

    @FXML
    private void handleViewAll() {
        unread = 0;
        unreadCount.setText("0");
        System.out.println("Viewing all notifications");
    }
}
