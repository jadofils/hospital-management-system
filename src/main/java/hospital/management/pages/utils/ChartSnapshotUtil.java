package hospital.management.pages.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;

/** Captures a live JavaFX chart as a raster image, for embedding into exported
 *  reports (e.g. the Analytics PDF report). Must be called on the FX Application
 *  thread — {@code Chart.snapshot(...)} is a UI operation. */
public final class ChartSnapshotUtil {

    private ChartSnapshotUtil() {}

    public static BufferedImage capture(Chart chart) {
        WritableImage image = chart.snapshot(new SnapshotParameters(), null);
        return SwingFXUtils.fromFXImage(image, null);
    }
}
