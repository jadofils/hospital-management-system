package hospital.management.pages;

import hospital.management.pages.components.SidebarController;
import hospital.management.pages.components.StatsWidgetController;
import hospital.management.model.Patient;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DashboardController {

    @FXML private SidebarController sidebarController;
    @FXML private StatsWidgetController statsWidgetController;

    @FXML private BarChart<String, Number> admissionsChart;
    @FXML private CategoryAxis admissionsXAxis;
    @FXML private NumberAxis admissionsYAxis;

    @FXML private PieChart statusChart;

    @FXML private TableView<Patient> recentTable;
    @FXML private TableColumn<Patient, String> recentIdCol;
    @FXML private TableColumn<Patient, String> recentNameCol;
    @FXML private TableColumn<Patient, Integer> recentAgeCol;
    @FXML private TableColumn<Patient, String> recentStatusCol;

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem("dashboard");

        setupAdmissionsChart();
        setupStatusChart();
        setupRecentTable();
    }

    private void setupAdmissionsChart() {
        admissionsChart.setTitle("");
        admissionsChart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().addAll(
            new XYChart.Data<>("Jan", 120),
            new XYChart.Data<>("Feb", 145),
            new XYChart.Data<>("Mar", 132),
            new XYChart.Data<>("Apr", 160),
            new XYChart.Data<>("May", 178),
            new XYChart.Data<>("Jun", 155),
            new XYChart.Data<>("Jul", 191)
        );
        admissionsChart.getData().add(series);
    }

    private void setupStatusChart() {
        statusChart.getData().addAll(
            new PieChart.Data("Admitted",   40),
            new PieChart.Data("Discharged", 50),
            new PieChart.Data("Pending",    10)
        );
        statusChart.setLegendVisible(true);
        statusChart.setLabelsVisible(true);
    }

    private void setupRecentTable() {
        recentIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        recentNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        recentAgeCol.setCellValueFactory(new PropertyValueFactory<>("age"));
        recentStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        recentTable.setItems(FXCollections.observableArrayList(
            new Patient("P001", "Alice Johnson", 30, "Admitted",   "Female", "555-0101", ""),
            new Patient("P002", "Bob Smith",     45, "Discharged", "Male",   "555-0102", ""),
            new Patient("P003", "Clara Davis",   28, "Pending",    "Female", "555-0103", ""),
            new Patient("P004", "Daniel Brown",  52, "Admitted",   "Male",   "555-0104", ""),
            new Patient("P005", "Eva Martinez",  35, "Discharged", "Female", "555-0105", "")
        ));
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}