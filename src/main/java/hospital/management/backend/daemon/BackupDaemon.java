package hospital.management.backend.daemon;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.service.backup.BackupManifest;
import hospital.management.backend.service.backup.BackupService;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled background daemon that takes a backup on a configurable interval,
 * then prunes backups older than the configured retention window.
 *
 * Lifecycle:
 *   Main.start()  → BackupDaemon.start()   (launches the scheduler)
 *   Main.stop()   → BackupDaemon.stop()    (graceful shutdown)
 *
 * Scheduled runs are opt-in — see {@link BackupPolicy#isScheduledBackupsEnabled()}.
 * When disabled, the daemon still starts (so runNow() and future restart()
 * after enabling still work) but never schedules a recurring run.
 *
 * When the admin saves a new BackupPolicy, call restart() to pick up the new
 * settings immediately without restarting the whole application.
 *
 * Events published to EventBus:
 *   BACKUP_STARTED    — backup run begins
 *   BACKUP_PROGRESS   — one message per table/collection completed (String)
 *   BACKUP_COMPLETED  — run finished (BackupManifest)
 *   BACKUP_FAILED     — run threw (String: error message)
 */
public final class BackupDaemon {

    private static final AppLogger logger = AppLogger.getLogger(BackupDaemon.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final BackupService backupService = new BackupService();

    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?>       future;
    private static volatile LocalDateTime   nextRunAt;

    private BackupDaemon() {}

    /**
     * When the next scheduled backup will fire, or {@code null} if scheduled
     * backups are disabled (or the daemon hasn't started yet). Read by the
     * Developer Dashboard's Backups tab to drive a live countdown.
     */
    public static LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Starts the daemon. Loads the current BackupPolicy and, if scheduled
     * backups are enabled, schedules the first run after one interval. Safe
     * to call from Main.start().
     */
    public static synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.warn("Backup daemon already running — use restart() to apply new settings.");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hms-backup-daemon");
            t.setDaemon(true);
            return t;
        });

        schedule();
        logger.info("Backup daemon started.");
    }

    /**
     * Stops the daemon gracefully. Waits up to 60 seconds for the current
     * backup to finish before forcing shutdown (a full DB dump can take a
     * while longer than a cleanup cycle).
     */
    public static synchronized void stop() {
        if (scheduler == null) return;
        if (future != null) future.cancel(false);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        scheduler = null;
        nextRunAt = null;
        logger.info("Backup daemon stopped.");
    }

    /**
     * Stops then restarts with the latest BackupPolicy from disk.
     * Call this from the Developer Dashboard's Backups tab after saving.
     */
    public static synchronized void restart() {
        stop();
        start();
        logger.info("Backup daemon restarted with updated backup policy.");
    }

    /**
     * Runs a backup cycle immediately, regardless of the scheduled interval
     * or whether scheduled backups are enabled. Admin can trigger this from
     * the Backups tab with a "Backup Now" button (though the UI's manual
     * trigger calls {@link BackupService} directly for tighter progress
     * reporting — this entry point exists for programmatic/"Run Now" use).
     */
    public static void runNow() {
        if (scheduler == null || scheduler.isShutdown()) {
            logger.warn("Backup daemon not running — call start() first.");
            return;
        }
        scheduler.submit(BackupDaemon::runCycle);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void schedule() {
        BackupPolicy policy = BackupPolicyStore.load();
        if (!policy.isScheduledBackupsEnabled()) {
            logger.info("Scheduled backups are disabled — daemon idle (manual Backup Now still works).");
            nextRunAt = null;
            return;
        }

        long intervalHours = policy.getBackupIntervalHours();
        future = scheduler.scheduleAtFixedRate(
            BackupDaemon::runCycle,
            intervalHours,   // initial delay — don't back up immediately at startup
            intervalHours,
            TimeUnit.HOURS
        );
        nextRunAt = LocalDateTime.now().plusHours(intervalHours);

        logger.info("Backup scheduled every " + intervalHours + "h. Next run: " + nextRunAt.format(FMT));
    }

    private static void runCycle() {
        BackupPolicy policy = BackupPolicyStore.load();
        // scheduleAtFixedRate fires again exactly one interval after THIS fire time
        // regardless of how long this cycle takes — advance the countdown accordingly,
        // right away, so the UI stays accurate even while a long backup is running.
        nextRunAt = LocalDateTime.now().plusHours(policy.getBackupIntervalHours());
        logger.info("=== Backup cycle started @ " + LocalDateTime.now().format(FMT)
            + " | policy: " + policy + " ===");

        EventBus.publish(AppEventType.BACKUP_STARTED, policy.getBackupType());

        try {
            BackupManifest manifest = backupService.runBackup(
                policy.getBackupType(),
                msg -> EventBus.publish(AppEventType.BACKUP_PROGRESS, msg));
            EventBus.publish(AppEventType.BACKUP_COMPLETED, manifest);

            int pruned = backupService.pruneOldBackups(policy);
            logger.info("=== Backup cycle finished — status=" + manifest.status
                + ", pruned " + pruned + " old backup(s) ===");
        } catch (Exception e) {
            String error = "Backup cycle FAILED: " + e.getMessage();
            logger.error(error, e);
            EventBus.publish(AppEventType.BACKUP_FAILED, error);
        }
    }
}
