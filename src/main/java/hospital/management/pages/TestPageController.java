package hospital.management.pages;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class TestPageController {

    @FXML
    private Button testButton;

    @FXML
    private void handleButtonClick() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Test Alert");
        alert.setHeaderText("Button Clicked!");
        alert.setContentText("You just clicked the test button.");
        alert.showAndWait();
    }
}
