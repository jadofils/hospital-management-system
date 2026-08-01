package hospital.management.pages.components.shared.layout;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;

public class FooterController {
    @FXML private Hyperlink privacyLink;
    @FXML private Hyperlink termsLink;
    @FXML private Hyperlink contactLink;

    public void initialize() {
        if (privacyLink != null) {
            privacyLink.setOnAction(e -> showPlaceholder("Privacy Policy"));
        }
        if (termsLink != null) {
            termsLink.setOnAction(e -> showPlaceholder("Terms of Service"));
        }
        if (contactLink != null) {
            contactLink.setOnAction(e -> showPlaceholder("Contact Us"));
        }
    }

    /** FooterController has no reference to whichever page hosts it, so these
     *  placeholder legal links use a plain Alert rather than the app's toast. */
    private void showPlaceholder(String pageName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, pageName + " page is not available yet.");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
