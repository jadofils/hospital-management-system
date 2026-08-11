package hospital.management.pages.components.shared.sort;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;

import java.util.function.BiConsumer;

/**
 * Reusable "Sort by" control shown in every entity page toolbar. Hosting pages
 * register their sortable fields via {@link #addOption(String)} and supply a
 * callback (usually delegating to {@code PaginatedTableController.applySort})
 * via {@link #setOnSort(BiConsumer)}.
 */
public class SortBarController {

    private static final String DEFAULT_OPTION = "Default order";

    @FXML private ComboBox<String> sortFieldCombo;
    @FXML private ToggleButton sortDirToggle;

    private BiConsumer<String, Boolean> onSort;

    public void initialize() {
        sortFieldCombo.getItems().add(DEFAULT_OPTION);
        sortFieldCombo.setValue(DEFAULT_OPTION);
        sortDirToggle.setSelected(false);
        updateToggleText();

        sortFieldCombo.setOnAction(e -> fireSort());
        sortDirToggle.setOnAction(e -> fireSort());
    }

    /** Adds a sortable field option to the dropdown. */
    public void addOption(String label) {
        if (label == null || label.isBlank() || sortFieldCombo.getItems().contains(label)) return;
        sortFieldCombo.getItems().add(label);
    }

    /** Adds every registered sortable field option from a table controller. */
    public void addOptions(java.util.Collection<String> labels) {
        if (labels == null) return;
        labels.forEach(this::addOption);
    }

    /** Hosting page supplies how a (field, ascending) selection is applied. */
    public void setOnSort(BiConsumer<String, Boolean> onSort) {
        this.onSort = onSort;
    }

    /** Resets to the natural (unsorted) order. */
    public void reset() {
        sortFieldCombo.setValue(DEFAULT_OPTION);
        sortDirToggle.setSelected(false);
        updateToggleText();
        fireSort();
    }

    private void fireSort() {
        updateToggleText();
        boolean ascending = !sortDirToggle.isSelected();
        String field = DEFAULT_OPTION.equals(sortFieldCombo.getValue()) ? null : sortFieldCombo.getValue();
        if (onSort != null) onSort.accept(field, ascending);
    }

    private void updateToggleText() {
        sortDirToggle.setText(sortDirToggle.isSelected() ? "↓ Descending" : "↑ Ascending");
    }
}
