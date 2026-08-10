package hospital.management.backend.daemon;

/**
 * Admin-configurable settings for the backup daemon.
 * All fields have safe production defaults.
 * Persisted and loaded by BackupPolicyStore.
 */
public final class BackupPolicy {

    // ── Defaults ──────────────────────────────────────────────────────────────
    public static final BackupType DEFAULT_BACKUP_TYPE = BackupType.PARTIAL;
    public static final int DEFAULT_BACKUP_INTERVAL_HOURS = 24;
    public static final int DEFAULT_BACKUP_RETENTION_DAYS = 30;
    public static final boolean DEFAULT_SCHEDULED_BACKUPS_ENABLED = false;

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Backup scope used by scheduled runs (manual "Backup Now" picks its own type). */
    private BackupType backupType = DEFAULT_BACKUP_TYPE;

    /** How often the daemon takes a scheduled backup, in hours. */
    private int backupIntervalHours = DEFAULT_BACKUP_INTERVAL_HOURS;

    /** Delete backup directories older than this many days. */
    private int backupRetentionDays = DEFAULT_BACKUP_RETENTION_DAYS;

    /**
     * Opt-in: a FULL scheduled backup silently writes a complete copy of the
     * database to local disk on a timer. Off by default so disk usage is a
     * deliberate admin choice; manual "Backup Now" always works regardless.
     */
    private boolean scheduledBackupsEnabled = DEFAULT_SCHEDULED_BACKUPS_ENABLED;

    public BackupPolicy() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public BackupType getBackupType()          { return backupType; }
    public int getBackupIntervalHours()         { return backupIntervalHours; }
    public int getBackupRetentionDays()         { return backupRetentionDays; }
    public boolean isScheduledBackupsEnabled()   { return scheduledBackupsEnabled; }

    public void setBackupType(BackupType type) {
        if (type == null) throw new IllegalArgumentException("Backup type must not be null");
        this.backupType = type;
    }
    public void setBackupIntervalHours(int hours) { this.backupIntervalHours = validate(hours, 1); }
    public void setBackupRetentionDays(int days)  { this.backupRetentionDays = validate(days, 1); }
    public void setScheduledBackupsEnabled(boolean enabled) { this.scheduledBackupsEnabled = enabled; }

    private int validate(int value, int min) {
        if (value < min) throw new IllegalArgumentException("Value must be >= " + min);
        return value;
    }

    @Override
    public String toString() {
        return "BackupPolicy{type=" + backupType + ", interval=" + backupIntervalHours
            + "h, retention=" + backupRetentionDays + "d, scheduledEnabled=" + scheduledBackupsEnabled + "}";
    }
}
