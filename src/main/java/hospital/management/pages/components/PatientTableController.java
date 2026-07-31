package hospital.management.pages.components;

import hospital.management.backend.model.patient.Patient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class PatientTableController {

    private static final int ROWS_PER_PAGE = 10;

    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, String> idColumn;
    @FXML private TableColumn<Patient, String> nameColumn;
    @FXML private TableColumn<Patient, Integer> ageColumn;
    @FXML private TableColumn<Patient, String> genderColumn;
    @FXML private TableColumn<Patient, String> phoneColumn;
    @FXML private TableColumn<Patient, String> statusColumn;
    @FXML private Pagination pagination;

    private FilteredList<Patient> filteredPatients;

    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        ObservableList<Patient> patients = FXCollections.observableArrayList(
            new Patient("P001", "Alice Johnson", 30, "Admitted",   "Female", "555-0101", "alice@example.com"),
            new Patient("P002", "Bob Smith",     45, "Discharged", "Male",   "555-0102", "bob@example.com"),
            new Patient("P003", "Clara Davis",   28, "Pending",    "Female", "555-0103", "clara@example.com"),
            new Patient("P004", "Daniel Brown",  52, "Admitted",   "Male",   "555-0104", "daniel@example.com"),
            new Patient("P005", "Eva Martinez",  35, "Discharged", "Female", "555-0105", "eva@example.com"),
            new Patient("P006", "Frank Wilson",  41, "Pending",    "Male",   "555-0106", "frank@example.com"),
            new Patient("P007", "Grace Lee",     26, "Admitted",   "Female", "555-0107", "grace@example.com")
        );

        filteredPatients = new FilteredList<>(patients, p -> true);
        patientTable.setItems(filteredPatients);
        patientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        pagination.setPageCount(Math.max(1, (int) Math.ceil(filteredPatients.size() / (double) ROWS_PER_PAGE)));
    }

    public void filter(String query) {
        String lower = query == null ? "" : query.trim().toLowerCase();
        filteredPatients.setPredicate(patient ->
            lower.isEmpty()
                || patient.getName().toLowerCase().contains(lower)
                || patient.getId().toLowerCase().contains(lower)
        );
        pagination.setPageCount(Math.max(1, (int) Math.ceil(filteredPatients.size() / (double) ROWS_PER_PAGE)));
        pagination.setCurrentPageIndex(0);
    }
}
