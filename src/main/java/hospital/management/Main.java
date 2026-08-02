package hospital.management;

import hospital.management.backend.config.AppConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource(AppConfig.HOME_FXML_PATH)
        );
        Scene scene = new Scene(loader.load(), AppConfig.WINDOW_WIDTH, AppConfig.WINDOW_HEIGHT);
        scene.getStylesheets().add(
            getClass().getResource(AppConfig.CSS_PATH).toExternalForm()
        );

        // Scale all em-based CSS from smallest phones (~360px) up to 4K TVs (~3840px).
        // Formula clamps base font between 11px (small) and 28px (TV).
        Rectangle2D screen = Screen.getPrimary().getBounds();
        double baseFontSize = Math.max(11, Math.min(28, 9 + screen.getWidth() / 300.0));
        scene.getRoot().setStyle("-fx-font-size: " + baseFontSize + "px;");
        stage.setTitle(AppConfig.APP_NAME);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}