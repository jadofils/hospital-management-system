package hospital.management.backend.daemon;

/**
 * Contract for a single cleanup operation run by DatabaseCleanupDaemon.
 * Each implementation handles one concern (users, DB logs, file logs).
 */
public interface CleanupTask {

    /** Short name used in log output, e.g. "user-inactivity", "db-logs". */
    String getName();

    /**
     * Executes the cleanup. Called on a background thread by the daemon.
     *
     * @param policy the current retention settings (re-read on every cycle)
     * @return a human-readable summary, e.g. "Deactivated 3 users, deleted 1 204 log rows"
     * @throws Exception any failure — the daemon catches and logs it without stopping
     */
    String run(RetentionPolicy policy) throws Exception;
}