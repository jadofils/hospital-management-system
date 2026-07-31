package hospital.management.backend.daemon;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled background daemon that runs all CleanupTask implementations on a
 * configurable interval set by the admin.
 *
 * Lifecycle:
 *   Main.start()  → DatabaseCleanupDaemon.start()   (launches the scheduler)
 *   Main.stop()   → DatabaseCleanupDaemon.stop()    (graceful shutdown)
 *
 * When the admin saves a new RetentionPolicy, call restart() to pick up the
 * new interval immediately without restarting the whole application.
 *
 * Events published to EventBus:
 *   DATA_CLEANING_STARTED    — daemon cycle begins
 *   DATA_CLEANING_PROGRESS   — after each task completes (Integer: task index)
 *   DATA_CLEANING_COMPLETED  — all tasks finished (List<String>: summaries)
 *   DATA_CLEANING_FAILED     — a task threw (String: error message)
 */
public final class DatabaseCleanupDaemon {

    private static final AppLogger logger = AppLogger.getLogger(DatabaseCleanupDaemon.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<CleanupTask> TASKS = List.of(
        new UserInactivityCleaner(),
        new DbLogCleaner(),
        new FileLogArchiver()
    );

    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?>       future;

    private DatabaseCleanupDaemon() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Starts the daemon. Loads the current RetentionPolicy and schedules the
     * first run after one interval. Safe to call from Main.start().
     */
    public static synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            logger.warn("Daemon already running — use restart() to apply new settings.");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hms-cleanup-daemon");
            t.setDaemon(true);   // killed automatically when JVM exits
            return t;
        });

        schedule();
        logger.info("Cleanup daemon started.");
    }

    /**
     * Stops the daemon gracefully. Waits up to 30 seconds for the current
     * cycle to finish before forcing shutdown.
     */
    public static synchronized void stop() {
        if (scheduler == null) return;
        if (future != null) future.cancel(false);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        scheduler = null;
        logger.info("Cleanup daemon stopped.");
    }

    /**
     * Stops then restarts with the latest RetentionPolicy from disk.
     * Call this from the admin settings controller after saving a new policy.
     */
    public static synchronized void restart() {
        stop();
        start();
        logger.info("Cleanup daemon restarted with updated retention policy.");
    }

    /**
     * Runs all tasks immediately, regardless of the scheduled interval.
     * Admin can trigger this from the settings page with a "Run Now" button.
     */
    public static void runNow() {
        if (scheduler == null || scheduler.isShutdown()) {
            logger.warn("Daemon not running — call start() first.");
            return;
        }
        scheduler.submit(DatabaseCleanupDaemon::runCycle);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void schedule() {
        RetentionPolicy policy = RetentionPolicyStore.load();
        long intervalHours = policy.getCleanupIntervalHours();

        future = scheduler.scheduleAtFixedRate(
            DatabaseCleanupDaemon::runCycle,
            intervalHours,   // initial delay — don't run immediately at startup
            intervalHours,
            TimeUnit.HOURS
        );

        logger.info("Cleanup scheduled every " + intervalHours + "h. Next run: "
            + LocalDateTime.now().plusHours(intervalHours).format(FMT));
    }

    private static void runCycle() {
        RetentionPolicy policy = RetentionPolicyStore.load();
        logger.info("=== Cleanup cycle started @ " + LocalDateTime.now().format(FMT)
            + " | policy: " + policy + " ===");

        EventBus.publish(AppEventType.DATA_CLEANING_STARTED, TASKS.size());

        java.util.List<String> summaries = new java.util.ArrayList<>();
        int completed = 0;

        for (CleanupTask task : TASKS) {
            try {
                String summary = task.run(policy);
                summaries.add("[" + task.getName() + "] " + summary);
                completed++;
                EventBus.publish(AppEventType.DATA_CLEANING_PROGRESS, completed);
            } catch (Exception e) {
                String error = "[" + task.getName() + "] FAILED: " + e.getMessage();
                logger.error(error, e);
                summaries.add(error);
                EventBus.publish(AppEventType.DATA_CLEANING_FAILED, error);
            }
        }

        logger.info("=== Cleanup cycle finished — " + completed + "/" + TASKS.size() + " tasks OK ===");
        EventBus.publish(AppEventType.DATA_CLEANING_COMPLETED, summaries);
    }
}