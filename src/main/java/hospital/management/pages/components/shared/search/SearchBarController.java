package hospital.management.pages.components.shared.search;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.text.BreakIterator;

public class SearchBarController {
    @FXML private TextField searchField;
    @FXML private Button searchBtn;

    public void initialize(){
        searchBtn.setOnAction(e->performSearch());
    }

    private void performSearch() {
        String query = searchField.getText();
        if (query != null && !query.isEmpty()) {
            System.out.println("Searching for: " + query);
            // TODO: integrate with patient/doctor DB or table filtering
        }
    }
}
