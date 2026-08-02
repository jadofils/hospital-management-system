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
 * Persists RetentionPolicy to a properties file in the user's home directory.
 * The file is created with defaults on first run.
 *
 * Storage location: ~/.hms/retention.properties
 *
 * The admin UI reads via load() and writes via save().
 * The daemon reads via load() at startup and on every cycle.
 */
public final class RetentionPolicyStore {

    private static final AppLogger logger = AppLogger.getLogger(RetentionPolicyStore.class);
    private static final Path STORE_PATH  =
        Paths.get(System.getProperty("user.home"), ".hms", "retention.properties");

    private static final String KEY_INACTIVE_USER    = "inactive.user.days";
    private static final String KEY_DB_LOG           = "db.log.retention.days";
    private static final String KEY_FILE_LOG_MB      = "file.log.max.size.mb";
    private static final String KEY_ARCHIVE_DAYS     = "archive.retention.days";
    private static final String KEY_INTERVAL_HOURS   = "cleanup.interval.hours";

    private RetentionPolicyStore() {}

    /**
     * Loads the policy from disk. Creates the file with defaults if it does not exist.
     */
    public static RetentionPolicy load() {
        ensureFileExists();
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(STORE_PATH)) {
            props.load(in);
        } catch (IOException e) {
            logger.warn("Could not read retention policy — using defaults: " + e.getMessage());
            return new RetentionPolicy();
        }

        RetentionPolicy policy = new RetentionPolicy();
        policy.setInactiveUserDays(intOf(props, KEY_INACTIVE_USER,   RetentionPolicy.DEFAULT_INACTIVE_USER_DAYS));
        policy.setDbLogRetentionDays(intOf(props, KEY_DB_LOG,        RetentionPolicy.DEFAULT_DB_LOG_RETENTION_DAYS));
        policy.setFileLogMaxSizeMb(intOf(props, KEY_FILE_LOG_MB,     RetentionPolicy.DEFAULT_FILE_LOG_MAX_SIZE_MB));
        policy.setArchiveRetentionDays(intOf(props, KEY_ARCHIVE_DAYS,RetentionPolicy.DEFAULT_ARCHIVE_RETENTION_DAYS));
        policy.setCleanupIntervalHours(intOf(props, KEY_INTERVAL_HOURS,RetentionPolicy.DEFAULT_CLEANUP_INTERVAL_HOURS));

        logger.info("Retention policy loaded: " + policy);
        return policy;
    }

    /**
     * Saves a policy to disk. Call this from the admin settings UI after the
     * admin clicks "Save". The daemon picks up the new values on its next cycle.
     */
    public static void save(RetentionPolicy policy) {
        ensureFileExists();
        Properties props = new Properties();
        props.setProperty(KEY_INACTIVE_USER,  String.valueOf(policy.getInactiveUserDays()));
        props.setProperty(KEY_DB_LOG,         String.valueOf(policy.getDbLogRetentionDays()));
        props.setProperty(KEY_FILE_LOG_MB,    String.valueOf(policy.getFileLogMaxSizeMb()));
        props.setProperty(KEY_ARCHIVE_DAYS,   String.valueOf(policy.getArchiveRetentionDays()));
        props.setProperty(KEY_INTERVAL_HOURS, String.valueOf(policy.getCleanupIntervalHours()));

        try (OutputStream out = Files.newOutputStream(STORE_PATH)) {
            props.store(out, "HMS Retention Policy — managed by admin settings");
            logger.info("Retention policy saved: " + policy);
        } catch (IOException e) {
            logger.error("Failed to save retention policy: " + e.getMessage(), e);
        }
    }

    private static void ensureFileExists() {
        try {
            Files.createDirectories(STORE_PATH.getParent());
            if (!Files.exists(STORE_PATH)) {
                save(new RetentionPolicy());
            }
        } catch (IOException e) {
            logger.warn("Could not create retention policy file: " + e.getMessage());
        }
    }

    private static int intOf(Properties props, String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}