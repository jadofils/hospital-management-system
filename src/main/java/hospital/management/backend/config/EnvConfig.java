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
}
