package hospital.management;

import hospital.management.backend.config.AppConfig;
import hospital.management.backend.daemon.BackupDaemon;
import hospital.management.backend.daemon.DatabaseCleanupDaemon;
import hospital.management.backend.mongo.config.MongoConfig;
import hospital.management.backend.service.notification.NotificationEventListener;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.logging.LogManager;

public class Main extends Application {

    static {
        // Ensure JUL uses src/main/resources/logging.properties in every launch mode.
        try (InputStream in = Main.class.getResourceAsStream("/logging.properties")) {
            if (in != null) {
                LogManager.getLogManager().readConfiguration(in);
            }
        } catch (Exception ignored) {
            // Keep app startup resilient even if logging config is missing.
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseCleanupDaemon.start();
        BackupDaemon.start();
        NotificationEventListener.start();

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
        stage.setResizable(true);

        // Let the window shrink down to phone/tablet-ish sizes.
        stage.setMinWidth(640);
        stage.setMinHeight(480);

        // Fit the window inside the visible screen area. Opening larger than the
        // display pushes the native title bar (close/minimize/maximize) off-screen,
        // which makes the window look undecorated and impossible to drag or resize.
        Rectangle2D workArea = Screen.getPrimary().getVisualBounds();
        double initialW = Math.min(AppConfig.WINDOW_WIDTH,
            Math.max(workArea.getWidth() - 32, stage.getMinWidth()));
        double initialH = Math.min(AppConfig.WINDOW_HEIGHT,
            Math.max(workArea.getHeight() - 32, stage.getMinHeight()));
        stage.setWidth(initialW);
        stage.setHeight(initialH);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() {
        DatabaseCleanupDaemon.stop();
        BackupDaemon.stop();
        MongoConfig.close();
    }

    public static void main(String[] args) {
        launch();
    }
}