package hospital.management.pages.components;

import hospital.management.model.Patient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class TableController {

    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, String> idColumn;
    @FXML private TableColumn<Patient, String> nameColumn;
    @FXML private TableColumn<Patient, Integer> ageColumn;
    @FXML private TableColumn<Patient, String> statusColumn;

    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        ObservableList<Patient> patients = FXCollections.observableArrayList(
                new Patient("P001", "Alice Johnson", 30, "Admitted", "Female", "555-0101", "alice@example.com"),
                new Patient("P002", "Bob Smith",     45, "Discharged", "Male", "555-0102", "bob@example.com"),
                new Patient("P003", "Clara Davis",   28, "Pending",    "Female", "555-0103", "clara@example.com")
        );
        patientTable.setItems(patients);
    }
}