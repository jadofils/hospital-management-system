package hospital.management.pages.components;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;

public class FooterController {
    @FXML private Hyperlink privacyLink;
    @FXML private Hyperlink termsLink;
    @FXML private Hyperlink contactLink;

    public void initialize() {
        if (privacyLink != null) {
            privacyLink.setOnAction(e -> System.out.println("Privacy Policy clicked"));
        }
        if (termsLink != null) {
            termsLink.setOnAction(e -> System.out.println("Terms of Service clicked"));
        }
        if (contactLink != null) {
            contactLink.setOnAction(e -> System.out.println("Contact Us clicked"));
        }
    }
}