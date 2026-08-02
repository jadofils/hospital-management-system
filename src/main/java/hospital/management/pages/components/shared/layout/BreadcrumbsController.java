package hospital.management.pages.components.shared.layout;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class BreadcrumbsController {

    @FXML private HBox breadcrumbBar;

    public void setPath(String... crumbs) {
        breadcrumbBar.getChildren().clear();
        for (int i = 0; i < crumbs.length; i++) {
            boolean last = i == crumbs.length - 1;
            if (last) {
                Label current = new Label(crumbs[i]);
                current.getStyleClass().add("breadcrumb-current");
                breadcrumbBar.getChildren().add(current);
            } else {
                Hyperlink link = new Hyperlink(crumbs[i]);
                link.getStyleClass().add("breadcrumb-link");
                breadcrumbBar.getChildren().add(link);

                Label separator = new Label("/");
                separator.getStyleClass().add("breadcrumb-separator");
                breadcrumbBar.getChildren().add(separator);
            }
        }
    }
}
