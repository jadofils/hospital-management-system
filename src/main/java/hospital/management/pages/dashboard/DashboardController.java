package hospital.management.pages.dashboard;

import hospital.management.pages.components.shared.layout.RightSidebarController;
import hospital.management.pages.components.shared.layout.SidebarController;
import hospital.management.pages.components.shared.widgets.StatsWidgetController;
import hospital.management.backend.model.patient.Patient;
import hospital.management.enums.PageRoute;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.Period;

public class DashboardController {

    @FXML private SidebarController sidebarController;
    @FXML private RightSidebarController rightSidebarController;
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
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.DASHBOARD);

        setupAdmissionsChart();
        setupStatusChart();
        setupRecentTable();
    }

    private void setupAdmissionsChart() {
        admissionsChart.setTitle("");
        admissionsChart.setLegendVisible(false);
        admissionsChart.getData().add(new XYChart.Series<>());
    }

    private void setupStatusChart() {
        statusChart.setLegendVisible(true);
        statusChart.setLabelsVisible(true);
    }

    private void setupRecentTable() {
        recentIdCol.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        recentNameCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFullName()));
        recentAgeCol.setCellValueFactory(cell -> {
            LocalDate dob = cell.getValue().getDob();
            int age = (dob != null) ? Period.between(dob, LocalDate.now()).getYears() : 0;
            return new SimpleIntegerProperty(age).asObject();
        });
        recentStatusCol.setCellValueFactory(cell -> new SimpleStringProperty("—"));
        recentTable.setItems(FXCollections.observableArrayList());
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}