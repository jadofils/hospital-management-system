package hospital.management.pages.components.shared.feedback;

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
            if (onConfirm != null) onConfirm.run();
            hide();
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
