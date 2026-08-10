package hospital.management.pages.dashboard;

import hospital.management.backend.config.security.PermissionGate;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.pages.BasePageController;
import hospital.management.pages.QuickAddCapable;
import hospital.management.pages.components.shared.widgets.StatsWidgetController;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.patient.PatientFeedbackDAOImpl;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.service.patient.PatientFeedbackServiceImpl;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.patient.interfaces.PatientService;
import hospital.management.backend.service.patient.interfaces.PatientFeedbackService;
import hospital.management.backend.service.clinical.interfaces.AppointmentService;
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
import hospital.management.backend.utils.FxFormValidator;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
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

    // Patient feedback submission (for patients)
    @FXML private VBox feedbackSubmissionBox;
    @FXML private ComboBox<AppointmentDTO> feedbackAppointmentDropdown;
    @FXML private Spinner<Integer> feedbackRating;
    @FXML private TextArea feedbackComments;
    @FXML private Button submitFeedbackBtn;

    // Feedback display (for admins/doctors)
    @FXML private VBox feedbackDisplayBox;
    @FXML private ComboBox<PatientDTO> adminFeedbackPatientDropdown;
    @FXML private ComboBox<AppointmentDTO> adminFeedbackAppointmentDropdown;
    @FXML private Spinner<Integer> adminFeedbackRating;
    @FXML private TextArea adminFeedbackComments;
    @FXML private Button submitAdminFeedbackBtn;
    @FXML private TableView<PatientFeedbackDTO> feedbackTable;
    @FXML private TableColumn<PatientFeedbackDTO, String> feedbackSubmitterCol;
    @FXML private TableColumn<PatientFeedbackDTO, String> feedbackPatientCol;
    @FXML private TableColumn<PatientFeedbackDTO, String> feedbackAppointmentCol;
    @FXML private TableColumn<PatientFeedbackDTO, Integer> feedbackRatingCol;
    @FXML private TableColumn<PatientFeedbackDTO, String> feedbackCommentsCol;
    @FXML private TableColumn<PatientFeedbackDTO, String> feedbackDateCol;

    private final PatientService patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final UserService userService = new UserServiceImpl(new UserDAOImpl());
    private final RoleService roleService = new RoleServiceImpl(
        new RoleDAOImpl(), new UserRoleDAOImpl(), new RolePermissionDAOImpl(), new PermissionDAOImpl());
    private final DoctorService doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final PatientFeedbackService feedbackService = new PatientFeedbackServiceImpl(new PatientFeedbackDAOImpl());
    private final AppointmentService appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.DASHBOARD);
    setupFeedbackUI();
    
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
        
        Button btn = (Button) e.getSource();
        Node originalGraphic = btn.getGraphic();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(13, 13);
        btn.setGraphic(spinner);
        btn.setDisable(true);
        
        new Thread(() -> {
            try {
                hospital.management.backend.service.analytics.PerformanceBenchmarkService benchmarkService = 
                    new hospital.management.backend.service.analytics.PerformanceBenchmarkService();
                java.nio.file.Path reportPath = benchmarkService.generateBenchmarkReport();
                
                Platform.runLater(() -> {
                    btn.setGraphic(originalGraphic);
                    btn.setDisable(false);
                    
                    // Offer to open the report
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Performance Report Generated");
                    alert.setHeaderText("PostgreSQL vs MongoDB Benchmark");
                    alert.setContentText("Report saved to:\n" + reportPath + "\n\nWould you like to open it?");
                    
                    javafx.scene.control.ButtonType openBtn = new javafx.scene.control.ButtonType("Open");
                    javafx.scene.control.ButtonType closeBtn = new javafx.scene.control.ButtonType("Close", 
                        javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(openBtn, closeBtn);
                    
                    alert.showAndWait().ifPresent(response -> {
                        if (response == openBtn) {
                            try {
                                java.awt.Desktop.getDesktop().open(reportPath.toFile());
                            } catch (Exception ex) {
                                toastError("Could not open report: " + ex.getMessage());
                            }
                        }
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btn.setGraphic(originalGraphic);
                    btn.setDisable(false);
                    toastError("Failed to generate report: " + ex.getMessage());
                });
            }
        }).start();
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

    /** Safe cross-table user lookup: patients → doctors → users (returns null if not found). */
    private PatientDTO findPatientByEmailSafe(String email) {
        try { return patientService.findByEmail(email); } catch (Exception ignored) { return null; }
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

    // ── Feedback Feature ──────────────────────────────────────────────────────

    private void setupFeedbackUI() {
        try {
            String userId = SessionManager.getCurrentUserId();
            List<RoleDTO> roles = roleService.findRolesForUser(userId);
            boolean isPatient = roles.stream().anyMatch(r -> "patient".equalsIgnoreCase(r.getRoleName()));
            boolean isAdminOrDoctor = roles.stream().anyMatch(r -> 
                "admin".equalsIgnoreCase(r.getRoleName()) || "doctor".equalsIgnoreCase(r.getRoleName()));

            // Show feedback submission for patients
            if (feedbackSubmissionBox != null) {
                feedbackSubmissionBox.setVisible(isPatient);
                feedbackSubmissionBox.setManaged(isPatient);
                if (isPatient) {
                    setupFeedbackSubmission(userId);
                }
            }

            // Show feedback display for admin/doctor
            if (feedbackDisplayBox != null) {
                feedbackDisplayBox.setVisible(isAdminOrDoctor);
                feedbackDisplayBox.setManaged(isAdminOrDoctor);
                if (isAdminOrDoctor) {
                    setupAdminFeedbackCreation(userId);
                    setupFeedbackDisplay();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to setup feedback UI: " + e.getMessage());
        }
    }

    private void setupFeedbackSubmission(String userId) {
        if (feedbackRating != null) {
            SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5);
            feedbackRating.setValueFactory(valueFactory);
            feedbackRating.setEditable(true);
        }
        if (feedbackComments != null) {
            feedbackComments.setPromptText("e.g. The doctor was very attentive and helpful. (required)");
            FxFormValidator.attachRequired(feedbackComments, null, "Comments");
        }

        // Load patient's appointments into dropdown
        if (feedbackAppointmentDropdown != null) {
            try {
                UserDTO user = userService.findById(userId);
                PatientDTO patient = findPatientByEmailSafe(user.getEmail());
                
                if (patient != null) {
                    List<AppointmentDTO> appointments = appointmentService.findByPatient(patient.getPatientId());
                    
                    // Set custom cell factory to show appointment details
                    feedbackAppointmentDropdown.setCellFactory(lv -> new javafx.scene.control.ListCell<AppointmentDTO>() {
                        @Override
                        protected void updateItem(AppointmentDTO item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                String dateStr = item.getAppointmentDate() != null 
                                    ? item.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                                    : "No date";
                                setText(dateStr + " - " + (item.getReason() != null ? item.getReason() : "No reason"));
                            }
                        }
                    });
                    
                    // Set button cell factory for selected item display
                    feedbackAppointmentDropdown.setButtonCell(new javafx.scene.control.ListCell<AppointmentDTO>() {
                        @Override
                        protected void updateItem(AppointmentDTO item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText("Select an appointment (optional)");
                            } else {
                                String dateStr = item.getAppointmentDate() != null 
                                    ? item.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                                    : "No date";
                                setText(dateStr);
                            }
                        }
                    });
                    
                    feedbackAppointmentDropdown.setItems(FXCollections.observableArrayList(appointments));
                }
            } catch (Exception e) {
                System.err.println("Failed to load appointments: " + e.getMessage());
            }
        }

        if (submitFeedbackBtn != null) {
            submitFeedbackBtn.setOnAction(e -> handleSubmitFeedback(userId));
        }
    }

    @FXML
    private void handleSubmitFeedback(String userId) {
        if (feedbackRating == null || feedbackComments == null) return;

        Integer rating = feedbackRating.getValue();
        String comments = feedbackComments.getText() == null ? "" : feedbackComments.getText().trim();

        if (comments.isEmpty()) {
            toastError("Please enter your feedback comments.");
            FxFormValidator.applyStyle(feedbackComments, false);
            return;
        }
        if (rating == null || rating < 1 || rating > 5) {
            toastError("Please select a rating between 1 and 5.");
            return;
        }

        withSpinner(submitFeedbackBtn, () -> {
            try {
                UserDTO user = userService.findById(userId);
                // A user may exist in users, doctors, or patients table — look up safely
                PatientDTO patient = findPatientByEmailSafe(user.getEmail());
                String patientId = patient != null ? patient.getPatientId() : null;

                PatientFeedbackDTO feedback = new PatientFeedbackDTO();
                feedback.setSubmittedBy(userId);  // Track who submitted the feedback
                feedback.setPatientId(patientId);
                feedback.setRating(rating);
                feedback.setComments(comments);
                feedback.setDateSubmitted(LocalDate.now());
                
                // Get selected appointment if any
                if (feedbackAppointmentDropdown != null) {
                    AppointmentDTO selectedAppt = feedbackAppointmentDropdown.getValue();
                    if (selectedAppt != null) {
                        feedback.setAppointmentId(selectedAppt.getAppointmentId());
                    }
                }

                feedbackService.submitFeedback(feedback);
                
                feedbackComments.clear();
                feedbackRating.getValueFactory().setValue(5);
                if (feedbackAppointmentDropdown != null) {
                    feedbackAppointmentDropdown.getSelectionModel().clearSelection();
                }
                
                toastSuccess("Thank you for your feedback!");
            } catch (Exception e) {
                toastError("Failed to submit feedback: " + e.getMessage());
            }
        });
    }

    private void setupAdminFeedbackCreation(String userId) {
        // Wire appointment dropdown cell factories once (reused every time patient changes)
        if (adminFeedbackAppointmentDropdown != null) {
            adminFeedbackAppointmentDropdown.setCellFactory(lv -> new javafx.scene.control.ListCell<AppointmentDTO>() {
                @Override protected void updateItem(AppointmentDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    String d = item.getAppointmentDate() != null
                        ? item.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "No date";
                    setText(d + "  ·  " + (item.getReason() != null ? item.getReason() : "—"));
                }
            });
            adminFeedbackAppointmentDropdown.setButtonCell(new javafx.scene.control.ListCell<AppointmentDTO>() {
                @Override protected void updateItem(AppointmentDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "Select appointment (optional)"
                        : (item.getAppointmentDate() != null
                            ? item.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—"));
                }
            });
        }

        // Setup patient dropdown — show "Full Name  ·  ID-prefix" for easy identification
        if (adminFeedbackPatientDropdown != null) {
            try {
                List<PatientDTO> patients = patientService.findAll(CursorPagination.firstPage(1000)).getItems();

                adminFeedbackPatientDropdown.setCellFactory(lv -> new javafx.scene.control.ListCell<PatientDTO>() {
                    @Override protected void updateItem(PatientDTO item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); return; }
                        String shortId = item.getPatientId() != null && item.getPatientId().length() >= 8
                            ? item.getPatientId().substring(0, 8) + "…" : item.getPatientId();
                        setText(item.getFullName() + "  ·  ID: " + shortId);
                    }
                });

                adminFeedbackPatientDropdown.setButtonCell(new javafx.scene.control.ListCell<PatientDTO>() {
                    @Override protected void updateItem(PatientDTO item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText("Select a patient"); return; }
                        String shortId = item.getPatientId() != null && item.getPatientId().length() >= 8
                            ? item.getPatientId().substring(0, 8) + "…" : item.getPatientId();
                        setText(item.getFullName() + " (" + shortId + ")");
                    }
                });

                adminFeedbackPatientDropdown.setItems(FXCollections.observableArrayList(patients));

                // When patient changes: clear appointment list then reload for selected patient
                adminFeedbackPatientDropdown.setOnAction(e -> {
                    PatientDTO selected = adminFeedbackPatientDropdown.getValue();
                    if (adminFeedbackAppointmentDropdown != null) {
                        adminFeedbackAppointmentDropdown.getSelectionModel().clearSelection();
                        adminFeedbackAppointmentDropdown.setItems(FXCollections.observableArrayList());
                    }
                    if (selected != null && adminFeedbackAppointmentDropdown != null) {
                        try {
                            List<AppointmentDTO> appointments = appointmentService.findByPatient(selected.getPatientId());
                            adminFeedbackAppointmentDropdown.setItems(FXCollections.observableArrayList(appointments));
                        } catch (Exception ex) {
                            System.err.println("Failed to load appointments for patient: " + ex.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Failed to load patients: " + e.getMessage());
            }
        }
        
        // Setup rating spinner
        if (adminFeedbackRating != null) {
            SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5);
            adminFeedbackRating.setValueFactory(valueFactory);
            adminFeedbackRating.setEditable(true);
        }
        if (adminFeedbackComments != null) {
            adminFeedbackComments.setPromptText("e.g. Patient responded well to treatment. (required)");
            FxFormValidator.attachRequired(adminFeedbackComments, null, "Comments");
        }
        
        // Setup submit button
        if (submitAdminFeedbackBtn != null) {
            submitAdminFeedbackBtn.setOnAction(e -> handleSubmitAdminFeedback(userId));
        }
    }

    private void handleSubmitAdminFeedback(String userId) {
        if (adminFeedbackPatientDropdown == null || adminFeedbackRating == null || adminFeedbackComments == null) {
            return;
        }

        PatientDTO selectedPatient = adminFeedbackPatientDropdown.getValue();
        Integer rating = adminFeedbackRating.getValue();
        String commentsRaw = adminFeedbackComments.getText();
        String comments    = commentsRaw == null ? "" : commentsRaw.trim();

        if (selectedPatient == null) {
            toastError("Please select a patient.");
            return;
        }
        if (rating == null || rating < 1 || rating > 5) {
            toastError("Please select a rating between 1 and 5.");
            return;
        }
        if (comments.isEmpty()) {
            toastError("Please enter comments.");
            FxFormValidator.applyStyle(adminFeedbackComments, false);
            return;
        }

        withSpinner(submitAdminFeedbackBtn, () -> {
            try {
                PatientFeedbackDTO feedback = new PatientFeedbackDTO();
                feedback.setSubmittedBy(userId);
                feedback.setPatientId(selectedPatient.getPatientId());
                feedback.setRating(rating);
                feedback.setComments(comments);
                feedback.setDateSubmitted(LocalDate.now());
                
                // Get selected appointment if any
                if (adminFeedbackAppointmentDropdown != null) {
                    AppointmentDTO selectedAppt = adminFeedbackAppointmentDropdown.getValue();
                    if (selectedAppt != null) {
                        feedback.setAppointmentId(selectedAppt.getAppointmentId());
                    }
                }

                feedbackService.submitFeedback(feedback);
                
                adminFeedbackPatientDropdown.getSelectionModel().clearSelection();
                if (adminFeedbackAppointmentDropdown != null) {
                    adminFeedbackAppointmentDropdown.getSelectionModel().clearSelection();
                }
                adminFeedbackComments.clear();
                adminFeedbackRating.getValueFactory().setValue(5);
                
                loadFeedback();  // Refresh the table
                toastSuccess("Feedback submitted successfully!");
            } catch (Exception ex) {
                toastError("Failed to submit feedback: " + ex.getMessage());
            }
        });
    }

    private void setupFeedbackDisplay() {
        // Submitter column - look up in users/patients/doctors
        if (feedbackSubmitterCol != null) {
            feedbackSubmitterCol.setCellValueFactory(cell -> {
                try {
                    String submitterId = cell.getValue().getSubmittedBy();
                    if (submitterId == null) return new SimpleStringProperty("Unknown");
                    
                    // Try to find user first
                    try {
                        UserDTO user = userService.findById(submitterId);
                        if (user != null) {
                            // Try to get more specific info from patient or doctor tables
                            try {
                                PatientDTO patient = patientService.findByEmail(user.getEmail());
                                if (patient != null) {
                                    return new SimpleStringProperty(patient.getFullName() + " (Patient)");
                                }
                            } catch (Exception ignored) {}
                            
                            try {
                                DoctorDTO doctor = doctorService.findByEmail(user.getEmail());
                                if (doctor != null) {
                                    return new SimpleStringProperty(doctor.getFullName() + " (Doctor)");
                                }
                            } catch (Exception ignored) {}
                            
                            // Fallback to username
                            return new SimpleStringProperty(user.getUsername());
                        }
                    } catch (Exception ignored) {}
                    
                    return new SimpleStringProperty("Unknown");
                } catch (Exception e) {
                    return new SimpleStringProperty("Error");
                }
            });
        }
        
        // Patient column — "Full Name (ID: xxxxxxxx…)" or "General" when null
        if (feedbackPatientCol != null) {
            feedbackPatientCol.setCellValueFactory(cell -> {
                String patientId = cell.getValue().getPatientId();
                if (patientId == null || patientId.isEmpty()) {
                    return new SimpleStringProperty("General");
                }
                String shortId = patientId.length() >= 8 ? patientId.substring(0, 8) + "…" : patientId;
                try {
                    PatientDTO patient = patientService.findById(patientId);
                    String name = (patient != null && patient.getFullName() != null) ? patient.getFullName() : "Unknown";
                    return new SimpleStringProperty(name + " (ID: " + shortId + ")");
                } catch (Exception e) {
                    return new SimpleStringProperty("ID: " + shortId);
                }
            });
        }
        
        feedbackAppointmentCol.setCellValueFactory(cell -> {
            String apptId = cell.getValue().getAppointmentId();
            if (apptId == null || apptId.isEmpty()) {
                return new SimpleStringProperty("General");
            }
            try {
                AppointmentDTO appt = appointmentService.findById(apptId);
                if (appt != null && appt.getAppointmentDate() != null) {
                    String dateStr = appt.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                    return new SimpleStringProperty(dateStr);
                }
                return new SimpleStringProperty("—");
            } catch (Exception e) {
                return new SimpleStringProperty("—");
            }
        });
        
        feedbackRatingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        feedbackCommentsCol.setCellValueFactory(new PropertyValueFactory<>("comments"));
        feedbackDateCol.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getDateSubmitted();
            String formatted = date != null ? date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
            return new SimpleStringProperty(formatted);
        });

        loadFeedback();
    }

    private void loadFeedback() {
        try {
            List<PatientFeedbackDTO> allFeedback = feedbackService.findAll();
            feedbackTable.setItems(FXCollections.observableArrayList(allFeedback));
        } catch (Exception e) {
            feedbackTable.setItems(FXCollections.observableArrayList());
            System.err.println("Failed to load feedback: " + e.getMessage());
        }
        feedbackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}