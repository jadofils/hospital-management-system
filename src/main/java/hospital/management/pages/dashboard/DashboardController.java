package hospital.management.pages.dashboard;

import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.pages.components.shared.widgets.StatsWidgetController;
import hospital.management.backend.model.patient.Patient;
import hospital.management.enums.PageRoute;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.Period;

public class DashboardController extends BasePageController {

    @FXML private BorderPane dashboardRoot;

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

    @FXML
    private void handleNewPatient(javafx.event.ActionEvent e) {
        navigateAndOpenAdd(PageRoute.PATIENTS, (Button) e.getSource());
    }

    @FXML
    private void handleNewAppointment(javafx.event.ActionEvent e) {
        navigateAndOpenAdd(PageRoute.APPOINTMENTS, (Button) e.getSource());
    }

    @FXML
    private void handleProcessBilling(javafx.event.ActionEvent e) {
        navigateAndOpenAdd(PageRoute.BILLING, (Button) e.getSource());
    }

    @FXML
    private void handleGenerateReport(javafx.event.ActionEvent e) {
        navigateTo(PageRoute.ANALYTICS, (Button) e.getSource());
    }

    /** Loads the target page and, if it supports it, immediately opens its Add dialog. */
    private void navigateAndOpenAdd(PageRoute route, Button source) {
        navigate(route, source, controller -> {
            if (controller instanceof QuickAddCapable quickAdd) {
                quickAdd.openAddDialog();
            }
        });
    }

    private void navigateTo(PageRoute route, Button source) {
        navigate(route, source, controller -> {});
    }

    private void navigate(PageRoute route, Button source, java.util.function.Consumer<Object> onLoaded) {
        Node originalGraphic = source.getGraphic();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(14, 14);
        source.setGraphic(spinner);
        source.setDisable(true);

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(route.getFxmlPath()));
                Parent root = loader.load();
                Scene scene = dashboardRoot.getScene();
                Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
                newScene.getStylesheets().add(
                    getClass().getResource("/hospital/management/css/global.css").toExternalForm()
                );
                ((Stage) scene.getWindow()).setScene(newScene);
                onLoaded.accept(loader.getController());
            } catch (Exception ex) {
                System.err.println("Navigation to " + route.getFxmlPath() + " failed: " + ex.getMessage());
                toastError("Couldn't open that page. Please try again.");
                source.setDisable(false);
                source.setGraphic(originalGraphic);
            }
        });
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