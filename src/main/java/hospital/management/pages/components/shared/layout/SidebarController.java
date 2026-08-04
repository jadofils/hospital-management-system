package hospital.management.pages.components.shared.layout;

import hospital.management.backend.config.security.PermissionGate;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.dao.auth.RoleDAOImpl;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.auth.UserRoleDAOImpl;
import hospital.management.backend.dao.auth.UserSessionDAOImpl;
import hospital.management.backend.dao.log.AuditLogDAOImpl;
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
import hospital.management.backend.utils.pipes.AsyncJobRunner;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
        if (patientItems != null) expandedState.put(patientItems, true);
        if (clinicalItems != null) expandedState.put(clinicalItems, true);
        if (pharmacyItems != null) expandedState.put(pharmacyItems, true);
        if (analyticsItems != null) expandedState.put(analyticsItems, true);
        if (adminItems != null) expandedState.put(adminItems, true);

        // Centralized permission gate: only show pages this user can actually access.
        try {
            configureForRole(PermissionGate.currentRole());
        } catch (Exception e) {
            // No active session: hide navigable items except profile/logout.
            configureForRole(null);
        }
    }

    // ── Section accordion toggles ─────────────────────────────────────────

    @FXML private void togglePatientSection()   { toggleSection(patientItems,   patientChevron); }
    @FXML private void toggleClinicalSection()  { toggleSection(clinicalItems,  clinicalChevron); }
    @FXML private void togglePharmacySection()  { toggleSection(pharmacyItems,  pharmacyChevron); }
    @FXML private void toggleAnalyticsSection() { toggleSection(analyticsItems, analyticsChevron); }
    @FXML private void toggleAdminSection()     { toggleSection(adminItems,     adminChevron); }

    private void toggleSection(VBox items, FontIcon chevron) {
        if (items == null || chevron == null) return;
        boolean nowExpanded = !items.isVisible();
        items.setVisible(nowExpanded);
        items.setManaged(nowExpanded);
        chevron.setIconLiteral(nowExpanded ? "fas-chevron-down" : "fas-chevron-right");
        expandedState.put(items, nowExpanded);
    }

    // ── Role-based section visibility ─────────────────────────────────────

    public void configureForRole(String role) {
        Map<Button, PageRoute> buttonRoutes = new LinkedHashMap<>();
        buttonRoutes.put(dashboardBtn, PageRoute.DASHBOARD);
        buttonRoutes.put(patientsBtn, PageRoute.PATIENTS);
        buttonRoutes.put(appointmentsBtn, PageRoute.APPOINTMENTS);
        buttonRoutes.put(billingBtn, PageRoute.BILLING);
        buttonRoutes.put(doctorsBtn, PageRoute.DOCTORS);
        buttonRoutes.put(appointmentsDoctorBtn, PageRoute.APPOINTMENTS);
        buttonRoutes.put(medicalRecordsBtn, PageRoute.MEDICAL_RECORDS);
        buttonRoutes.put(prescriptionsBtn, PageRoute.PRESCRIPTIONS);
        buttonRoutes.put(labOrdersBtn, PageRoute.LAB_ORDERS);
        buttonRoutes.put(referralsBtn, PageRoute.REFERRALS);
        buttonRoutes.put(scheduleBtn, PageRoute.MY_SCHEDULE);
        buttonRoutes.put(prescriptionsQueueBtn, PageRoute.PRESCRIPTIONS);
        buttonRoutes.put(inventoryBtn, PageRoute.PHARMACY);
        buttonRoutes.put(analyticsBtn, PageRoute.ANALYTICS);
        buttonRoutes.put(usersBtn, PageRoute.USERS);
        buttonRoutes.put(rolesBtn, PageRoute.ROLES);
        buttonRoutes.put(departmentsBtn, PageRoute.DEPARTMENTS);
        buttonRoutes.put(systemLogsBtn, PageRoute.SYSTEM_LOGS);
        buttonRoutes.put(auditLogsBtn, PageRoute.AUDIT_LOGS);
        buttonRoutes.put(retentionBtn, PageRoute.RETENTION);
        // profileBtn/logoutBtn intentionally excluded: PROFILE has no role restriction
        // and logout must always stay reachable regardless of role.

        buttonRoutes.forEach((btn, route) -> {
            if (btn == null) return;
            boolean allowed = PermissionGate.isAllowed(route);
            btn.setVisible(allowed);
            btn.setManaged(allowed);
        });

        show(mainSection, accountSection);
        configureSectionVisibility(patientSection, patientItems);
        configureSectionVisibility(clinicalSection, clinicalItems);
        configureSectionVisibility(pharmacySection, pharmacyItems);
        configureSectionVisibility(analyticsSection, analyticsItems);
        configureSectionVisibility(adminSection, adminItems);
    }

    /** A collapsible section is shown iff at least one of its own nav buttons is visible for this role. */
    private void configureSectionVisibility(VBox section, VBox items) {
        if (section == null) return;
        if (items == null) {
            section.setVisible(false);
            section.setManaged(false);
            return;
        }
        boolean anyVisible = items.getChildren().stream()
                .filter(n -> n instanceof Button)
                .anyMatch(Node::isVisible);
        section.setVisible(anyVisible);
        section.setManaged(anyVisible);
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
            case PRESCRIPTIONS   -> prescriptionsNavButton();
            case LAB_ORDERS      -> labOrdersBtn;
            case REFERRALS       -> referralsBtn;
            case MY_SCHEDULE     -> scheduleBtn;
            case PHARMACY        -> inventoryBtn;
            case ANALYTICS -> analyticsBtn;
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
        java.util.List<Button> sectionHeaders = new java.util.ArrayList<>();
        Button[] headerCandidates = new Button[] {
            patientHeaderBtn, clinicalHeaderBtn, pharmacyHeaderBtn, analyticsHeaderBtn, adminHeaderBtn
        };
        for (Button b : headerCandidates) if (b != null) sectionHeaders.add(b);
        sectionHeaders.forEach(h -> { h.setVisible(!collapsed); h.setManaged(!collapsed); });

        // In icon-only mode: show all items directly (no headers to click)
        // In expanded mode: restore each section's remembered expanded/collapsed state
        java.util.List<VBox> allItems = new java.util.ArrayList<>();
        VBox[] itemCandidates = new VBox[] { patientItems, clinicalItems, pharmacyItems, analyticsItems, adminItems };
        for (VBox v : itemCandidates) if (v != null) allItems.add(v);
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
    @FXML private void handlePrescriptions()  { navigate(PageRoute.PRESCRIPTIONS, prescriptionsNavButton()); }
    @FXML private void handleLabOrders()      { navigate(PageRoute.LAB_ORDERS, labOrdersBtn); }
    @FXML private void handleReferrals()      { navigate(PageRoute.REFERRALS, referralsBtn); }
    @FXML private void handleSchedule()       { navigate(PageRoute.MY_SCHEDULE, scheduleBtn); }
    @FXML private void handlePharmacy()       { navigate(PageRoute.PHARMACY, inventoryBtn); }
    @FXML private void handleAnalytics()      { navigate(PageRoute.ANALYTICS, analyticsBtn); }
    @FXML private void handleUsers()          { navigate(PageRoute.USERS, usersBtn); }
    @FXML private void handleRoles()          { navigate(PageRoute.ROLES, rolesBtn); }
    @FXML private void handleDepartments()    { navigate(PageRoute.DEPARTMENTS, departmentsBtn); }
    @FXML private void handleSystemLogs()     { navigate(PageRoute.SYSTEM_LOGS, systemLogsBtn); }
    @FXML private void handleAuditLogs()      { navigate(PageRoute.AUDIT_LOGS, auditLogsBtn); }
    @FXML private void handleRetention()      { navigate(PageRoute.RETENTION, retentionBtn); }
    @FXML private void handleProfile()        { navigate(PageRoute.PROFILE, profileBtn); }
    @FXML
    private void handleLogout() {
        // Try to obtain the backing session id even if the token is expired.
        String sessionId = SessionManager.peekCurrentSessionId();
        // Clear local session immediately so the UI reflects sign-out.
        SessionManager.logout();

        if (sessionId != null) {
            // Deactivate the server-side session asynchronously so the UI doesn't block.
            AsyncJobRunner.submit(() -> {
                authService.logout(sessionId);
                return Boolean.TRUE;
            }, ok -> {
                // no-op on success; server-side logout publishes events already
            }, err -> {
                System.err.println("Logout cleanup failed: " + err.getMessage());
                showErrorAlert("Server-side logout failed: " + err.getMessage());
            });
        }

        navigate(PageRoute.HOME, logoutBtn);
    }

    /** Swaps the clicked button's icon for a spinner until the target page has loaded. */
    private void navigate(PageRoute route, Button sourceBtn) {
        // HOME is the public landing page — allow navigating there even if session was just cleared.
        if (route != PageRoute.HOME && !PermissionGate.isAllowed(route)) {
            showErrorAlert("You don't have permission to access this page.");
            return;
        }

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
        java.util.List<Button> buttons = new java.util.ArrayList<>();
        Button[] candidates = new Button[] {
            dashboardBtn, patientsBtn, appointmentsBtn, billingBtn,
            doctorsBtn, appointmentsDoctorBtn, medicalRecordsBtn, prescriptionsBtn,
            labOrdersBtn, referralsBtn, scheduleBtn, prescriptionsQueueBtn,
            inventoryBtn, analyticsBtn, usersBtn, rolesBtn, departmentsBtn,
            systemLogsBtn, auditLogsBtn, retentionBtn, profileBtn, logoutBtn
        };
        for (Button b : candidates) if (b != null) buttons.add(b);
        return buttons;
    }

    private Button prescriptionsNavButton() {
        return prescriptionsBtn != null ? prescriptionsBtn : prescriptionsQueueBtn;
    }

    private void show(VBox... sections) {
        for (VBox s : sections) {
            if (s != null) {
                s.setVisible(true);
                s.setManaged(true);
            }
        }
    }
}