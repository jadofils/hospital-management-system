package hospital.management.pages.components.shared.feedback;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;

/**
 * Reusable read-only "view details" popup, mirroring FormDialogController's
 * shell (overlay, box, title, icon, close) but rendering an ordered set of
 * label:value rows instead of editable fields. Used by every entity table's
 * "view details" row action except Patients, which navigate to the fuller
 * PatientDetailController page instead.
 */
public class DetailViewController {

    @FXML private StackPane detailOverlay;
    @FXML private VBox      detailBox;
    @FXML private Label     detailTitle;
    @FXML private FontIcon  detailIcon;
    @FXML private VBox      detailFieldsBox;

    public void initialize() {
        detailBox.maxHeightProperty().bind(detailOverlay.heightProperty().multiply(0.9));
    }

    /** Opens the popup with an ordered field label -> value map (use a LinkedHashMap to control order). */
    public void show(String title, String iconLiteral, Map<String, String> fields) {
        show(title, iconLiteral, fields, null);
    }

    /** Same as {@link #show(String, String, Map)}, but appends an arbitrary extra node
     *  (e.g. a resource-grouped permission-card grid) after the plain label/value fields —
     *  used by View Role, where permissions are shown as cards instead of a text field. */
    public void show(String title, String iconLiteral, Map<String, String> fields, Node extraContent) {
        detailTitle.setText(title);
        if (iconLiteral == null || iconLiteral.isBlank()) {
            detailIcon.setVisible(false);
            detailIcon.setManaged(false);
        } else {
            detailIcon.setVisible(true);
            detailIcon.setManaged(true);
            detailIcon.setIconLiteral(iconLiteral);
        }

        detailFieldsBox.getChildren().clear();
        fields.forEach((label, value) -> {
            Label lbl = new Label(label);
            lbl.getStyleClass().add("field-label");
            Label val = new Label(value == null || value.isBlank() ? "—" : value);
            val.getStyleClass().add("detail-value");
            val.setWrapText(true);
            detailFieldsBox.getChildren().add(new VBox(2, lbl, val));
        });
        if (extraContent != null) {
            detailFieldsBox.getChildren().add(extraContent);
        }

        detailOverlay.setVisible(true);
        detailOverlay.setManaged(true);
    }

    @FXML
    private void handleClose() {
        detailOverlay.setVisible(false);
        detailOverlay.setManaged(false);
    }
}
