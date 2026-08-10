package hospital.management.pages.admin;

import hospital.management.pages.BasePageController;
import hospital.management.backend.daemon.DatabaseCleanupDaemon;
import hospital.management.backend.daemon.RetentionPolicy;
import hospital.management.backend.daemon.RetentionPolicyStore;
import hospital.management.backend.daemon.UserInactivityCleaner;
import hospital.management.backend.dao.log.AuditLogDAOImpl;
import hospital.management.backend.dao.log.SystemLogDAOImpl;
import hospital.management.backend.service.log.AuditServiceImpl;
import hospital.management.backend.service.log.SystemLogServiceImpl;
import hospital.management.backend.service.log.interfaces.AuditService;
import hospital.management.backend.service.log.interfaces.SystemLogService;
import hospital.management.backend.utils.listeners.AppEvent;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.enums.NotificationType;
import hospital.management.enums.PageRoute;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Controller for the admin Retention & Cleanup Settings page.
 *
 * No backend integration yet — spinners are initialised with RetentionPolicy
 * defaults and all action handlers are stubs. Toast/confirm-modal feedback
 * (inherited from {@link BasePageController}) is wired for real, so validation
 * failures and save/run results are surfaced to the user even though nothing
 * is persisted yet.
 *
 * Integration checklist (connect when ready):
 * Integration status:
 *   - load/save policy: wired through RetentionPolicyStore
 *   - daemon control: wired through DatabaseCleanupDaemon (restart/runNow)
 *   - preview: user inactivity estimate + archive count are live
 *   - event stream: DATA_CLEANING_* is subscribed/unsubscribed on page lifecycle
 */
public class RetentionSettingsController extends BasePageController {

     private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
     private static final Path LOG_DIR = Paths.get(System.getProperty("user.home"), ".hms", "logs");

    // ── Services ──────────────────────────────────────────────────────────────
    private final AuditService auditService = new AuditServiceImpl(new AuditLogDAOImpl());
    private final SystemLogService systemLogService = new SystemLogServiceImpl(new SystemLogDAOImpl());
    private final UserInactivityCleaner userInactivityCleaner = new UserInactivityCleaner();

    private Consumer<AppEvent> onCleaningStarted;
    private Consumer<AppEvent> onCleaningProgress;
    private Consumer<AppEvent> onCleaningCompleted;
    private Consumer<AppEvent> onCleaningFailed;

    // ── Status banner ─────────────────────────────────────────────────────────
    @FXML private HBox  statusBanner;
    @FXML private Label statusLabel;
    @FXML private Label lastRunLabel;
    @FXML private Label nextRunLabel;

    // ── Spinners ──────────────────────────────────────────────────────────────
    @FXML private Spinner<Integer> inactiveUserDaysSpinner;
    @FXML private Spinner<Integer> dbLogRetentionSpinner;
    @FXML private Spinner<Integer> fileLogMaxSizeSpinner;
    @FXML private Spinner<Integer> archiveRetentionSpinner;
    @FXML private Spinner<Integer> intervalHoursSpinner;

    // ── Impact chips ──────────────────────────────────────────────────────────
    @FXML private Label inactiveUserCount;
    @FXML private Label dbLogCount;
    @FXML private Label archiveCount;
    @FXML private Label logDirLabel;

    // ── Preview panel ─────────────────────────────────────────────────────────
    @FXML private Label previewUserCount;
    @FXML private Label previewSysLogCount;
    @FXML private Label previewAuditLogCount;
    @FXML private Label previewArchiveCount;
    @FXML private Label previewDeleteCount;
    @FXML private Label previewNote;

    // ── Last run log ──────────────────────────────────────────────────────────
    @FXML private TextArea lastRunLog;

    // ── Buttons ───────────────────────────────────────────────────────────────
    @FXML private Button saveBtn;
    @FXML private Button runNowBtn;
    @FXML private Button resetDefaultsBtn;
    @FXML private Button previewBtn;
    @FXML private Button clearLogBtn;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        if (sidebarController != null) sidebarController.setActiveItem(PageRoute.RETENTION);

        initSpinners();
        statusLabel.setText("Daemon is running. Next scheduled run: based on saved interval.");
        lastRunLabel.setText("Last run: never");
        nextRunLabel.setText("—");
        logDirLabel.setText("Log directory: " +
            System.getProperty("user.home") + "/.hms/logs/");

        loadPolicyIntoForm();
        refreshArchiveIndicators();
        subscribeCleaningEvents();
    }

    @FXML
    private void onPageHidden() {
        unsubscribeCleaningEvents();
    }

    // ── Spinner setup ─────────────────────────────────────────────────────────

    private void initSpinners() {
        inactiveUserDaysSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 3650, RetentionPolicy.DEFAULT_INACTIVE_USER_DAYS));

        dbLogRetentionSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 3650, RetentionPolicy.DEFAULT_DB_LOG_RETENTION_DAYS));

        fileLogMaxSizeSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 1024, RetentionPolicy.DEFAULT_FILE_LOG_MAX_SIZE_MB));

        archiveRetentionSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 365, RetentionPolicy.DEFAULT_ARCHIVE_RETENTION_DAYS));

        intervalHoursSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 168, RetentionPolicy.DEFAULT_CLEANUP_INTERVAL_HOURS));

        // Commit on focus lost so typing a value without arrow keys takes effect
        makeEditable(inactiveUserDaysSpinner);
        makeEditable(dbLogRetentionSpinner);
        makeEditable(fileLogMaxSizeSpinner);
        makeEditable(archiveRetentionSpinner);
        makeEditable(intervalHoursSpinner);
    }

    private void makeEditable(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                spinner.increment(0); // commits the typed value
            }
        });
    }

    // ── Reads current spinner values into a RetentionPolicy object ────────────

    private RetentionPolicy buildPolicyFromForm() {
        RetentionPolicy policy = new RetentionPolicy();
        policy.setInactiveUserDays(inactiveUserDaysSpinner.getValue());
        policy.setDbLogRetentionDays(dbLogRetentionSpinner.getValue());
        policy.setFileLogMaxSizeMb(fileLogMaxSizeSpinner.getValue());
        policy.setArchiveRetentionDays(archiveRetentionSpinner.getValue());
        policy.setCleanupIntervalHours(intervalHoursSpinner.getValue());
        return policy;
    }

    private void loadPolicyIntoForm() {
        RetentionPolicy policy = RetentionPolicyStore.load();
        inactiveUserDaysSpinner.getValueFactory().setValue(policy.getInactiveUserDays());
        dbLogRetentionSpinner.getValueFactory().setValue(policy.getDbLogRetentionDays());
        fileLogMaxSizeSpinner.getValueFactory().setValue(policy.getFileLogMaxSizeMb());
        archiveRetentionSpinner.getValueFactory().setValue(policy.getArchiveRetentionDays());
        intervalHoursSpinner.getValueFactory().setValue(policy.getCleanupIntervalHours());
        nextRunLabel.setText("Every " + policy.getCleanupIntervalHours() + " hour(s)");
    }

    // ── Handlers (stubs) ──────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        withSpinner(saveBtn, () -> {
            try {
                RetentionPolicy policy = buildPolicyFromForm();
                RetentionPolicyStore.save(policy);
                DatabaseCleanupDaemon.restart();
                appendToLog("[INFO] Settings saved and daemon restarted: " + policy);
                statusLabel.setText("Settings saved and daemon restarted.");
                nextRunLabel.setText("Every " + policy.getCleanupIntervalHours() + " hour(s)");
                toastSuccess("Retention settings saved.");
            } catch (IllegalArgumentException e) {
                toastError(e.getMessage());
            }
        });
    }

    @FXML
    private void onRunNow() {
        confirm("Run Cleanup Now",
                "This will immediately run the data retention cleanup using the current settings. Continue?",
                () -> {
                    try {
                        RetentionPolicy policy = buildPolicyFromForm();
                        RetentionPolicyStore.save(policy);
                        appendToLog("[INFO] Manual cleanup requested.");
                        runNowBtn.setDisable(true);
                        statusLabel.setText("Cleanup running...");
                        DatabaseCleanupDaemon.runNow();
                        toastSuccess("Cleanup run started.");
                    } catch (IllegalArgumentException e) {
                        toastError(e.getMessage());
                    } catch (Exception e) {
                        toastError("Failed to run cleanup: " + e.getMessage());
                    }
                });
    }

    @FXML
    private void onPreview() {
        withSpinner(previewBtn, () -> {
            RetentionPolicy policy;
            try {
                policy = buildPolicyFromForm();
            } catch (IllegalArgumentException e) {
                toastError(e.getMessage());
                return;
            }

            int usersToDeactivate = userInactivityCleaner.previewCount(policy);
            int archiveCount = countArchives();
            previewUserCount.setText(usersToDeactivate >= 0 ? String.valueOf(usersToDeactivate) : "n/a");
            previewSysLogCount.setText("depends on current DB log volume");
            previewAuditLogCount.setText("depends on current DB log volume");
            previewArchiveCount.setText(String.valueOf(archiveCount));
            previewDeleteCount.setText("based on " + policy.getArchiveRetentionDays() + " day retention");
            previewNote.setText("Preview mixes exact (users, archive count) and bounded estimates.");

            inactiveUserCount.setText(usersToDeactivate >= 0 ? String.valueOf(usersToDeactivate) : "n/a");
            archiveCount = Math.max(archiveCount, 0);
            archiveCount = archiveCount; // keep explicit for readability in UI flow
            this.archiveCount.setText(String.valueOf(archiveCount));

            toast("Preview refreshed.", NotificationType.INFO);
        });
    }

    @FXML
    private void onResetDefaults() {
        withSpinner(resetDefaultsBtn, () -> {
            inactiveUserDaysSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_INACTIVE_USER_DAYS);
            dbLogRetentionSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_DB_LOG_RETENTION_DAYS);
            fileLogMaxSizeSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_FILE_LOG_MAX_SIZE_MB);
            archiveRetentionSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_ARCHIVE_RETENTION_DAYS);
            intervalHoursSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_CLEANUP_INTERVAL_HOURS);
            appendToLog("[INFO] Settings reset to defaults.");
            refreshArchiveIndicators();
            toastSuccess("Settings reset to defaults.");
        });
    }

    @FXML
    private void onClearLog() {
        withSpinner(clearLogBtn, () -> {
            lastRunLog.clear();
            toastSuccess("Log cleared.");
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendToLog(String line) {
        lastRunLog.appendText("[" + LocalDateTime.now().format(TS) + "] " + line + "\n");
    }

    /**
     * Called by EventBus listener (to be wired in initialize) when a
     * DATA_CLEANING_COMPLETED event arrives.
     * Updates the log area and re-enables the Run Now button.
     */
    public void onCleaningCompleted(java.util.List<String> summaries) {
        summaries.forEach(this::appendToLog);
        lastRunLabel.setText("Last run: just now");
        statusLabel.setText("Cleanup completed.");
        runNowBtn.setDisable(false);
        refreshArchiveIndicators();
    }

    private void subscribeCleaningEvents() {
        onCleaningStarted = e -> Platform.runLater(() -> {
            statusLabel.setText("Cleanup running...");
            appendToLog("[INFO] Cleanup cycle started.");
            runNowBtn.setDisable(true);
        });
        onCleaningProgress = e -> Platform.runLater(() -> {
            Object payload = e.getPayload();
            appendToLog("[INFO] Task progress: " + payload);
        });
        onCleaningCompleted = e -> Platform.runLater(() -> {
            Object payload = e.getPayload();
            if (payload instanceof java.util.List<?> list) {
                list.forEach(item -> appendToLog(String.valueOf(item)));
            }
            lastRunLabel.setText("Last run: " + LocalDateTime.now().format(TS));
            statusLabel.setText("Cleanup completed.");
            runNowBtn.setDisable(false);
            refreshArchiveIndicators();
        });
        onCleaningFailed = e -> Platform.runLater(() -> {
            appendToLog("[ERROR] " + e.getPayload());
            statusLabel.setText("Cleanup finished with errors.");
            runNowBtn.setDisable(false);
        });

        EventBus.subscribe(AppEventType.DATA_CLEANING_STARTED, onCleaningStarted);
        EventBus.subscribe(AppEventType.DATA_CLEANING_PROGRESS, onCleaningProgress);
        EventBus.subscribe(AppEventType.DATA_CLEANING_COMPLETED, onCleaningCompleted);
        EventBus.subscribe(AppEventType.DATA_CLEANING_FAILED, onCleaningFailed);
    }

    private void unsubscribeCleaningEvents() {
        if (onCleaningStarted != null) {
            EventBus.unsubscribe(AppEventType.DATA_CLEANING_STARTED, onCleaningStarted);
            onCleaningStarted = null;
        }
        if (onCleaningProgress != null) {
            EventBus.unsubscribe(AppEventType.DATA_CLEANING_PROGRESS, onCleaningProgress);
            onCleaningProgress = null;
        }
        if (onCleaningCompleted != null) {
            EventBus.unsubscribe(AppEventType.DATA_CLEANING_COMPLETED, onCleaningCompleted);
            onCleaningCompleted = null;
        }
        if (onCleaningFailed != null) {
            EventBus.unsubscribe(AppEventType.DATA_CLEANING_FAILED, onCleaningFailed);
            onCleaningFailed = null;
        }
    }

    private void refreshArchiveIndicators() {
        int archives = countArchives();
        this.archiveCount.setText(String.valueOf(archives));
        dbLogCount.setText("—");
    }

    private int countArchives() {
        try {
            if (!Files.isDirectory(LOG_DIR)) return 0;
            try (java.util.stream.Stream<Path> stream = Files.list(LOG_DIR)) {
                return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".log.gz"))
                    .count();
            }
        } catch (Exception e) {
            return 0;
        }
    }
}