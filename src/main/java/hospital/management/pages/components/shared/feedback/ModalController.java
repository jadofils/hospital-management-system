package hospital.management.pages.components.shared.feedback;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class ModalController {

    @FXML private StackPane modalOverlay;
    @FXML private Label modalTitle;
    @FXML private Label modalBody;
    @FXML private Button cancelBtn;
    @FXML private Button confirmBtn;

    private Runnable onConfirm;

    public void initialize() {
        cancelBtn.setOnAction(e -> hide());
        confirmBtn.setOnAction(e -> {
            if (onConfirm == null) { hide(); return; }
            ButtonSpinner.setLoading(confirmBtn, true);
            cancelBtn.setDisable(true);
            // Deferred one pulse so the spinner graphic is guaranteed to paint
            // before the confirm action (a synchronous DB/in-memory op today) runs.
            Platform.runLater(() -> {
                try {
                    onConfirm.run();
                } finally {
                    ButtonSpinner.setLoading(confirmBtn, false);
                    cancelBtn.setDisable(false);
                    hide();
                }
            });
        });
    }

    public void show(String title, String body, Runnable onConfirm) {
        modalTitle.setText(title);
        modalBody.setText(body);
        this.onConfirm = onConfirm;
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    @FXML
    private void handleClose() {
        hide();
    }

    public void hide() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
    }
}
