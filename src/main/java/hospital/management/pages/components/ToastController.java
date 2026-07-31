package hospital.management.pages.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class ToastController {

    @FXML private HBox toastContainer;
    @FXML private Label toastMessage;

    public void show(String message) {
        show(message, "info");
    }

    public void show(String message, String type) {
        toastMessage.setText(message);
        toastContainer.getStyleClass().removeAll("toast-info", "toast-success", "toast-error");
        toastContainer.getStyleClass().add("toast-" + type);

        toastContainer.setVisible(true);
        toastContainer.setManaged(true);
        toastContainer.setOpacity(1);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(400), toastContainer);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(f -> {
                toastContainer.setVisible(false);
                toastContainer.setManaged(false);
            });
            fade.play();
        });
        pause.play();
    }
}
