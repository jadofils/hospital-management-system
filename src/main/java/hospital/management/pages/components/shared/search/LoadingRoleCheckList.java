package hospital.management.pages.components.shared.search;

import javafx.geometry.Pos;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Pairs a {@link RoleCheckListBox} with its own small loading spinner, mirroring
 * {@link LoadingIdComboBox}'s pairing for the single-select id dropdown. Passed as
 * a single field to {@code FormDialogController.addField(...)}.
 */
public class LoadingRoleCheckList extends HBox {

    private final RoleCheckListBox checkList = new RoleCheckListBox();
    private final ProgressIndicator spinner = new ProgressIndicator();

    public LoadingRoleCheckList() {
        super(8);
        setAlignment(Pos.CENTER_LEFT);
        spinner.setPrefSize(16, 16);
        spinner.getStyleClass().add("button-spinner");
        spinner.setVisible(false);
        spinner.setManaged(false);
        HBox.setHgrow(checkList, Priority.ALWAYS);
        getChildren().addAll(checkList, spinner);
    }

    public RoleCheckListBox getCheckList() {
        return checkList;
    }

    /** Shows the spinner and disables the checklist while its options are still loading. */
    public void setLoading(boolean loading) {
        spinner.setVisible(loading);
        spinner.setManaged(loading);
        checkList.setDisable(loading);
    }
}
