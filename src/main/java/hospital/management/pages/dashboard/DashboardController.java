package hospital.management.pages.dashboard;

import hospital.management.backend.config.security.PermissionGate;
import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.pages.components.shared.widgets.StatsWidgetController;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.service.patient.interfaces.PatientService;
import hospital.management.backend.dao.auth.PermissionDAOImpl;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.RolePermissionDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.service.auth.RoleServiceImpl;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.RoleService;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.department.interfaces.DoctorService;
import hospital.management.backend.utils.pagination.CursorPagination;
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
import javafx.scene.layout.HBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DashboardController extends BasePageController {

    private static final int DIRECTORY_PAGE_SIZE = 500;

    @FXML private BorderPane dashboardRoot;
    @FXML private HBox quickActionsBox;

    @FXML private StatsWidgetController statsWidgetController;

    @FXML private BarChart<String, Number> admissionsChart;
    @FXML private CategoryAxis admissionsXAxis;
    @FXML private NumberAxis admissionsYAxis;

    @FXML private PieChart statusChart;

    @FXML private TableView<PatientDTO> recentTable;
    @FXML private TableColumn<PatientDTO, String> recentIdCol;
    @FXML private TableColumn<PatientDTO, String> recentNameCol;
    @FXML private TableColumn<PatientDTO, Integer> recentAgeCol;
    @FXML private TableColumn<PatientDTO, String> recentStatusCol;
    @FXML private Button newPatientBtn;
    @FXML private Button newAppointmentBtn;
    @FXML private Button generateReportBtn;
    @FXML private Button processBillingBtn;

    @FXML private VBox teamDirectoryBox;
    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, String> userUsernameCol;
    @FXML private TableColumn<UserDTO, String> userEmailCol;
    @FXML private TableColumn<UserDTO, String> userRoleCol;
    @FXML private TableView<DoctorDTO> doctorsTable;
    @FXML private TableColumn<DoctorDTO, String> doctorNameCol;
    @FXML private TableColumn<DoctorDTO, String> doctorEmailCol;
    @FXML private TableColumn<DoctorDTO, String> doctorSpecCol;

    private final PatientService patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    private final RoleService roleService = new RoleServiceImpl(
        new RoleDAOImpl(), new UserRoleDAOImpl(), new RolePermissionDAOImpl(), new PermissionDAOImpl());
    private final DoctorService doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.DASHBOARD);

        if (!PermissionGate.isAllowed(PageRoute.DASHBOARD)) {
            if (quickActionsBox != null) {
                quickActionsBox.setVisible(false);
                quickActionsBox.setManaged(false);
            }
            toastError("You don't have permission to view the dashboard.");
        }

        applyQuickActionPermissions();

        setupAdmissionsChart();
        setupStatusChart();
        setupRecentTable();
        setupTeamDirectory();
    }

    private void applyQuickActionPermissions() {
        setVisibleIfAllowed(newPatientBtn, PageRoute.PATIENTS);
        setVisibleIfAllowed(newAppointmentBtn, PageRoute.APPOINTMENTS);
        setVisibleIfAllowed(generateReportBtn, PageRoute.ANALYTICS);
        setVisibleIfAllowed(processBillingBtn, PageRoute.BILLING);

        if (quickActionsBox != null) {
            boolean any = quickActionsBox.getChildren().stream().anyMatch(node -> node instanceof Button b && b.isVisible());
            quickActionsBox.setVisible(any);
            quickActionsBox.setManaged(any);
        }
    }

    private void setVisibleIfAllowed(Button button, PageRoute route) {
        if (button == null) return;
        boolean allowed = PermissionGate.isAllowed(route);
        button.setVisible(allowed);
        button.setManaged(allowed);
        button.setDisable(!allowed);
    }

    @FXML
    private void handleNewPatient(javafx.event.ActionEvent e) {
        if (!PermissionGate.isAllowed(PageRoute.PATIENTS)) { toastError("Access denied."); return; }
        navigateAndOpenAdd(PageRoute.PATIENTS, (Button) e.getSource());
    }

    @FXML
    private void handleNewAppointment(javafx.event.ActionEvent e) {
        if (!PermissionGate.isAllowed(PageRoute.APPOINTMENTS)) { toastError("Access denied."); return; }
        navigateAndOpenAdd(PageRoute.APPOINTMENTS, (Button) e.getSource());
    }

    @FXML
    private void handleProcessBilling(javafx.event.ActionEvent e) {
        if (!PermissionGate.isAllowed(PageRoute.BILLING)) { toastError("Access denied."); return; }
        navigateAndOpenAdd(PageRoute.BILLING, (Button) e.getSource());
    }

    @FXML
    private void handleGenerateReport(javafx.event.ActionEvent e) {
        if (!PermissionGate.isAllowed(PageRoute.ANALYTICS)) { toastError("Access denied."); return; }
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
        try {
            var patients = patientService.findAll(CursorPagination.firstPage(10)).getItems();
            recentTable.setItems(FXCollections.observableArrayList(patients));
        } catch (Exception e) {
            recentTable.setItems(FXCollections.observableArrayList());
        }
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /** Admin-only directory of every account and doctor, so different doctor logins can be
     *  identified for testing without querying the database directly. */
    private void setupTeamDirectory() {
        boolean visible = canRead(PageRoute.USERS) && canRead(PageRoute.DOCTORS);
        teamDirectoryBox.setVisible(visible);
        teamDirectoryBox.setManaged(visible);
        if (!visible) return;

        userUsernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        userRoleCol.setCellValueFactory(cell -> {
            try {
                List<RoleDTO> roles = roleService.findRolesForUser(cell.getValue().getUserId());
                return new SimpleStringProperty(roles.isEmpty() ? "—" : roles.get(0).getRoleName());
            } catch (Exception e) {
                return new SimpleStringProperty("—");
            }
        });
        List<UserDTO> users = List.of();
        try {
            users = fetchAllUsers();
            usersTable.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) {
            usersTable.setItems(FXCollections.observableArrayList());
        }
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        doctorNameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        doctorEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        doctorSpecCol.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        try {
            var doctors = fetchDoctorsForDirectory(users);
            doctorsTable.setItems(FXCollections.observableArrayList(doctors));
        } catch (Exception e) {
            doctorsTable.setItems(FXCollections.observableArrayList());
        }
        doctorsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private List<DoctorDTO> fetchDoctorsForDirectory(List<UserDTO> users) throws Exception {
        List<DoctorDTO> doctors = new java.util.ArrayList<>(fetchAllDoctors());
        Set<String> knownDoctorIds = new HashSet<>();
        Set<String> knownEmails = new HashSet<>();
        for (DoctorDTO doctor : doctors) {
            if (doctor.getDoctorId() != null) knownDoctorIds.add(doctor.getDoctorId());
            if (doctor.getEmail() != null) knownEmails.add(doctor.getEmail().toLowerCase());
        }

        for (UserDTO user : users) {
            List<RoleDTO> roles = roleService.findRolesForUser(user.getUserId());
            boolean isDoctorRole = roles.stream().anyMatch(r -> "doctor".equalsIgnoreCase(r.getRoleName()));
            if (!isDoctorRole) continue;

            String linkedDoctorId = user.getDoctorId();
            String email = user.getEmail() == null ? null : user.getEmail().toLowerCase();
            boolean alreadyRepresented = (linkedDoctorId != null && knownDoctorIds.contains(linkedDoctorId))
                    || (email != null && knownEmails.contains(email));
            if (alreadyRepresented) continue;

            DoctorDTO synthetic = new DoctorDTO();
            synthetic.setDoctorId(linkedDoctorId != null ? linkedDoctorId : "account:" + user.getUserId());
            synthetic.setFirstName(user.getUsername());
            synthetic.setLastName("");
            synthetic.setEmail(user.getEmail());
            synthetic.setSpecialization("Account only (create doctor profile)");
            doctors.add(synthetic);
        }
        return doctors;
    }

    private List<UserDTO> fetchAllUsers() throws Exception {
        List<UserDTO> all = new java.util.ArrayList<>();
        var page = userService.findAll(CursorPagination.firstPage(DIRECTORY_PAGE_SIZE));
        all.addAll(page.getItems());
        while (page.hasMore() && page.getNextCursor() != null) {
            page = userService.findAll(CursorPagination.nextPage(page.getNextCursor(), DIRECTORY_PAGE_SIZE));
            all.addAll(page.getItems());
        }
        return all;
    }

    private List<DoctorDTO> fetchAllDoctors() throws Exception {
        List<DoctorDTO> all = new java.util.ArrayList<>();
        var page = doctorService.findAll(CursorPagination.firstPage(DIRECTORY_PAGE_SIZE));
        all.addAll(page.getItems());
        while (page.hasMore() && page.getNextCursor() != null) {
            page = doctorService.findAll(CursorPagination.nextPage(page.getNextCursor(), DIRECTORY_PAGE_SIZE));
            all.addAll(page.getItems());
        }
        return all;
    }
}