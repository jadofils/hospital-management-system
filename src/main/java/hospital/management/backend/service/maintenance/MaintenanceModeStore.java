package hospital.management.backend.service.maintenance;

import hospital.management.backend.config.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Persists {@link MaintenanceMode} to a properties file in the user's home
 * directory. The file is created with defaults (maintenance off, nobody
 * blocked) on first run.
 *
 * Storage location: ~/.hms/maintenance.properties (sibling of
 * retention.properties / backup.properties).
 *
 * The Developer Dashboard's Maintenance tab reads via load() and writes via
 * save(). {@link MaintenanceGate} reads fresh via load() on every login check
 * — no daemon/restart step needed, a save takes effect on the very next login.
 */
public final class MaintenanceModeStore {

    private static final AppLogger logger = AppLogger.getLogger(MaintenanceModeStore.class);

    private static final String KEY_ENABLED     = "maintenance.enabled";
    private static final String KEY_BLOCKED_IDS = "maintenance.blocked.user.ids";
    private static final String KEY_STATUS_PAGE  = "maintenance.status.page";
    private static final String KEY_MESSAGE      = "maintenance.message";

    private MaintenanceModeStore() {}

    private static Path getStorePath() {
        return Paths.get(System.getProperty("user.home"), ".hms", "maintenance.properties");
    }

    public static MaintenanceMode load() {
        ensureFileExists();
        Path storePath = getStorePath();
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(storePath)) {
            props.load(in);
        } catch (IOException e) {
            logger.warn("Could not read maintenance mode — using defaults: " + e.getMessage());
            return new MaintenanceMode();
        }

        MaintenanceMode mode = new MaintenanceMode();
        mode.setEnabled(Boolean.parseBoolean(props.getProperty(KEY_ENABLED, String.valueOf(MaintenanceMode.DEFAULT_ENABLED))));
        mode.setBlockedUserIds(parseIds(props.getProperty(KEY_BLOCKED_IDS, "")));
        mode.setStatusPage(statusPageOf(props, KEY_STATUS_PAGE, MaintenanceMode.DEFAULT_STATUS_PAGE));
        mode.setMessage(props.getProperty(KEY_MESSAGE, MaintenanceMode.DEFAULT_MESSAGE));
        return mode;
    }

    public static void save(MaintenanceMode mode) {
        ensureFileExists();
        Path storePath = getStorePath();
        Properties props = new Properties();
        props.setProperty(KEY_ENABLED, String.valueOf(mode.isEnabled()));
        props.setProperty(KEY_BLOCKED_IDS, String.join(",", mode.getBlockedUserIds()));
        props.setProperty(KEY_STATUS_PAGE, mode.getStatusPage().name());
        props.setProperty(KEY_MESSAGE, mode.getMessage());

        try (OutputStream out = Files.newOutputStream(storePath)) {
            props.store(out, "HMS Maintenance Mode — managed by the Developer Dashboard Maintenance tab");
            logger.info("Maintenance mode saved: " + mode);
        } catch (IOException e) {
            logger.error("Failed to save maintenance mode: " + e.getMessage(), e);
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
            logger.warn("Could not create maintenance mode file: " + e.getMessage());
        }
    }

    private static void writeDefaults(Path storePath) throws IOException {
        MaintenanceMode defaults = new MaintenanceMode();
        Properties props = new Properties();
        props.setProperty(KEY_ENABLED, String.valueOf(defaults.isEnabled()));
        props.setProperty(KEY_BLOCKED_IDS, "");
        props.setProperty(KEY_STATUS_PAGE, defaults.getStatusPage().name());
        props.setProperty(KEY_MESSAGE, defaults.getMessage());

        try (OutputStream out = Files.newOutputStream(storePath)) {
            props.store(out, "HMS Maintenance Mode — managed by the Developer Dashboard Maintenance tab");
        }
    }

    private static Set<String> parseIds(String csv) {
        Set<String> ids = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) return ids;
        for (String id : csv.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) ids.add(trimmed);
        }
        return ids;
    }

    private static SystemStatusPage statusPageOf(Properties props, String key, SystemStatusPage defaultValue) {
        try {
            String raw = props.getProperty(key);
            return raw == null ? defaultValue : SystemStatusPage.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
