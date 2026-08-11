package hospital.management.pages.components.shared.feedback;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

/**
 * Reusable modal form dialog used by every data page.
 *
 * Each page opens it in "Add" mode (empty fields) or "Update" mode (fields
 * pre-filled from the selected row) and populates it with labeled fields via
 * {@link #addField(String, String, Node)}.
 */
public class FormDialogController {

    @FXML private StackPane    dialogOverlay;
    @FXML private VBox         dialogBox;
    @FXML private VBox         fieldsBox;
    @FXML private Label        dialogTitle;
    @FXML private FontIcon     dialogIcon;
    @FXML private Label        dialogError;
    @FXML private Button       submitBtn;
    @FXML private ProgressIndicator submitSpinner;

    private Consumer<Void> onSubmit;
    private boolean addMode = true;

    public void initialize() {
        submitBtn.setOnAction(e -> {
            if (onSubmit == null) return;
            setLoading(true);
            onSubmit.accept(null);
        });

        // Keep the dialog within the window at all times — the overlay always
        // spans the full page, so cap the box a bit under its height instead
        // of a fixed CSS value that could exceed a smaller window.
        dialogBox.maxHeightProperty().bind(
            dialogOverlay.heightProperty().multiply(0.9)
        );
    }

    /** Opens the dialog. Callback runs when the (blue) submit button is pressed. */
    public void open(String title, String iconLiteral, boolean addMode, Consumer<Void> onSubmit) {
        this.onSubmit = onSubmit;
        this.addMode = addMode;
        dialogTitle.setText(title);
        if (iconLiteral == null || iconLiteral.isBlank()) {
            dialogIcon.setVisible(false);
            dialogIcon.setManaged(false);
        } else {
            dialogIcon.setVisible(true);
            dialogIcon.setManaged(true);
            dialogIcon.setIconLiteral(iconLiteral);
        }
        dialogError.setText("");
        fieldsBox.getChildren().clear();
        setLoading(false);
        dialogOverlay.setVisible(true);
        dialogOverlay.setManaged(true);
        Platform.runLater(() -> {
            if (!fieldsBox.getChildren().isEmpty()) {
                fieldsBox.getChildren().get(0).requestFocus();
            }
        });
    }

    /** Appends a labeled field row (label + icon above the control), home-form style.
     *  Accepts any Node (not just Control) so a composite field — e.g. a dropdown
     *  paired with its own loading spinner — can be passed as a single unit. */
    public void addField(String label, String iconLiteral, Node control) {
        addFieldRow(label, iconLiteral, control, false);
    }

    /** Same as {@link #addField} but appends a red asterisk to the label to mark
     *  the field as mandatory *before* the user submits — no more discovering it
     *  from an error message after the fact. */
    public void addRequiredField(String label, String iconLiteral, Node control) {
        addFieldRow(label, iconLiteral, control, true);
    }

    private void addFieldRow(String label, String iconLiteral, Node control, boolean required) {
        HBox labelRow = new HBox(6);
        labelRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (iconLiteral != null && !iconLiteral.isBlank()) {
            FontIcon icon = new FontIcon(iconLiteral);
            icon.setIconSize(12);
            icon.getStyleClass().add("field-icon");
            labelRow.getChildren().add(icon);
        }
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        labelRow.getChildren().add(lbl);
        if (required) {
            Label star = new Label(" *");
            star.getStyleClass().add("field-required");
            Tooltip.install(star, new Tooltip("Required field"));
            labelRow.getChildren().add(star);
        }

        VBox row = new VBox(4);
        row.getChildren().addAll(labelRow, control);
        fieldsBox.getChildren().add(row);
    }

    /** Appends a fully custom row (e.g. two controls side by side). */
    public void addRow(Node node) {
        fieldsBox.getChildren().add(node);
    }

    /** Clears every field row previously added. */
    public void clearFields() {
        fieldsBox.getChildren().clear();
    }

    public void setError(String message) {
        dialogError.setText(message == null ? "" : message);
    }

    public boolean isAddMode() {
        return addMode;
    }

    /** Shows/hides the in-button spinner and disables the submit button while running. */
    public void setLoading(boolean loading) {
        submitSpinner.setVisible(loading);
        submitSpinner.setManaged(loading);
        submitBtn.setDisable(loading);
        if (!loading) {
            submitBtn.setText(addMode ? "Add" : "Update");
        }
    }

    public void close() {
        dialogOverlay.setVisible(false);
        dialogOverlay.setManaged(false);
        setLoading(false);
    }

    @FXML
    private void handleClose() {
        close();
    }
}
