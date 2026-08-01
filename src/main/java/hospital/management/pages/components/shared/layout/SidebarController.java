package hospital.management.pages.components.shared.layout;

import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dao.auth.UserSessionDAOImpl;
import hospital.management.backend.dao.log.AuditLogDAOImpl;
import hospital.management.backend.model.enums.RoleName;
import hospital.management.backend.service.auth.AuthServiceImpl;
import hospital.management.backend.service.auth.interfaces.AuthService;
import hospital.management.enums.PageRoute;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SidebarController {

    private final AuthService authService = new AuthServiceImpl(
        new UserDAOImpl(), new UserSessionDAOImpl(), new UserRoleDAOImpl(),
        new RoleDAOImpl(), new AuditLogDAOImpl());

    // ── Section containers ────────────────────────────────────────────────
    @FXML private VBox sidebarRoot;
    @FXML private VBox mainSection;
    @FXML private VBox patientSection;
    @FXML private VBox clinicalSection;
    @FXML private VBox pharmacySection;
    @FXML private VBox analyticsSection;
    @FXML private VBox adminSection;
    @FXML private VBox accountSection;

    // ── Section items (collapsible content) ──────────────────────────────
    @FXML private VBox patientItems;
    @FXML private VBox clinicalItems;
    @FXML private VBox pharmacyItems;
    @FXML private VBox analyticsItems;
    @FXML private VBox adminItems;

    // ── Section header buttons ────────────────────────────────────────────
    @FXML private Button patientHeaderBtn;
    @FXML private Button clinicalHeaderBtn;
    @FXML private Button pharmacyHeaderBtn;
    @FXML private Button analyticsHeaderBtn;
    @FXML private Button adminHeaderBtn;

    // ── Section chevron icons ─────────────────────────────────────────────
    @FXML private FontIcon patientChevron;
    @FXML private FontIcon clinicalChevron;
    @FXML private FontIcon pharmacyChevron;
    @FXML private FontIcon analyticsChevron;
    @FXML private FontIcon adminChevron;

    // ── Collapse control ──────────────────────────────────────────────────
    @FXML private Button   collapseBtn;
    @FXML private FontIcon collapseIcon;

    // ── Navigation buttons ────────────────────────────────────────────────
    @FXML private Button dashboardBtn;
    @FXML private Button patientsBtn;
    @FXML private Button appointmentsBtn;
    @FXML private Button billingBtn;
    @FXML private Button doctorsBtn;
    @FXML private Button appointmentsDoctorBtn;
    @FXML private Button medicalRecordsBtn;
    @FXML private Button prescriptionsBtn;
    @FXML private Button labOrdersBtn;
    @FXML private Button referralsBtn;
    @FXML private Button scheduleBtn;
    @FXML private Button prescriptionsQueueBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button analyticsBtn;
    @FXML private Button feedbackBtn;
    @FXML private Button usersBtn;
    @FXML private Button rolesBtn;
    @FXML private Button departmentsBtn;
    @FXML private Button systemLogsBtn;
    @FXML private Button auditLogsBtn;
    @FXML private Button retentionBtn;
    @FXML private Button profileBtn;
    @FXML private Button logoutBtn;

    private boolean collapsed = false;

    // Tracks expanded state per items-VBox so sidebar collapse/expand can restore it
    private final Map<VBox, Boolean> expandedState = new HashMap<>();

    public void initialize() {
        // All sections start expanded
        expandedState.put(patientItems,   true);
        expandedState.put(clinicalItems,  true);
        expandedState.put(pharmacyItems,  true);
        expandedState.put(analyticsItems, true);
        expandedState.put(adminItems,     true);
    }

    // ── Section accordion toggles ─────────────────────────────────────────

    @FXML private void togglePatientSection()   { toggleSection(patientItems,   patientChevron); }
    @FXML private void toggleClinicalSection()  { toggleSection(clinicalItems,  clinicalChevron); }
    @FXML private void togglePharmacySection()  { toggleSection(pharmacyItems,  pharmacyChevron); }
    @FXML private void toggleAnalyticsSection() { toggleSection(analyticsItems, analyticsChevron); }
    @FXML private void toggleAdminSection()     { toggleSection(adminItems,     adminChevron); }

    private void toggleSection(VBox items, FontIcon chevron) {
        boolean nowExpanded = !items.isVisible();
        items.setVisible(nowExpanded);
        items.setManaged(nowExpanded);
        chevron.setIconLiteral(nowExpanded ? "fas-chevron-down" : "fas-chevron-right");
        expandedState.put(items, nowExpanded);
    }

    // ── Role-based section visibility ─────────────────────────────────────

    public void configureForRole(RoleName role) {
        switch (role) {
            case ADMIN -> {
                show(mainSection, patientSection, analyticsSection, adminSection, accountSection);
                hide(clinicalSection, pharmacySection);
            }
            case DOCTOR -> {
                show(mainSection, patientSection, clinicalSection, accountSection);
                hide(billingBtn, doctorsBtn);
                hide(pharmacySection, analyticsSection, adminSection);
            }
            case RECEPTIONIST -> {
                show(mainSection, patientSection, accountSection);
                hide(billingBtn, doctorsBtn);
                hide(clinicalSection, pharmacySection, analyticsSection, adminSection);
            }
            case ANALYST -> {
                show(mainSection, analyticsSection, accountSection);
                hide(patientSection, clinicalSection, pharmacySection, adminSection);
            }
            case PHARMACIST -> {
                show(mainSection, pharmacySection, accountSection);
                hide(patientSection, clinicalSection, analyticsSection, adminSection);
            }
            default -> {
                show(mainSection, accountSection);
                hide(patientSection, clinicalSection, pharmacySection, analyticsSection, adminSection);
            }
        }
    }

    // ── Active item highlight ─────────────────────────────────────────────

    public void setActiveItem(PageRoute route) {
        allNavButtons().forEach(b -> b.getStyleClass().remove("active"));
        Button target = switch (route) {
            case DASHBOARD       -> dashboardBtn;
            case PATIENTS        -> patientsBtn;
            case APPOINTMENTS    -> appointmentsBtn;
            case BILLING         -> billingBtn;
            case DOCTORS         -> doctorsBtn;
            case MEDICAL_RECORDS -> medicalRecordsBtn;
            case PRESCRIPTIONS   -> prescriptionsBtn;
            case LAB_ORDERS      -> labOrdersBtn;
            case REFERRALS       -> referralsBtn;
            case MY_SCHEDULE     -> scheduleBtn;
            case PHARMACY        -> inventoryBtn;
            case ANALYTICS, FEEDBACK -> analyticsBtn;
            case USERS           -> usersBtn;
            case ROLES            -> rolesBtn;
            case DEPARTMENTS     -> departmentsBtn;
            case SYSTEM_LOGS     -> systemLogsBtn;
            case AUDIT_LOGS      -> auditLogsBtn;
            case RETENTION       -> retentionBtn;
            case PROFILE         -> profileBtn;
            default              -> null;
        };
        if (target != null) target.getStyleClass().add("active");
    }

    // ── Sidebar collapse / expand ─────────────────────────────────────────

    private static final double COLLAPSED_WIDTH = 56;

    @FXML
    private void handleToggleCollapse() {
        collapsed = !collapsed;
        if (collapsed) {
            // CSS sets a min/max width for the expanded sidebar (~190-260px);
            // those constraints win over a smaller setPrefWidth() alone, so
            // the container never actually shrinks unless min/max are pinned
            // down too here.
            sidebarRoot.setMinWidth(COLLAPSED_WIDTH);
            sidebarRoot.setPrefWidth(COLLAPSED_WIDTH);
            sidebarRoot.setMaxWidth(COLLAPSED_WIDTH);
        } else {
            // Hand control back to sidebar.css's normal min/pref/max-width.
            sidebarRoot.setMinWidth(Region.USE_COMPUTED_SIZE);
            sidebarRoot.setPrefWidth(Region.USE_COMPUTED_SIZE);
            sidebarRoot.setMaxWidth(Region.USE_COMPUTED_SIZE);
        }

        ContentDisplay display = collapsed ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT;
        allNavButtons().forEach(btn -> {
            btn.setContentDisplay(display);
            btn.setTooltip(collapsed ? new Tooltip(btn.getText()) : null);
        });

        // Hide/show MAIN section label
        mainSection.getChildren().stream()
                .filter(n -> n instanceof javafx.scene.control.Label)
                .forEach(lbl -> { lbl.setVisible(!collapsed); lbl.setManaged(!collapsed); });

        // Hide/show ACCOUNT section label
        accountSection.getChildren().stream()
                .filter(n -> n instanceof javafx.scene.control.Label)
                .forEach(lbl -> { lbl.setVisible(!collapsed); lbl.setManaged(!collapsed); });

        // Section headers: hide in icon-only mode, show in expanded mode
        List<Button> sectionHeaders = List.of(
                patientHeaderBtn, clinicalHeaderBtn, pharmacyHeaderBtn,
                analyticsHeaderBtn, adminHeaderBtn);
        sectionHeaders.forEach(h -> { h.setVisible(!collapsed); h.setManaged(!collapsed); });

        // In icon-only mode: show all items directly (no headers to click)
        // In expanded mode: restore each section's remembered expanded/collapsed state
        List<VBox> allItems = List.of(patientItems, clinicalItems, pharmacyItems, analyticsItems, adminItems);
        if (collapsed) {
            allItems.forEach(v -> { v.setVisible(true); v.setManaged(true); });
        } else {
            allItems.forEach(v -> {
                boolean wasExpanded = expandedState.getOrDefault(v, true);
                v.setVisible(wasExpanded);
                v.setManaged(wasExpanded);
            });
        }

        collapseIcon.setIconLiteral(collapsed ? "fas-chevron-right" : "fas-chevron-left");
    }

    // ── Navigation handlers ───────────────────────────────────────────────

    @FXML private void handleDashboard()      { navigate(PageRoute.DASHBOARD, dashboardBtn); }
    @FXML private void handlePatients()       { navigate(PageRoute.PATIENTS, patientsBtn); }
    @FXML private void handleAppointments()   { navigate(PageRoute.APPOINTMENTS, appointmentsBtn); }
    @FXML private void handleBilling()        { navigate(PageRoute.BILLING, billingBtn); }
    @FXML private void handleDoctors()        { navigate(PageRoute.DOCTORS, doctorsBtn); }
    @FXML private void handleMedicalRecords() { navigate(PageRoute.MEDICAL_RECORDS, medicalRecordsBtn); }
    @FXML private void handlePrescriptions()  { navigate(PageRoute.PRESCRIPTIONS, prescriptionsBtn); }
    @FXML private void handleLabOrders()      { navigate(PageRoute.LAB_ORDERS, labOrdersBtn); }
    @FXML private void handleReferrals()      { navigate(PageRoute.REFERRALS, referralsBtn); }
    @FXML private void handleSchedule()       { navigate(PageRoute.MY_SCHEDULE, scheduleBtn); }
    @FXML private void handlePharmacy()       { navigate(PageRoute.PHARMACY, inventoryBtn); }
    @FXML private void handleAnalytics()      { navigate(PageRoute.ANALYTICS, analyticsBtn); }
    @FXML private void handleFeedback()       { navigate(PageRoute.FEEDBACK, feedbackBtn); }
    @FXML private void handleUsers()          { navigate(PageRoute.USERS, usersBtn); }
    @FXML private void handleRoles()          { navigate(PageRoute.ROLES, rolesBtn); }
    @FXML private void handleDepartments()    { navigate(PageRoute.DEPARTMENTS, departmentsBtn); }
    @FXML private void handleSystemLogs()     { navigate(PageRoute.SYSTEM_LOGS, systemLogsBtn); }
    @FXML private void handleAuditLogs()      { navigate(PageRoute.AUDIT_LOGS, auditLogsBtn); }
    @FXML private void handleRetention()      { navigate(PageRoute.RETENTION, retentionBtn); }
    @FXML private void handleProfile()        { navigate(PageRoute.PROFILE, profileBtn); }
    @FXML
    private void handleLogout() {
        try {
            String sessionId = SessionManager.getCurrentSessionId();
            if (sessionId != null) authService.logout(sessionId);
        } catch (Exception e) {
            System.err.println("Logout cleanup failed: " + e.getMessage());
            showErrorAlert("You've been signed out locally, but the server-side session couldn't be closed cleanly.");
        } finally {
            SessionManager.logout();
        }
        navigate(PageRoute.HOME, logoutBtn);
    }

    /** Swaps the clicked button's icon for a spinner until the target page has loaded. */
    private void navigate(PageRoute route, Button sourceBtn) {
        Node originalGraphic = sourceBtn.getGraphic();
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(14, 14);
        sourceBtn.setGraphic(spinner);
        allNavButtons().forEach(b -> b.setDisable(true));

        // Defer the actual (blocking) load one pulse so the spinner graphic
        // above is guaranteed to paint before the FX thread gets busy loading.
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(route.getFxmlPath()));
                Parent root = loader.load();
                Scene scene = sourceBtn.getScene();
                Scene newScene = new Scene(root, scene.getWidth(), scene.getHeight());
                newScene.getStylesheets().add(
                    getClass().getResource("/hospital/management/css/global.css").toExternalForm()
                );
                ((Stage) scene.getWindow()).setScene(newScene);
            } catch (Exception e) {
                System.err.println("Navigation to " + route.getFxmlPath() + " failed: " + e.getMessage());
                showErrorAlert("Couldn't open that page. Please try again.");
                allNavButtons().forEach(b -> b.setDisable(false));
                sourceBtn.setGraphic(originalGraphic);
            }
        });
    }

    /**
     * SidebarController is a shared component with no reference to whichever page's
     * toast/BasePageController happens to host it, so navigation-level failures (a
     * missing FXML, a broken server-side logout call) use a plain JavaFX Alert
     * instead of the app's custom toast — still real, visible feedback rather than
     * a console-only System.err that the user never sees.
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private List<Button> allNavButtons() {
        return List.of(dashboardBtn, patientsBtn, appointmentsBtn, billingBtn,
                doctorsBtn, appointmentsDoctorBtn, medicalRecordsBtn, prescriptionsBtn,
                labOrdersBtn, referralsBtn, scheduleBtn, prescriptionsQueueBtn,
                inventoryBtn, analyticsBtn, feedbackBtn, usersBtn, rolesBtn, departmentsBtn,
                systemLogsBtn, auditLogsBtn, retentionBtn, profileBtn, logoutBtn);
    }

    private void show(VBox... sections) {
        for (VBox s : sections) { s.setVisible(true);  s.setManaged(true); }
    }

    private void hide(VBox... sections) {
        for (VBox s : sections) { s.setVisible(false); s.setManaged(false); }
    }

    private void hide(Button... buttons) {
        for (Button b : buttons) { b.setVisible(false); b.setManaged(false); }
    }
}