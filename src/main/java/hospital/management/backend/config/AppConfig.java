package hospital.management.backend.config;

/**
 * Application-wide constants and environment-backed settings.
 *
 * Hardcoded constants (same in all environments):
 *   APP_NAME, APP_VERSION, WINDOW_WIDTH, WINDOW_HEIGHT, CSS_PATH
 *
 * Environment-backed (read from .env, can differ per deployment):
 *   getPageSize()        — APP_PAGE_SIZE
 *   getMaxUploadSizeMb() — APP_MAX_UPLOAD_SIZE_MB
 */
public final class AppConfig {

    // ── Identity ──────────────────────────────────────────────────────────
    public static final String APP_NAME    = "Hospital Management System";
    public static final String APP_VERSION = "1.0.0";

    // ── Window defaults ────────────────────────────────────────────────────
    public static final int WINDOW_WIDTH  = 1280;
    public static final int WINDOW_HEIGHT = 800;

    // ── Resource paths ────────────────────────────────────────────────────
    public static final String CSS_PATH         = "/hospital/management/css/global.css";
    public static final String HOME_FXML_PATH   = "/hospital/management/frontend/pages/home-page.fxml";

    // ── Pagination ────────────────────────────────────────────────────────
    /** Default number of rows per page in all table views. */
    public static int getPageSize() { return EnvConfig.getPageSize(); }

    // ── File upload ───────────────────────────────────────────────────────
    /** Maximum allowed upload size in megabytes (enforced before calling Cloudinary). */
    public static int getMaxUploadSizeMb() { return EnvConfig.getMaxUploadSizeMb(); }

    /** Maximum allowed upload size in bytes. */
    public static long getMaxUploadSizeBytes() { return (long) getMaxUploadSizeMb() * 1024 * 1024; }

    // ── Session ───────────────────────────────────────────────────────────
    /** Token lifetime in hours, mirroring JWT_EXPIRY_HOURS. */
    public static int getSessionExpiryHours() { return EnvConfig.getJwtExpiryHours(); }

    private AppConfig() {}
}