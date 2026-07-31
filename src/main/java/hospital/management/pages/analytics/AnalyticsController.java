package hospital.management.pages.analytics;

import hospital.management.pages.BasePageController;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

public class AnalyticsController extends BasePageController {

    @FXML private ComboBox<String> periodFilter;
    @FXML private Button exportBtn;

    @FXML private BarChart<String, Number>  admissionsChart;
    @FXML private CategoryAxis admXAxis;
    @FXML private NumberAxis   admYAxis;

    @FXML private BarChart<String, Number>  revenueChart;
    @FXML private CategoryAxis revXAxis;
    @FXML private NumberAxis   revYAxis;

    @FXML private PieChart apptStatusChart;

    @FXML private BarChart<String, Number>  feedbackChart;
    @FXML private CategoryAxis fbXAxis;
    @FXML private NumberAxis   fbYAxis;

    @FXML private BarChart<String, Number>  labStatusChart;
    @FXML private CategoryAxis labXAxis;
    @FXML private NumberAxis   labYAxis;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.ANALYTICS);

        periodFilter.getItems().addAll("Last 30 days", "Last 3 months", "Last 6 months", "Last 12 months");
        periodFilter.setValue("Last 12 months");

        setupCharts();

        periodFilter.setOnAction(e -> reloadData());
        exportBtn.setOnAction(e -> toast("Export not yet implemented.", NotificationType.INFO));
    }

    private void setupCharts() {
        admissionsChart.setLegendVisible(false);
        admissionsChart.setTitle("");
        admissionsChart.getData().add(new XYChart.Series<>());

        revenueChart.setLegendVisible(false);
        revenueChart.setTitle("");
        revenueChart.getData().add(new XYChart.Series<>());

        feedbackChart.setLegendVisible(false);
        feedbackChart.setTitle("");
        feedbackChart.getData().add(new XYChart.Series<>());

        labStatusChart.setLegendVisible(false);
        labStatusChart.setTitle("");
        labStatusChart.getData().add(new XYChart.Series<>());

        apptStatusChart.setLegendVisible(true);
        apptStatusChart.setLabelsVisible(true);
    }

    private void reloadData() {
        // reload charts with real data when backend is wired
        toast("Loading " + periodFilter.getValue() + " data...", NotificationType.INFO);
    }
}