package hospital.management.pages.components.shared.buttons;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;

public class LoadingButtonController {

    @FXML private Button actionBtn;
    @FXML private ProgressIndicator spinner;

    private Runnable onAction;

    public void initialize() {
        actionBtn.setOnAction(e -> {
            if (onAction != null) {
                setLoading(true);
                onAction.run();
            }
        });
    }

    public void setOnAction(Runnable onAction) {
        this.onAction = onAction;
    }

    public void setLoading(boolean loading) {
        spinner.setVisible(loading);
        spinner.setManaged(loading);
        actionBtn.setDisable(loading);
    }
}
