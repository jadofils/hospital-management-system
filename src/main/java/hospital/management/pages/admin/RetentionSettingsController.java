package hospital.management.pages.admin;

import hospital.management.pages.BasePageController;
import hospital.management.backend.daemon.RetentionPolicy;
import hospital.management.enums.PageRoute;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

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
 *   TODO: initialize()  — call RetentionPolicyStore.load() and populate spinners
 *   TODO: onSave()      — call RetentionPolicyStore.save() + DatabaseCleanupDaemon.restart()
 *   TODO: onRunNow()    — call DatabaseCleanupDaemon.runNow()
 *   TODO: onPreview()   — call UserInactivityCleaner.previewCount(), query log counts
 *   TODO: onReset()     — reset spinners to RetentionPolicy defaults
 *   TODO: EventBus      — subscribe DATA_CLEANING_* events to update lastRunLog + statusLabel
 */
public class RetentionSettingsController extends BasePageController {

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
        statusLabel.setText("Daemon is running. Next scheduled run: —");
        lastRunLabel.setText("Last run: never");
        nextRunLabel.setText("—");
        logDirLabel.setText("Log directory: " +
            System.getProperty("user.home") + "/.hms/logs/");

        // TODO: load real policy from RetentionPolicyStore.load()
        // TODO: subscribe to EventBus DATA_CLEANING_* events
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

    // ── Handlers (stubs) ──────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        try {
            RetentionPolicy policy = buildPolicyFromForm();
            // TODO: RetentionPolicyStore.save(policy);
            // TODO: DatabaseCleanupDaemon.restart();
            appendToLog("[STUB] Settings saved: " + policy);
            statusLabel.setText("Settings saved — daemon will restart on next cycle.");
            toastSuccess("Retention settings saved.");
        } catch (IllegalArgumentException e) {
            toastError(e.getMessage());
        }
    }

    @FXML
    private void onRunNow() {
        confirm("Run Cleanup Now",
                "This will immediately run the data retention cleanup using the current settings. Continue?",
                () -> {
                    try {
                        RetentionPolicy policy = buildPolicyFromForm();
                        // TODO: DatabaseCleanupDaemon.runNow();
                        appendToLog("[STUB] Manual run triggered with policy: " + policy);
                        lastRunLabel.setText("Last run: just now");
                        statusLabel.setText("Cleanup completed.");
                        toastSuccess("Cleanup run completed.");
                    } catch (IllegalArgumentException e) {
                        toastError(e.getMessage());
                    }
                });
    }

    @FXML
    private void onPreview() {
        // TODO: run previewCount queries and update all preview labels
        previewNote.setText("Preview not yet connected to backend.");
        previewUserCount.setText("?");
        previewSysLogCount.setText("?");
        previewAuditLogCount.setText("?");
        previewArchiveCount.setText("?");
        previewDeleteCount.setText("?");
    }

    @FXML
    private void onResetDefaults() {
        inactiveUserDaysSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_INACTIVE_USER_DAYS);
        dbLogRetentionSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_DB_LOG_RETENTION_DAYS);
        fileLogMaxSizeSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_FILE_LOG_MAX_SIZE_MB);
        archiveRetentionSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_ARCHIVE_RETENTION_DAYS);
        intervalHoursSpinner.getValueFactory().setValue(RetentionPolicy.DEFAULT_CLEANUP_INTERVAL_HOURS);
        appendToLog("[INFO] Settings reset to defaults.");
    }

    @FXML
    private void onClearLog() {
        lastRunLog.clear();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void appendToLog(String line) {
        lastRunLog.appendText(line + "\n");
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
    }
}