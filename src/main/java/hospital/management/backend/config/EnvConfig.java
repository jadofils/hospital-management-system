package hospital.management.backend.config;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {
    private static final Dotenv dotenv = Dotenv.load();

    // ── Database ──────────────────────────────────────────────────────────
    public static String getUrl()      { return dotenv.get("DB_URL"); }
    public static String getUser()     { return dotenv.get("DB_USER"); }
    public static String getPassword() { return dotenv.get("DB_PASSWORD"); }

    // ── Cloudinary ────────────────────────────────────────────────────────
    public static String getCloudName()      { return dotenv.get("CLOUDINARY_CLOUD_NAME"); }
    public static String getCloudApiKey()    { return dotenv.get("CLOUDINARY_API_KEY"); }
    public static String getCloudApiSecret() { return dotenv.get("CLOUDINARY_API_SECRET"); }

    // ── Gmail / SMTP ──────────────────────────────────────────────────────
    public static String getMailHost()     { return dotenv.get("GMAIL_HOST"); }
    public static int    getMailPort()     { return Integer.parseInt(dotenv.get("GMAIL_PORT")); }
    public static String getMailUsername() { return dotenv.get("GMAIL_USERNAME"); }
    public static String getMailPassword() { return dotenv.get("GMAIL_PASSWORD"); }
    public static String getMailFromName() { return dotenv.get("GMAIL_FROM_NAME"); }

    // ── JWT ───────────────────────────────────────────────────────────────
    public static String getJwtSecret()      { return dotenv.get("JWT_SECRET"); }
    public static int    getJwtExpiryHours() { return Integer.parseInt(dotenv.get("JWT_EXPIRY_HOURS")); }

    // ── Security ──────────────────────────────────────────────────────────
    public static int    getBcryptRounds()   { return Integer.parseInt(dotenv.get("BCRYPT_ROUNDS")); }
    public static String getEncryptionKey()  { return dotenv.get("ENCRYPTION_KEY"); }

    // ── Redis ─────────────────────────────────────────────────────────────
    public static String getRedisHost()     { return dotenv.get("REDIS_HOST"); }
    public static int    getRedisPort()     { return Integer.parseInt(dotenv.get("REDIS_PORT")); }
    public static String getRedisPassword() { return dotenv.get("REDIS_PASSWORD"); }

    // ── Application ───────────────────────────────────────────────────────
    public static int getPageSize()          { return Integer.parseInt(dotenv.get("APP_PAGE_SIZE")); }
    public static int getMaxUploadSizeMb()   { return Integer.parseInt(dotenv.get("APP_MAX_UPLOAD_SIZE_MB")); }

    // ── MongoDB (NoSQL) ──────────────────────────────────────────────────
    public static String getMongoUri() {
        // Prefer MONGO_URL (project convention), fallback to MONGO_URI for compatibility.
        String primary = dotenv.get("MONGO_URL");
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        String fallback = dotenv.get("MONGO_URI");
        return fallback == null || fallback.isBlank() ? "mongodb://localhost:27017" : fallback;
    }
}
