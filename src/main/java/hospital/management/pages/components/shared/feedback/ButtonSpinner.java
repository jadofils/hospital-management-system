package hospital.management.pages.components.shared.feedback;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ProgressIndicator;

/**
 * Toggles an in-place loading spinner on any {@link Button} without requiring
 * the button's FXML to declare one (unlike {@code FormDialogController}'s
 * {@code submitSpinner}, which is declared in form-dialog.fxml). Swaps the
 * button's graphic for a small {@link ProgressIndicator} and restores the
 * original graphic afterwards, so it works on plain toolbar/row buttons too.
 */
public final class ButtonSpinner {

    private static final String ORIGINAL_GRAPHIC_KEY = "buttonSpinner.originalGraphic";

    private ButtonSpinner() {}

    public static void setLoading(Button button, boolean loading) {
        if (loading) {
            if (!button.getProperties().containsKey(ORIGINAL_GRAPHIC_KEY)) {
                button.getProperties().put(ORIGINAL_GRAPHIC_KEY, button.getGraphic());
            }
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(14, 14);
            spinner.getStyleClass().add("button-spinner");
            button.setContentDisplay(ContentDisplay.LEFT);
            button.setGraphic(spinner);
            button.setDisable(true);
        } else {
            Object original = button.getProperties().remove(ORIGINAL_GRAPHIC_KEY);
            button.setGraphic(original instanceof Node node ? node : null);
            button.setDisable(false);
        }
    }
}
