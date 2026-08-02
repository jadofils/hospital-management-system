package hospital.management.pages.components.shared.search;

import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Pairs an {@link EntityIdComboBox} with its own small loading spinner, for
 * dropdowns whose options come from an async DB fetch (see {@code AsyncJobRunner}).
 * Passed as a single field to {@code FormDialogController.addField(...)} (which
 * accepts any Node). Call {@link #setLoading(boolean)} around the fetch: it shows
 * the spinner and disables the dropdown while options are still loading.
 */
public class LoadingIdComboBox extends HBox {

    private final EntityIdComboBox comboBox = new EntityIdComboBox();
    private final ProgressIndicator spinner = new ProgressIndicator();

    public LoadingIdComboBox() {
        super(8);
        setAlignment(Pos.CENTER_LEFT);
        spinner.setPrefSize(16, 16);
        spinner.getStyleClass().add("button-spinner");
        spinner.setVisible(false);
        spinner.setManaged(false);
        HBox.setHgrow(comboBox, Priority.ALWAYS);
        getChildren().addAll(comboBox, spinner);
    }

    public EntityIdComboBox getComboBox() {
        return comboBox;
    }

    /** Shows the spinner and disables the dropdown while its options are still loading. */
    public void setLoading(boolean loading) {
        spinner.setVisible(loading);
        spinner.setManaged(loading);
        comboBox.setDisable(loading);
    }
}
