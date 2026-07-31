package hospital.management.backend.config;

import com.cloudinary.Cloudinary;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a singleton Cloudinary instance configured from .env via EnvConfig.
 *
 * Required .env keys:
 *   CLOUDINARY_CLOUD_NAME
 *   CLOUDINARY_API_KEY
 *   CLOUDINARY_API_SECRET
 *
 * Usage:
 *   Cloudinary cloud = CloudinaryConfig.get();
 *   Map result = cloud.uploader().upload(file, ObjectUtils.emptyMap());
 *   String url  = (String) result.get("secure_url");
 */
public final class CloudinaryConfig {

    private static final AppLogger  logger = AppLogger.getLogger(CloudinaryConfig.class);
    private static final Cloudinary INSTANCE;

    static {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", EnvConfig.getCloudName());
        config.put("api_key",    EnvConfig.getCloudApiKey());
        config.put("api_secret", EnvConfig.getCloudApiSecret());
        config.put("secure",     true);

        INSTANCE = new Cloudinary(config);
        logger.info("Cloudinary initialised — cloud: " + EnvConfig.getCloudName());
    }

    private CloudinaryConfig() {}

    /** Returns the shared, pre-configured Cloudinary instance. */
    public static Cloudinary get() {
        return INSTANCE;
    }
}
