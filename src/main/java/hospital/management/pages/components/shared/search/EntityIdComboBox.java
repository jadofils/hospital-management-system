package hospital.management.pages.components.shared.search;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.util.List;

/**
 * A searchable dropdown for picking a foreign-key id by its human-readable
 * name: the user sees/types a label, the selected {@link Option}'s id is what
 * the form actually submits. Built as a plain, code-constructible ComboBox
 * (not FXML-based) since every existing Add/Edit dialog already builds its
 * fields directly in Java inside open*Dialog() methods — this is a drop-in
 * replacement for {@code new TextField()} in that same style.
 */
public class EntityIdComboBox extends ComboBox<EntityIdComboBox.Option> {

    public record Option(String id, String label) {
        @Override
        public String toString() { return label; }
    }

    private final ObservableList<Option> allOptions = FXCollections.observableArrayList();
    private final ObservableList<Option> displayedOptions = FXCollections.observableArrayList();

    public EntityIdComboBox() {
        setEditable(true);
        setItems(displayedOptions);

        setConverter(new StringConverter<>() {
            @Override
            public String toString(Option option) {
                return option == null ? "" : option.label();
            }

            @Override
            public Option fromString(String text) {
                return allOptions.stream()
                        .filter(o -> o.label().equalsIgnoreCase(text))
                        .findFirst()
                        .orElse(getValue());
            }
        });

        getEditor().textProperty().addListener((obs, oldText, newText) -> {
            // Editor text changes when a selection is committed too — skip re-filtering then.
            if (getValue() != null && getValue().label().equals(newText)) return;
            String lower = newText == null ? "" : newText.toLowerCase();
            displayedOptions.setAll(allOptions.stream()
                    .filter(o -> o.label().toLowerCase().contains(lower))
                    .toList());
            if (!isShowing() && getEditor().isFocused()) show();
        });

        setOnShowing(e -> {
            String text = getEditor().getText();
            if (text == null || text.isBlank()) {
                displayedOptions.setAll(allOptions);
            }
        });
    }

    /** Replaces the full set of selectable options. */
    public void setOptions(List<Option> options) {
        allOptions.setAll(options);
        displayedOptions.setAll(options);
    }

    /** The id of the currently selected option, or null if nothing is selected. */
    public String getSelectedId() {
        Option selected = getValue();
        return selected == null ? null : selected.id();
    }

    /** Pre-selects an option by id (e.g. to prefill Edit mode). No-op if the id isn't in the option list. */
    public void selectById(String id) {
        if (id == null) return;
        allOptions.stream().filter(o -> o.id().equals(id)).findFirst().ifPresent(this::setValue);
    }
}
