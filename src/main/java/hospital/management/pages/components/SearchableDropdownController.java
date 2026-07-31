package hospital.management.pages.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;

public class SearchableDropdownController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> dropdown;

    private final ObservableList<String> allItems = FXCollections.observableArrayList();

    public void initialize() {
        dropdown.setItems(allItems);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filter(newVal));
    }

    public void setItems(List<String> items) {
        allItems.setAll(items);
        dropdown.setItems(allItems);
    }

    public String getSelectedItem() {
        return dropdown.getValue();
    }

    private void filter(String query) {
        if (query == null || query.isEmpty()) {
            dropdown.setItems(allItems);
            return;
        }
        String lower = query.toLowerCase();
        ObservableList<String> filtered = allItems.filtered(item -> item.toLowerCase().contains(lower));
        dropdown.setItems(filtered);
        dropdown.show();
    }
}
