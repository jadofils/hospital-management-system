package hospital.management.backend.daemon;

/**
 * Admin-configurable retention settings for the cleanup daemon.
 * All fields have safe production defaults.
 * Persisted and loaded by RetentionPolicyStore.
 */
public final class RetentionPolicy {

    // ── Defaults ──────────────────────────────────────────────────────────────
    public static final int DEFAULT_INACTIVE_USER_DAYS    = 90;
    public static final int DEFAULT_DB_LOG_RETENTION_DAYS = 90;
    public static final int DEFAULT_FILE_LOG_MAX_SIZE_MB  = 10;
    public static final int DEFAULT_ARCHIVE_RETENTION_DAYS = 15;
    public static final int DEFAULT_CLEANUP_INTERVAL_HOURS = 24;

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Mark user is_active = false after this many days without any login. */
    private int inactiveUserDays    = DEFAULT_INACTIVE_USER_DAYS;

    /** Delete system_logs and audit_log records older than this many days. */
    private int dbLogRetentionDays  = DEFAULT_DB_LOG_RETENTION_DAYS;

    /** Archive (zip) any log file on disk that exceeds this size in megabytes. */
    private int fileLogMaxSizeMb    = DEFAULT_FILE_LOG_MAX_SIZE_MB;

    /**
     * Delete an archived log file if its last-access time is older than this
     * many days. Prevents archives from accumulating indefinitely.
     */
    private int archiveRetentionDays = DEFAULT_ARCHIVE_RETENTION_DAYS;

    /** How often the daemon wakes up and runs all tasks, in hours. */
    private int cleanupIntervalHours = DEFAULT_CLEANUP_INTERVAL_HOURS;

    public RetentionPolicy() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getInactiveUserDays()     { return inactiveUserDays; }
    public int getDbLogRetentionDays()   { return dbLogRetentionDays; }
    public int getFileLogMaxSizeMb()     { return fileLogMaxSizeMb; }
    public int getArchiveRetentionDays() { return archiveRetentionDays; }
    public int getCleanupIntervalHours() { return cleanupIntervalHours; }

    public void setInactiveUserDays(int days)     { this.inactiveUserDays = validate(days, 1); }
    public void setDbLogRetentionDays(int days)   { this.dbLogRetentionDays = validate(days, 1); }
    public void setFileLogMaxSizeMb(int mb)       { this.fileLogMaxSizeMb = validate(mb, 1); }
    public void setArchiveRetentionDays(int days) { this.archiveRetentionDays = validate(days, 1); }
    public void setCleanupIntervalHours(int hours){ this.cleanupIntervalHours = validate(hours, 1); }

    private int validate(int value, int min) {
        if (value < min) throw new IllegalArgumentException("Value must be >= " + min);
        return value;
    }

    @Override
    public String toString() {
        return "RetentionPolicy{inactiveUser=" + inactiveUserDays + "d, dbLog=" + dbLogRetentionDays
            + "d, fileLog=" + fileLogMaxSizeMb + "MB, archive=" + archiveRetentionDays
            + "d, interval=" + cleanupIntervalHours + "h}";
    }
}