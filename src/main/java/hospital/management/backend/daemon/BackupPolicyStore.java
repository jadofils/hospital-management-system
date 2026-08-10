package hospital.management.backend.daemon;

import hospital.management.backend.config.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Persists BackupPolicy to a properties file in the user's home directory.
 * The file is created with defaults on first run.
 *
 * Storage location: ~/.hms/backup.properties (sibling of retention.properties)
 *
 * The admin UI reads via load() and writes via save().
 * The daemon reads via load() at startup and on every cycle.
 */
public final class BackupPolicyStore {

    private static final AppLogger logger = AppLogger.getLogger(BackupPolicyStore.class);

    private static final String KEY_TYPE               = "backup.type";
    private static final String KEY_INTERVAL_HOURS      = "backup.interval.hours";
    private static final String KEY_RETENTION_DAYS      = "backup.retention.days";
    private static final String KEY_SCHEDULED_ENABLED   = "backup.scheduled.enabled";

    private BackupPolicyStore() {}

    private static Path getStorePath() {
        return Paths.get(System.getProperty("user.home"), ".hms", "backup.properties");
    }

    /**
     * Loads the policy from disk. Creates the file with defaults if it does not exist.
     */
    public static BackupPolicy load() {
        ensureFileExists();
        Path storePath = getStorePath();
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(storePath)) {
            props.load(in);
        } catch (IOException e) {
            logger.warn("Could not read backup policy — using defaults: " + e.getMessage());
            return new BackupPolicy();
        }

        BackupPolicy policy = new BackupPolicy();
        policy.setBackupType(typeOf(props, KEY_TYPE, BackupPolicy.DEFAULT_BACKUP_TYPE));
        policy.setBackupIntervalHours(intOf(props, KEY_INTERVAL_HOURS, BackupPolicy.DEFAULT_BACKUP_INTERVAL_HOURS));
        policy.setBackupRetentionDays(intOf(props, KEY_RETENTION_DAYS, BackupPolicy.DEFAULT_BACKUP_RETENTION_DAYS));
        policy.setScheduledBackupsEnabled(boolOf(props, KEY_SCHEDULED_ENABLED, BackupPolicy.DEFAULT_SCHEDULED_BACKUPS_ENABLED));

        logger.info("Backup policy loaded: " + policy);
        return policy;
    }

    /**
     * Saves a policy to disk. Call this from the Developer Dashboard's Backups
     * tab after clicking "Save Settings". The daemon picks up new values on
     * its next cycle (or immediately after BackupDaemon.restart()).
     */
    public static void save(BackupPolicy policy) {
        ensureFileExists();
        Path storePath = getStorePath();
        Properties props = new Properties();
        props.setProperty(KEY_TYPE, policy.getBackupType().name());
        props.setProperty(KEY_INTERVAL_HOURS, String.valueOf(policy.getBackupIntervalHours()));
        props.setProperty(KEY_RETENTION_DAYS, String.valueOf(policy.getBackupRetentionDays()));
        props.setProperty(KEY_SCHEDULED_ENABLED, String.valueOf(policy.isScheduledBackupsEnabled()));

        try (OutputStream out = Files.newOutputStream(storePath)) {
            props.store(out, "HMS Backup Policy — managed by the Developer Dashboard Backups tab");
            logger.info("Backup policy saved: " + policy);
        } catch (IOException e) {
            logger.error("Failed to save backup policy: " + e.getMessage(), e);
        }
    }

    private static void ensureFileExists() {
        Path storePath = getStorePath();
        try {
            Files.createDirectories(storePath.getParent());
            if (!Files.exists(storePath)) {
                writeDefaults(storePath);
            }
        } catch (IOException e) {
            logger.warn("Could not create backup policy file: " + e.getMessage());
        }
    }

    private static void writeDefaults(Path storePath) throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_TYPE, BackupPolicy.DEFAULT_BACKUP_TYPE.name());
        props.setProperty(KEY_INTERVAL_HOURS, String.valueOf(BackupPolicy.DEFAULT_BACKUP_INTERVAL_HOURS));
        props.setProperty(KEY_RETENTION_DAYS, String.valueOf(BackupPolicy.DEFAULT_BACKUP_RETENTION_DAYS));
        props.setProperty(KEY_SCHEDULED_ENABLED, String.valueOf(BackupPolicy.DEFAULT_SCHEDULED_BACKUPS_ENABLED));

        try (OutputStream out = Files.newOutputStream(storePath)) {
            props.store(out, "HMS Backup Policy — managed by the Developer Dashboard Backups tab");
        }
    }

    private static int intOf(Properties props, String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean boolOf(Properties props, String key, boolean defaultValue) {
        String raw = props.getProperty(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(raw);
    }

    private static BackupType typeOf(Properties props, String key, BackupType defaultValue) {
        try {
            String raw = props.getProperty(key);
            return raw == null ? defaultValue : BackupType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
