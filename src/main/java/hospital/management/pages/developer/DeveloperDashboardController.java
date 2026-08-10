package hospital.management.pages.developer;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.cache.RedisConnection;
import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.daemon.BackupDaemon;
import hospital.management.backend.daemon.BackupPolicy;
import hospital.management.backend.daemon.BackupPolicyStore;
import hospital.management.backend.daemon.BackupType;
import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.mongo.config.MongoConfig;
import hospital.management.backend.service.analytics.DatabaseInspectionService;
import hospital.management.backend.service.analytics.IndexComparisonService;
import hospital.management.backend.service.analytics.PerformanceBenchmarkService;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.service.backup.BackupManifest;
import hospital.management.backend.service.backup.BackupService;
import hospital.management.backend.service.maintenance.MaintenanceMode;
import hospital.management.backend.service.maintenance.MaintenanceModeStore;
import hospital.management.backend.service.maintenance.SystemStatusPage;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pipes.AsyncJobRunner;
import hospital.management.enums.PageRoute;
import hospital.management.pages.BasePageController;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeveloperDashboardController extends BasePageController {

    private static final AppLogger logger = AppLogger.getLogger(DeveloperDashboardController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static int sessionBenchmarkRuns = 0;

    private final PerformanceBenchmarkService benchmarkService   = new PerformanceBenchmarkService();
    private final DatabaseInspectionService   dbInspectionService = new DatabaseInspectionService();
    private final BackupService               backupService       = new BackupService();
    private final IndexComparisonService      indexComparisonService = new IndexComparisonService();
    private final UserService                 userService         = new UserServiceImpl(new UserDAOImpl());

    private static final DateTimeFormatter BACKUP_TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private List<PerformanceBenchmarkService.BenchmarkResult> lastResults;

    // ── Status cards ──────────────────────────────────────────────────────
    @FXML private Label pgStatusValue;
    @FXML private Label pgStatusTrend;
    @FXML private Label mongoStatusValue;
    @FXML private Label mongoStatusTrend;
    @FXML private Label redisStatusValue;
    @FXML private Label redisStatusTrend;
    @FXML private Label benchmarkRunsValue;
    @FXML private Label benchmarkRunsTrend;

    // ── Benchmark controls ────────────────────────────────────────────────
    @FXML private Button runBenchmarkBtn;
    @FXML private Button downloadReportBtn;
    @FXML private Label  benchmarkStatusLabel;

    // ── Benchmark results table ───────────────────────────────────────────
    @FXML private TableView<PerformanceBenchmarkService.BenchmarkResult> resultsTable;
    @FXML private TableColumn<PerformanceBenchmarkService.BenchmarkResult, String> operationCol;
    @FXML private TableColumn<PerformanceBenchmarkService.BenchmarkResult, String> storeCol;
    @FXML private TableColumn<PerformanceBenchmarkService.BenchmarkResult, String> avgBeforeCol;
    @FXML private TableColumn<PerformanceBenchmarkService.BenchmarkResult, String> avgAfterCol;
    @FXML private TableColumn<PerformanceBenchmarkService.BenchmarkResult, String> p95BeforeCol;
    @FXML private TableColumn<PerformanceBenchmarkService.BenchmarkResult, String> p95AfterCol;
    @FXML private TableColumn<PerformanceBenchmarkService.BenchmarkResult, String> throughputBeforeCol;
    @FXML private TableColumn<PerformanceBenchmarkService.BenchmarkResult, String> throughputAfterCol;
    @FXML private TableColumn<PerformanceBenchmarkService.BenchmarkResult, String> improvementCol;

    // ── Charts ────────────────────────────────────────────────────────────
    @FXML private BarChart<String, Number> pgChart;
    @FXML private CategoryAxis pgChartXAxis;
    @FXML private NumberAxis   pgChartYAxis;
    @FXML private BarChart<String, Number> mongoChart;
    @FXML private CategoryAxis mongoChartXAxis;
    @FXML private NumberAxis   mongoChartYAxis;
    @FXML private LineChart<String, Number> performanceLineChart;
    @FXML private CategoryAxis lineChartXAxis;
    @FXML private NumberAxis   lineChartYAxis;

    // ── System info ───────────────────────────────────────────────────────
    @FXML private Label javaVersionLabel;
    @FXML private Label pgVersionLabel;
    @FXML private Label mongoVersionLabel;
    @FXML private Label redisVersionLabel;
    @FXML private Label cacheLayerLabel;
    @FXML private Label algoLabel;

    // ── DB Objects section ────────────────────────────────────────────────
    @FXML private Button refreshDbObjectsBtn;
    @FXML private Button indexBenchmarkBtn;
    @FXML private Label  indexBenchmarkLabel;
    @FXML private BarChart<String, Number> indexComparisonChart;
    @FXML private CategoryAxis indexComparisonXAxis;
    @FXML private NumberAxis   indexComparisonYAxis;

    // Indexes tab
    @FXML private TableView<DatabaseInspectionService.DbIndex> indexesTable;
    @FXML private TableColumn<DatabaseInspectionService.DbIndex, Boolean> indexSelectCol;
    @FXML private TableColumn<DatabaseInspectionService.DbIndex, String> indexNameCol;
    @FXML private TableColumn<DatabaseInspectionService.DbIndex, String> indexTableCol;
    @FXML private TableColumn<DatabaseInspectionService.DbIndex, String> indexTypeCol;
    @FXML private TableColumn<DatabaseInspectionService.DbIndex, String> indexDefCol;
    @FXML private Button regenerateIndexesBtn;
    @FXML private Button dropIndexBtn;
    @FXML private Label  indexActionLabel;
    private final RowSelection<DatabaseInspectionService.DbIndex> indexSelection = new RowSelection<>();

    // Views tab
    @FXML private TableView<DatabaseInspectionService.DbView> viewsTable;
    @FXML private TableColumn<DatabaseInspectionService.DbView, Boolean> viewSelectCol;
    @FXML private TableColumn<DatabaseInspectionService.DbView, String> viewNameCol;
    @FXML private TableColumn<DatabaseInspectionService.DbView, String> viewDefCol;
    @FXML private Button regenerateViewsBtn;
    @FXML private Button dropViewBtn;
    @FXML private Label  viewActionLabel;
    private final RowSelection<DatabaseInspectionService.DbView> viewSelection = new RowSelection<>();

    // Routines tab
    @FXML private TableView<DatabaseInspectionService.DbRoutine> routinesTable;
    @FXML private TableColumn<DatabaseInspectionService.DbRoutine, Boolean> routineSelectCol;
    @FXML private TableColumn<DatabaseInspectionService.DbRoutine, String> routineNameCol;
    @FXML private TableColumn<DatabaseInspectionService.DbRoutine, String> routineTypeCol;
    @FXML private TableColumn<DatabaseInspectionService.DbRoutine, String> routineLangCol;
    @FXML private TableColumn<DatabaseInspectionService.DbRoutine, String> routineDefCol;
    @FXML private Button regenerateRoutinesBtn;
    @FXML private Button dropRoutineBtn;
    @FXML private Label  routineActionLabel;
    private final RowSelection<DatabaseInspectionService.DbRoutine> routineSelection = new RowSelection<>();

    // Backups tab
    @FXML private ComboBox<BackupType> manualBackupTypeCombo;
    @FXML private Button backupNowBtn;
    @FXML private Label  backupStatusLabel;
    @FXML private ComboBox<BackupType> scheduledBackupTypeCombo;
    @FXML private Spinner<Integer> backupIntervalHoursSpinner;
    @FXML private Spinner<Integer> backupRetentionDaysSpinner;
    @FXML private CheckBox scheduledBackupsEnabledCheck;
    @FXML private Button saveBackupSettingsBtn;
    @FXML private Button refreshBackupHistoryBtn;
    @FXML private TableView<BackupManifest> backupHistoryTable;
    @FXML private TableColumn<BackupManifest, String> backupTimeCol;
    @FXML private TableColumn<BackupManifest, String> backupTypeCol;
    @FXML private TableColumn<BackupManifest, String> backupTablesCol;
    @FXML private TableColumn<BackupManifest, String> backupCollsCol;
    @FXML private TableColumn<BackupManifest, String> backupStatusCol;
    @FXML private TableColumn<BackupManifest, String> backupPathCol;
    @FXML private Label backupCountdownLabel;
    private javafx.animation.Timeline backupCountdownTimeline;

    // Maintenance tab
    @FXML private CheckBox maintenanceEnabledCheck;
    @FXML private ComboBox<SystemStatusPage> maintenanceStatusPageCombo;
    @FXML private TextField maintenanceMessageField;
    @FXML private Button saveMaintenanceBtn;
    @FXML private Label blockedCountLabel;
    @FXML private Button revokeSelectedBtn;
    @FXML private Button grantSelectedBtn;
    @FXML private TableView<UserDTO> maintenanceUsersTable;
    @FXML private TableColumn<UserDTO, Boolean> maintenanceUserSelectCol;
    @FXML private TableColumn<UserDTO, String> maintenanceUsernameCol;
    @FXML private TableColumn<UserDTO, String> maintenanceEmailCol;
    @FXML private TableColumn<UserDTO, String> maintenanceStatusCol;
    private final RowSelection<UserDTO> maintenanceUserSelection = new RowSelection<>();
    private List<UserDTO> maintenanceUsers = List.of();

    /**
     * Tracks per-row checkbox selection for a {@code TableView<T>} without
     * touching the table's own (single-item, detail-view) selection model.
     * Binds a header "select all" checkbox that tri-states as rows are
     * toggled — same header-drives-children idea as
     * {@code RolesPageController.syncSelectAllState()}, built on JavaFX's
     * {@link CheckBoxTableCell} instead of a hand-rolled checkbox grid.
     */
    private static final class RowSelection<T> {
        private final Map<T, BooleanProperty> byItem = new LinkedHashMap<>();
        private final CheckBox selectAll = new CheckBox();
        private boolean syncing = false;

        void bind(TableColumn<T, Boolean> col) {
            col.setCellValueFactory(cd -> byItem.computeIfAbsent(cd.getValue(), k -> newProp()));
            col.setCellFactory((Callback<TableColumn<T, Boolean>, TableCell<T, Boolean>>)
                CheckBoxTableCell.forTableColumn(col));
            col.setSortable(false);
            col.setGraphic(selectAll);
            selectAll.setOnAction(e -> {
                syncing = true;
                byItem.values().forEach(p -> p.setValue(selectAll.isSelected()));
                syncing = false;
            });
        }

        private BooleanProperty newProp() {
            BooleanProperty p = new SimpleBooleanProperty(false);
            p.addListener((obs, was, is) -> { if (!syncing) syncSelectAll(); });
            return p;
        }

        /** Call after every table refresh so stale selections don't survive a reload. */
        void reset(List<T> items) {
            byItem.clear();
            items.forEach(i -> byItem.put(i, newProp()));
            syncSelectAll();
        }

        List<T> selected() {
            return byItem.entrySet().stream()
                .filter(e -> Boolean.TRUE.equals(e.getValue().get()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }

        private void syncSelectAll() {
            long checked = byItem.values().stream().filter(p -> Boolean.TRUE.equals(p.get())).count();
            selectAll.setIndeterminate(checked > 0 && checked < byItem.size());
            selectAll.setSelected(checked > 0 && checked == byItem.size());
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.DEVELOPER_DASHBOARD);

        // Belt-and-suspenders: this page is already Admin-only via PermissionGate's
        // ROUTE_RESOURCES mapping (sidebar/navbar block non-admins from ever reaching
        // it), but disable the destructive actions here too in case it's ever reached
        // through a path that bypasses that guard.
        boolean canManage = canDelete(PageRoute.DEVELOPER_DASHBOARD);
        dropIndexBtn.setDisable(!canManage);
        dropViewBtn.setDisable(!canManage);
        dropRoutineBtn.setDisable(!canManage);
        backupNowBtn.setDisable(!canManage);
        saveBackupSettingsBtn.setDisable(!canManage);
        saveMaintenanceBtn.setDisable(!canManage);
        revokeSelectedBtn.setDisable(!canManage);
        grantSelectedBtn.setDisable(!canManage);

        downloadReportBtn.setDisable(true);
        benchmarkStatusLabel.setText("No benchmark run yet — click Run Benchmark to start.");

        setupBenchmarkTable();
        loadSystemInfo();
        refreshStatusCards();
        setupDbObjectsTables();
        loadDbObjects();
        setupBackupTab();
        setupMaintenanceTab();

        runBenchmarkBtn.setOnAction(e -> withSpinner(runBenchmarkBtn, this::runBenchmark));
        downloadReportBtn.setOnAction(e -> downloadReport());

        refreshDbObjectsBtn.setOnAction(e -> withSpinner(refreshDbObjectsBtn, this::loadDbObjects));
        indexBenchmarkBtn.setOnAction(e -> confirm(
            "Run Index Impact Benchmark",
            "This measures query performance twice — once BEFORE the index exists (temporarily dropped) "
                + "and once AFTER it's recreated — for both PostgreSQL and MongoDB. Indexes are always "
                + "restored afterward, whichever result you're most interested in.\n\nContinue?",
            () -> withSpinner(indexBenchmarkBtn, this::runIndexBenchmark)));

        regenerateIndexesBtn.setOnAction(e -> withSpinner(regenerateIndexesBtn, this::regenerateAllIndexes));
        dropIndexBtn.setOnAction(e -> dropSelectedIndexes());

        regenerateViewsBtn.setOnAction(e -> withSpinner(regenerateViewsBtn, this::regenerateAllViews));
        dropViewBtn.setOnAction(e -> dropSelectedViews());

        regenerateRoutinesBtn.setOnAction(e -> withSpinner(regenerateRoutinesBtn, this::regenerateAllRoutines));
        dropRoutineBtn.setOnAction(e -> dropSelectedRoutines());

        backupNowBtn.setOnAction(e -> backupNow());
        saveBackupSettingsBtn.setOnAction(e -> saveBackupSettings());
        refreshBackupHistoryBtn.setOnAction(e -> withSpinner(refreshBackupHistoryBtn, this::refreshBackupHistory));
    }

    // ── Benchmark table ───────────────────────────────────────────────────

    private void setupBenchmarkTable() {
        operationCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().operation));
        storeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().store));
        avgBeforeCol.setCellValueFactory(c -> new SimpleStringProperty(fmtMs(c.getValue().beforeAvgMs)));
        avgAfterCol.setCellValueFactory(c -> new SimpleStringProperty(fmtMs(c.getValue().afterAvgMs)));
        p95BeforeCol.setCellValueFactory(c -> new SimpleStringProperty(fmtMs(c.getValue().beforeP95Ms)));
        p95AfterCol.setCellValueFactory(c -> new SimpleStringProperty(fmtMs(c.getValue().afterP95Ms)));
        throughputBeforeCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().beforeThroughput <= 0 ? "N/A" : String.format("%,.0f", c.getValue().beforeThroughput)));
        throughputAfterCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().afterThroughput <= 0 ? "N/A" : String.format("%,.0f", c.getValue().afterThroughput)));
        improvementCol.setCellValueFactory(c -> new SimpleStringProperty(
            String.format("%.1f%%", c.getValue().improvementPct)));

        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        resultsTable.setPlaceholder(new Label("Run a benchmark to populate this table."));
    }

    private static String fmtMs(double ms) {
        return ms < 0 ? "N/A" : String.format("%.2f", ms);
    }

    // ── DB Objects table setup ────────────────────────────────────────────

    private void setupDbObjectsTables() {
        // Indexes
        indexNameCol.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().indexName()));
        indexTableCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().tableName()));
        indexTypeCol.setCellValueFactory(c  -> new SimpleStringProperty(
            c.getValue().isPrimary() ? "PK" : c.getValue().isUnique() ? "Unique" : "Index"));
        indexDefCol.setCellValueFactory(c   -> new SimpleStringProperty(
            truncate(c.getValue().indexDef(), 80)));
        indexesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        indexesTable.setPlaceholder(new Label("Click Refresh to load indexes."));
        indexesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        indexSelection.bind(indexSelectCol);

        // Views
        viewNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().viewName()));
        viewDefCol.setCellValueFactory(c  -> new SimpleStringProperty(
            truncate(c.getValue().definition(), 100)));
        viewsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        viewsTable.setPlaceholder(new Label("Click Refresh to load views."));
        viewsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        viewSelection.bind(viewSelectCol);

        // Routines
        routineNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().routineName()));
        routineTypeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().routineType()));
        routineLangCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().language()));
        routineDefCol.setCellValueFactory(c  -> new SimpleStringProperty(
            truncate(c.getValue().definition(), 80)));
        routinesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        routinesTable.setPlaceholder(new Label("Click Refresh to load routines."));
        routinesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        routineSelection.bind(routineSelectCol);

        // Double-click to show full definition in a detail alert
        indexesTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) showIndexDetail();
        });
        viewsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) showViewDetail();
        });
        routinesTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) showRoutineDetail();
        });
    }

    // ── Load DB objects ───────────────────────────────────────────────────

    private void loadDbObjects() {
        indexesTable.setPlaceholder(new Label("Loading indexes…"));
        viewsTable.setPlaceholder(new Label("Loading views…"));
        routinesTable.setPlaceholder(new Label("Loading routines…"));
        setActionLabel(indexActionLabel,   "Loading…");
        setActionLabel(viewActionLabel,    "Loading…");
        setActionLabel(routineActionLabel, "Loading…");

        AsyncJobRunner.submit(
            () -> {
                // Constraint-backed indexes (PRIMARY KEY *and* UNIQUE) are structural —
                // Postgres refuses DROP INDEX on them even with CASCADE ("cannot drop
                // index because constraint X requires it"), so hide them entirely rather
                // than show-then-refuse-to-drop. isPrimary() alone isn't enough: it only
                // catches pk_*/*_pkey-named indexes and misses UNIQUE column constraints
                // (e.g. users_email_key) — isConstraintBacked() is the real, pg_constraint
                // -verified check and supersedes it.
                var indexes = dbInspectionService.listIndexes().stream()
                    .filter(i -> !i.isConstraintBacked())
                    .collect(Collectors.toList());
                var views    = dbInspectionService.listViews();
                var routines = dbInspectionService.listRoutines();
                return new Object[]{ indexes, views, routines };
            },
            result -> {
                @SuppressWarnings("unchecked")
                var indexes  = (List<DatabaseInspectionService.DbIndex>)   result[0];
                @SuppressWarnings("unchecked")
                var views    = (List<DatabaseInspectionService.DbView>)    result[1];
                @SuppressWarnings("unchecked")
                var routines = (List<DatabaseInspectionService.DbRoutine>) result[2];

                indexesTable.setItems(FXCollections.observableArrayList(indexes));
                viewsTable.setItems(FXCollections.observableArrayList(views));
                routinesTable.setItems(FXCollections.observableArrayList(routines));
                indexSelection.reset(indexes);
                viewSelection.reset(views);
                routineSelection.reset(routines);

                indexesTable.setPlaceholder(new Label("No non-primary-key indexes found."));
                viewsTable.setPlaceholder(new Label("No views found."));
                routinesTable.setPlaceholder(new Label("No routines found."));

                setActionLabel(indexActionLabel,   indexes.size()  + " indexes loaded (primary keys hidden)");
                setActionLabel(viewActionLabel,    views.size()    + " views loaded");
                setActionLabel(routineActionLabel, routines.size() + " routines loaded");
            },
            ex -> {
                String msg = "Load failed — click Refresh to retry.";
                indexesTable.setPlaceholder(new Label(msg));
                viewsTable.setPlaceholder(new Label(msg));
                routinesTable.setPlaceholder(new Label(msg));
                setActionLabel(indexActionLabel,   "Load failed");
                setActionLabel(viewActionLabel,    "Load failed");
                setActionLabel(routineActionLabel, "Load failed");
                toastError("Failed to load DB objects: " + ex.getMessage());
            }
        );
    }

    // ── Drop actions ──────────────────────────────────────────────────────

    private void dropSelectedIndexes() {
        List<DatabaseInspectionService.DbIndex> selected = indexSelection.selected();
        if (selected.isEmpty()) { toastError("Select at least one index to drop."); return; }

        // Defense in depth — loadDbObjects() already excludes these from the table
        // entirely, but guard here too in case stale items ever slip through (e.g. a
        // selection held across a refresh).
        List<DatabaseInspectionService.DbIndex> constraintBacked =
            selected.stream().filter(DatabaseInspectionService.DbIndex::isConstraintBacked).collect(Collectors.toList());
        if (!constraintBacked.isEmpty()) {
            toastError("Deselect constraint-backed index(es) first (Postgres won't allow dropping these): " + constraintBacked.stream()
                .map(DatabaseInspectionService.DbIndex::indexName).collect(Collectors.joining(", ")));
            return;
        }

        List<String> names = selected.stream().map(DatabaseInspectionService.DbIndex::indexName).collect(Collectors.toList());
        confirm("Drop " + selected.size() + " Index(es)",
            "Drop the following indexes?\n" + String.join(", ", names) + "\n" +
            "This degrades query performance until regenerated.",
            () -> withSpinner(dropIndexBtn, () -> {
                try {
                    dbInspectionService.dropIndexes(names);
                    loadDbObjects();
                    toastSuccess(selected.size() + " index(es) dropped.");
                    notifyDbObjectChange(selected.size() + " index(es) dropped: " + String.join(", ", names));
                } catch (Exception ex) {
                    toastError("Failed to drop indexes: " + ex.getMessage());
                }
            }));
    }

    private void dropSelectedViews() {
        List<DatabaseInspectionService.DbView> selected = viewSelection.selected();
        if (selected.isEmpty()) { toastError("Select at least one view to drop."); return; }

        List<String> names = selected.stream().map(DatabaseInspectionService.DbView::viewName).collect(Collectors.toList());
        confirm("Drop " + selected.size() + " View(s)",
            "Drop the following views?\n" + String.join(", ", names) + "\n" +
            "Any queries that depend on them will fail until regenerated.",
            () -> withSpinner(dropViewBtn, () -> {
                try {
                    dbInspectionService.dropViews(names);
                    loadDbObjects();
                    toastSuccess(selected.size() + " view(s) dropped.");
                    notifyDbObjectChange(selected.size() + " view(s) dropped: " + String.join(", ", names));
                } catch (Exception ex) {
                    toastError("Failed to drop views: " + ex.getMessage());
                }
            }));
    }

    private void dropSelectedRoutines() {
        List<DatabaseInspectionService.DbRoutine> selected = routineSelection.selected();
        if (selected.isEmpty()) { toastError("Select at least one function or procedure to drop."); return; }

        List<String> names = selected.stream().map(DatabaseInspectionService.DbRoutine::routineName).collect(Collectors.toList());
        confirm("Drop " + selected.size() + " Routine(s)",
            "Drop the following functions/procedures?\n" + String.join(", ", names) + "\n" +
            "Features that call them will break until regenerated.",
            () -> withSpinner(dropRoutineBtn, () -> {
                try {
                    dbInspectionService.dropRoutines(selected);
                    loadDbObjects();
                    toastSuccess(selected.size() + " routine(s) dropped.");
                    notifyDbObjectChange(selected.size() + " routine(s) dropped: " + String.join(", ", names));
                } catch (Exception ex) {
                    toastError("Failed to drop routines: " + ex.getMessage());
                }
            }));
    }

    // ── Regenerate actions ────────────────────────────────────────────────

    private void regenerateAllIndexes() {
        try {
            dbInspectionService.regenerateIndexes();
            loadDbObjects();
            toastSuccess("All indexes regenerated.");
            notifyDbObjectChange("All indexes regenerated.");
        } catch (Exception ex) {
            toastError("Failed to regenerate indexes: " + ex.getMessage());
        }
    }

    private void regenerateAllViews() {
        try {
            dbInspectionService.regenerateViews();
            loadDbObjects();
            toastSuccess("All views regenerated.");
            notifyDbObjectChange("All views regenerated.");
        } catch (Exception ex) {
            toastError("Failed to regenerate views: " + ex.getMessage());
        }
    }

    private void regenerateAllRoutines() {
        try {
            dbInspectionService.regenerateRoutines();
            loadDbObjects();
            toastSuccess("All routines regenerated.");
            notifyDbObjectChange("All routines regenerated.");
        } catch (Exception ex) {
            toastError("Failed to regenerate routines: " + ex.getMessage());
        }
    }

    /** Fires a self-notifying admin-audit notification for any index/view/routine change. */
    private void notifyDbObjectChange(String description) {
        EventBus.publish(AppEventType.DB_OBJECT_CHANGED, description);
    }

    // ── Backups tab ────────────────────────────────────────────────────────

    private void setupBackupTab() {
        // Started first and unconditionally: it only depends on backupCountdownLabel
        // (already FXML-injected by this point) and BackupDaemon's static state, so a
        // failure anywhere later in this method can never prevent it from initializing.
        startBackupCountdown();

        manualBackupTypeCombo.setItems(FXCollections.observableArrayList(BackupType.values()));
        manualBackupTypeCombo.getSelectionModel().select(BackupPolicy.DEFAULT_BACKUP_TYPE);
        scheduledBackupTypeCombo.setItems(FXCollections.observableArrayList(BackupType.values()));

        backupIntervalHoursSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 168, BackupPolicy.DEFAULT_BACKUP_INTERVAL_HOURS));
        backupRetentionDaysSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3650, BackupPolicy.DEFAULT_BACKUP_RETENTION_DAYS));
        makeSpinnerEditable(backupIntervalHoursSpinner);
        makeSpinnerEditable(backupRetentionDaysSpinner);

        backupTimeCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().startedAt == null ? "—" : c.getValue().startedAt.format(BACKUP_TS_FMT)));
        backupTypeCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().type)));
        backupTablesCol.setCellValueFactory(c -> new SimpleStringProperty(
            String.valueOf(c.getValue().postgresTables.size())));
        backupCollsCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().mongoSkipped ? "skipped" : String.valueOf(c.getValue().mongoCollections.size())));
        backupStatusCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().status + (c.getValue().errorMessage != null ? " — " + c.getValue().errorMessage : "")));
        backupPathCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().directoryPath == null ? "—" : c.getValue().directoryPath));
        backupHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        backupHistoryTable.setPlaceholder(new Label("No backups yet — click Backup Now to create one."));

        loadBackupSettingsForm();
        refreshBackupHistory();
    }

    /** Ticks every second, showing DD:HH:MM:SS until BackupDaemon's next scheduled run. */
    private void startBackupCountdown() {
        if (backupCountdownTimeline != null) backupCountdownTimeline.stop();
        backupCountdownTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> refreshBackupCountdownLabel()));
        backupCountdownTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        backupCountdownTimeline.play();
        refreshBackupCountdownLabel();
    }

    private void refreshBackupCountdownLabel() {
        LocalDateTime nextRunAt = BackupDaemon.getNextRunAt();
        if (nextRunAt == null) {
            backupCountdownLabel.setText("No countdown — check \"Enable scheduled backups\" above and Save to start one.");
            return;
        }
        java.time.Duration remaining = java.time.Duration.between(LocalDateTime.now(), nextRunAt);
        if (remaining.isNegative()) {
            backupCountdownLabel.setText("Running now…");
            return;
        }
        long totalSeconds = remaining.getSeconds();
        long days    = totalSeconds / 86400;
        long hours   = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        backupCountdownLabel.setText(String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds));
    }

    private void makeSpinnerEditable(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) spinner.increment(0); // commits a typed value
        });
    }

    private void loadBackupSettingsForm() {
        BackupPolicy policy = BackupPolicyStore.load();
        scheduledBackupTypeCombo.getSelectionModel().select(policy.getBackupType());
        backupIntervalHoursSpinner.getValueFactory().setValue(policy.getBackupIntervalHours());
        backupRetentionDaysSpinner.getValueFactory().setValue(policy.getBackupRetentionDays());
        scheduledBackupsEnabledCheck.setSelected(policy.isScheduledBackupsEnabled());
    }

    private void backupNow() {
        BackupType type = manualBackupTypeCombo.getValue();
        if (type == null) { toastError("Select a backup type."); return; }

        backupNowBtn.setDisable(true);
        backupStatusLabel.setText("Backup starting…");
        AsyncJobRunner.submit(
            () -> backupService.runBackup(type, msg -> Platform.runLater(() -> backupStatusLabel.setText(msg))),
            manifest -> {
                backupNowBtn.setDisable(false);
                // The path goes ONLY in this persistent label (and the history table's "Saved
                // To" column) — never in the toast. Toasts here auto-dismiss after ~3.4s and
                // cap at 300px wide, so a full absolute path wrapped into one barely has time
                // to be read before it fades; this label stays until the next action.
                String summary = "Backup " + manifest.status + " — " + manifest.postgresTables.size() + " table(s)"
                    + (manifest.mongoSkipped ? ", MongoDB skipped" : ", " + manifest.mongoCollections.size() + " collection(s)")
                    + "\nSaved to: " + manifest.directoryPath;
                backupStatusLabel.setText(summary);
                if ("SUCCESS".equals(manifest.status)) {
                    toastSuccess("Backup complete — see path below.");
                    EventBus.publish(AppEventType.BACKUP_COMPLETED, manifest);
                } else {
                    toastError("Backup finished with issues — see details below.");
                    EventBus.publish(AppEventType.BACKUP_FAILED, summary);
                }
                refreshBackupHistory();
            },
            ex -> {
                backupNowBtn.setDisable(false);
                backupStatusLabel.setText("Backup failed: " + ex.getMessage());
                toastError("Backup failed: " + ex.getMessage());
                EventBus.publish(AppEventType.BACKUP_FAILED, "Backup failed: " + ex.getMessage());
                refreshBackupHistory();
            }
        );
    }

    private void saveBackupSettings() {
        try {
            BackupPolicy policy = new BackupPolicy();
            policy.setBackupType(scheduledBackupTypeCombo.getValue());
            policy.setBackupIntervalHours(backupIntervalHoursSpinner.getValue());
            policy.setBackupRetentionDays(backupRetentionDaysSpinner.getValue());
            policy.setScheduledBackupsEnabled(scheduledBackupsEnabledCheck.isSelected());
            BackupPolicyStore.save(policy);
            BackupDaemon.restart();
            refreshBackupCountdownLabel();
            toastSuccess("Backup settings saved.");
        } catch (IllegalArgumentException ex) {
            toastError(ex.getMessage());
        }
    }

    private void refreshBackupHistory() {
        backupHistoryTable.setPlaceholder(new Label("Loading…"));
        AsyncJobRunner.submit(
            backupService::listBackups,
            history -> backupHistoryTable.setItems(FXCollections.observableArrayList(history)),
            ex -> {
                backupHistoryTable.setPlaceholder(new Label("Failed to load backup history."));
                toastError("Failed to load backup history: " + ex.getMessage());
            }
        );
    }

    // ── Maintenance tab ───────────────────────────────────────────────────

    private void setupMaintenanceTab() {
        maintenanceStatusPageCombo.setItems(FXCollections.observableArrayList(SystemStatusPage.values()));
        maintenanceUserSelection.bind(maintenanceUserSelectCol);

        maintenanceUsernameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        maintenanceEmailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        maintenanceStatusCol.setCellValueFactory(c -> new SimpleStringProperty(
            isCurrentlyBlocked(c.getValue().getUserId()) ? "Revoked" : "Normal"));
        maintenanceUsersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        maintenanceUsersTable.setPlaceholder(new Label("Loading users…"));

        saveMaintenanceBtn.setOnAction(e -> saveMaintenanceSettings());
        revokeSelectedBtn.setOnAction(e -> applyAccessChange(true));
        grantSelectedBtn.setOnAction(e -> applyAccessChange(false));

        loadMaintenanceForm();
        loadMaintenanceUsers();
    }

    private void loadMaintenanceForm() {
        MaintenanceMode mode = MaintenanceModeStore.load();
        maintenanceEnabledCheck.setSelected(mode.isEnabled());
        maintenanceStatusPageCombo.getSelectionModel().select(mode.getStatusPage());
        maintenanceMessageField.setText(mode.getMessage());
        blockedCountLabel.setText("Currently blocked: " + mode.getBlockedUserIds().size() + " user(s)");
    }

    private void loadMaintenanceUsers() {
        AsyncJobRunner.submit(
            () -> userService.findAll(CursorPagination.firstPage(500)).getItems(),
            users -> {
                maintenanceUsers = users;
                maintenanceUsersTable.setItems(FXCollections.observableArrayList(users));
                maintenanceUserSelection.reset(users);
                maintenanceUsersTable.setPlaceholder(new Label("No users found."));
                maintenanceUsersTable.refresh();
            },
            ex -> {
                maintenanceUsersTable.setPlaceholder(new Label("Failed to load users."));
                toastError("Failed to load users: " + ex.getMessage());
            }
        );
    }

    private boolean isCurrentlyBlocked(String userId) {
        return MaintenanceModeStore.load().isUserBlocked(userId);
    }

    private void saveMaintenanceSettings() {
        MaintenanceMode mode = MaintenanceModeStore.load(); // preserve blockedUserIds set via Revoke/Grant
        mode.setEnabled(maintenanceEnabledCheck.isSelected());
        mode.setStatusPage(maintenanceStatusPageCombo.getValue());
        mode.setMessage(maintenanceMessageField.getText());
        MaintenanceModeStore.save(mode);
        toastSuccess("Maintenance settings saved.");
        EventBus.publish(AppEventType.MAINTENANCE_ACCESS_CHANGED, "Maintenance settings updated — enabled="
            + mode.isEnabled() + ", page=" + mode.getStatusPage());
        loadMaintenanceForm();
    }

    /** {@code revoke=true} adds every selected user to the blocked list; {@code false} removes them. */
    private void applyAccessChange(boolean revoke) {
        List<UserDTO> selected = maintenanceUserSelection.selected();
        if (selected.isEmpty()) {
            toastError("Select at least one user first.");
            return;
        }
        MaintenanceMode mode = MaintenanceModeStore.load();
        for (UserDTO user : selected) {
            if (revoke) mode.getBlockedUserIds().add(user.getUserId());
            else mode.getBlockedUserIds().remove(user.getUserId());
        }
        MaintenanceModeStore.save(mode);
        toastSuccess((revoke ? "Revoked " : "Granted ") + selected.size() + " user(s).");
        blockedCountLabel.setText("Currently blocked: " + mode.getBlockedUserIds().size() + " user(s)");
        maintenanceUsersTable.refresh();

        String usernames = selected.stream().map(UserDTO::getUsername).collect(Collectors.joining(", "));
        EventBus.publish(AppEventType.MAINTENANCE_ACCESS_CHANGED,
            (revoke ? "Revoked access for: " : "Granted access for: ") + usernames);
    }

    // ── Index impact benchmark ────────────────────────────────────────────

    private void runIndexBenchmark() {
        indexBenchmarkLabel.setText("Running index benchmark (PostgreSQL GIN + MongoDB single-field index)…");
        AsyncJobRunner.submit(
            () -> indexComparisonService.benchmarkAcrossStores(),
            result -> {
                IndexComparisonService.StoreIndexComparison pg    = result.postgres();
                IndexComparisonService.StoreIndexComparison mongo = result.mongo();
                String text = String.format(
                    "PostgreSQL: %.2f ms -> %.2f ms (%.1fx)  |  MongoDB: %.2f ms -> %.2f ms (%.1fx)",
                    pg.beforeMs(), pg.afterMs(), pg.speedupFactor(),
                    mongo.beforeMs(), mongo.afterMs(), mongo.speedupFactor());
                indexBenchmarkLabel.setText(text);
                refreshIndexComparisonChart(result);
                toastSuccess("Index benchmark complete.");
                loadDbObjects();
            },
            ex -> {
                indexBenchmarkLabel.setText("Benchmark failed: " + ex.getMessage());
                toastError("Index benchmark failed: " + ex.getMessage());
                loadDbObjects();
            }
        );
    }

    private void refreshIndexComparisonChart(IndexComparisonService.CrossStoreIndexComparison result) {
        XYChart.Series<String, Number> pgSeries    = new XYChart.Series<>();
        XYChart.Series<String, Number> mongoSeries = new XYChart.Series<>();
        pgSeries.setName(result.postgres().store());
        mongoSeries.setName(result.mongo().store());

        pgSeries.getData().add(new XYChart.Data<>("Before Index", Math.max(0, result.postgres().beforeMs())));
        pgSeries.getData().add(new XYChart.Data<>("After Index",  Math.max(0, result.postgres().afterMs())));
        mongoSeries.getData().add(new XYChart.Data<>("Before Index", Math.max(0, result.mongo().beforeMs())));
        mongoSeries.getData().add(new XYChart.Data<>("After Index",  Math.max(0, result.mongo().afterMs())));

        indexComparisonChart.getData().setAll(pgSeries, mongoSeries);
    }

    // ── Detail popups ─────────────────────────────────────────────────────

    private void showIndexDetail() {
        DatabaseInspectionService.DbIndex sel = indexesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        showDetailAlert("Index: " + sel.indexName(),
            "Table:   " + sel.tableName() + "\n" +
            "Type:    " + (sel.isPrimary() ? "Primary Key" : sel.isUnique() ? "Unique" : "Regular") + "\n\n" +
            "Definition:\n" + sel.indexDef());
    }

    private void showViewDetail() {
        DatabaseInspectionService.DbView sel = viewsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        showDetailAlert("View: " + sel.viewName(), sel.definition());
    }

    private void showRoutineDetail() {
        DatabaseInspectionService.DbRoutine sel = routinesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        showDetailAlert(sel.routineType() + ": " + sel.routineName(),
            "Language: " + sel.language() + "\n\nDefinition:\n" + sel.definition());
    }

    private void showDetailAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        TextArea ta = new TextArea(content);
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setPrefSize(700, 400);
        alert.getDialogPane().setContent(ta);
        alert.getDialogPane().setPrefWidth(740);
        alert.showAndWait();
    }

    // ── Benchmark run ─────────────────────────────────────────────────────

    private void runBenchmark() {
        benchmarkStatusLabel.setText("Running… this may take 30–60 seconds.");
        downloadReportBtn.setDisable(true);
        resultsTable.getItems().clear();
        pgChart.getData().clear();
        mongoChart.getData().clear();
        performanceLineChart.getData().clear();

        AsyncJobRunner.submit(
            () -> benchmarkService.runBenchmarks(),
            results -> {
                lastResults = results;
                sessionBenchmarkRuns++;

                resultsTable.setItems(FXCollections.observableArrayList(results));
                refreshCharts(results);

                benchmarkStatusLabel.setText("Completed at " + LocalDateTime.now().format(TIME_FMT));
                benchmarkRunsValue.setText(String.valueOf(sessionBenchmarkRuns));
                downloadReportBtn.setDisable(false);
            },
            ex -> {
                benchmarkStatusLabel.setText("Failed: " + ex.getMessage());
                toastError("Benchmark failed: " + ex.getMessage());
            }
        );
    }

    // ── Charts ────────────────────────────────────────────────────────────

    private void refreshCharts(List<PerformanceBenchmarkService.BenchmarkResult> results) {
        XYChart.Series<String, Number> pgSeries    = new XYChart.Series<>();
        XYChart.Series<String, Number> mongoSeries = new XYChart.Series<>();
        pgSeries.setName("Avg (ms)");
        mongoSeries.setName("Avg (ms)");

        for (PerformanceBenchmarkService.BenchmarkResult r : results) {
            double ms = Math.max(0, r.avgMs);
            String label = shortLabel(r.operation);
            if (r.operation.startsWith("[PG]")) {
                pgSeries.getData().add(new XYChart.Data<>(label, ms));
            } else if (r.operation.startsWith("[Mongo")) {
                mongoSeries.getData().add(new XYChart.Data<>(label, ms));
            }
        }

        pgChart.getData().setAll(pgSeries);
        mongoChart.getData().setAll(mongoSeries);
        refreshLineChart(results);
    }

    private void refreshLineChart(List<PerformanceBenchmarkService.BenchmarkResult> results) {
        XYChart.Series<String, Number> beforeSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> afterSeries  = new XYChart.Series<>();
        beforeSeries.setName("Before (Cold)");
        afterSeries.setName("After (Warm)");

        for (PerformanceBenchmarkService.BenchmarkResult r : results) {
            String label = shortLabel(r.operation);
            if (r.beforeAvgMs > 0) beforeSeries.getData().add(new XYChart.Data<>(label, r.beforeAvgMs));
            if (r.afterAvgMs  > 0) afterSeries.getData().add(new XYChart.Data<>(label, r.afterAvgMs));
        }

        performanceLineChart.getData().setAll(beforeSeries, afterSeries);
    }

    private String shortLabel(String operation) {
        String s = operation
            .replaceAll("\\[PG\\]\\s*", "")
            .replaceAll("\\[Mongo-Notif\\]\\s*", "N:")
            .replaceAll("\\[Mongo\\]\\s*", "")
            .replaceAll("\\(.*?\\)", "")
            .trim();
        return s.length() > 22 ? s.substring(0, 22) + "…" : s;
    }

    // ── Download report ───────────────────────────────────────────────────

    private void downloadReport() {
        if (lastResults == null || lastResults.isEmpty()) return;

        ChoiceDialog<String> formatDialog = new ChoiceDialog<>("Markdown (.md)", "Markdown (.md)", "CSV (.csv)", "PDF (.pdf)");
        formatDialog.setTitle("Export Report");
        formatDialog.setHeaderText("Choose report format");
        formatDialog.setContentText("Format:");
        java.util.Optional<String> choice = formatDialog.showAndWait();
        if (choice.isEmpty()) return;

        PerformanceBenchmarkService.ReportFormat format = switch (choice.get()) {
            case "CSV (.csv)"  -> PerformanceBenchmarkService.ReportFormat.CSV;
            case "PDF (.pdf)"  -> PerformanceBenchmarkService.ReportFormat.PDF;
            default            -> PerformanceBenchmarkService.ReportFormat.MARKDOWN;
        };

        String ext  = switch (format) { case CSV -> ".csv"; case PDF -> ".pdf"; default -> ".md"; };
        String desc = switch (format) { case CSV -> "CSV (*.csv)"; case PDF -> "PDF (*.pdf)"; default -> "Markdown (*.md)"; };
        String glob = switch (format) { case CSV -> "*.csv"; case PDF -> "*.pdf"; default -> "*.md"; };

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Benchmark Report");
        chooser.setInitialFileName("performance_benchmark_report" + ext);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, glob));
        File dest = chooser.showSaveDialog(downloadReportBtn.getScene().getWindow());
        if (dest == null) return;

        final PerformanceBenchmarkService.ReportFormat finalFormat = format;
        AsyncJobRunner.submit(
            () -> {
                Path tmp = benchmarkService.generateReportFromResults(lastResults, finalFormat);
                Files.copy(tmp, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return dest;
            },
            savedFile -> {
                toastSuccess("Report saved: " + savedFile.getName());
                if (Desktop.isDesktopSupported()
                        && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    AsyncJobRunner.submit(
                        () -> { Desktop.getDesktop().open(savedFile); return null; },
                        v -> {}, err -> logger.warn("Could not open file: " + err.getMessage())
                    );
                }
            },
            ex -> toastError("Failed to save report: " + ex.getMessage())
        );
    }

    // ── Status cards ──────────────────────────────────────────────────────

    private void refreshStatusCards() {
        try (Connection c = DBConnection.getConnection()) {
            pgStatusValue.setText("Connected");
            pgStatusValue.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
            pgStatusTrend.setText("JDBC 42.7.4 · HikariCP pool");
        } catch (Exception e) {
            pgStatusValue.setText("Error");
            pgStatusValue.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
            pgStatusTrend.setText(truncate(e.getMessage(), 40));
        }

        try {
            boolean ok = MongoConfig.getDatabase() != null;
            mongoStatusValue.setText(ok ? "Connected" : "Unavailable");
            mongoStatusValue.setStyle(ok
                ? "-fx-text-fill: #27AE60; -fx-font-weight: bold;"
                : "-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
            mongoStatusTrend.setText(ok
                ? "mongodb-driver-sync 5.1.4"
                : "Check MONGO_URL in .env");
        } catch (Exception e) {
            mongoStatusValue.setText("Error");
            mongoStatusValue.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
            mongoStatusTrend.setText(truncate(e.getMessage(), 40));
        }

        boolean redisOk = RedisConnection.isHealthy();
        redisStatusValue.setText(redisOk ? "Connected" : "Unavailable");
        redisStatusValue.setStyle(redisOk
            ? "-fx-text-fill: #27AE60; -fx-font-weight: bold;"
            : "-fx-text-fill: #F39C12; -fx-font-weight: bold;");
        redisStatusTrend.setText(redisOk
            ? "Jedis 5.2.0 · L2 cache active"
            : "L1 in-process cache still active");

        benchmarkRunsValue.setText(String.valueOf(sessionBenchmarkRuns));
        benchmarkRunsTrend.setText("this session");
    }

    // ── System info ───────────────────────────────────────────────────────

    private void loadSystemInfo() {
        javaVersionLabel.setText(System.getProperty("java.version")
            + " (" + System.getProperty("java.vm.name") + ")");
        pgVersionLabel.setText("PostgreSQL · JDBC 42.7.4 · HikariCP 7.0.2 connection pool");
        mongoVersionLabel.setText(MongoConfig.getDatabase() != null
            ? "mongodb-driver-sync 5.1.4 · DB: hospital_nosql"
            : "Not connected — check MONGO_URL in .env");
        redisVersionLabel.setText("Jedis 5.2.0 · max 10 connections · "
            + (RedisConnection.isHealthy() ? "healthy" : "unavailable"));
        cacheLayerLabel.setText(
            "L1: In-process ConcurrentHashMap (500 entries, LRU, 5-min idle TTL)  +  "
            + "L2: Redis (delete-before-write, JSON serialization)");
        algoLabel.setText(
            "AlgorithmUtils: MergeSort O(n log n) stable · BinarySearch O(log n)  "
            + "— integrated in DoctorServiceImpl.findByDepartment");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private void setActionLabel(Label label, String text) {
        if (label != null) label.setText(text);
    }
}