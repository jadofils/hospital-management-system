package hospital.management.backend.mongo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.EnvConfig;

/**
 * Lazy singleton that holds one shared MongoClient for the application.
 *
 * Callers MUST null-check getDatabase() — a null return means MongoDB is
 * unavailable and the caller should silently skip the write/read.
 * MongoDB failure never propagates to the user.
 */
public final class MongoConfig {

    private static final AppLogger logger = AppLogger.getLogger(MongoConfig.class);

    private static volatile MongoClient client;
    private static volatile MongoDatabase database;

    private MongoConfig() {}

    public static MongoDatabase getDatabase() {
        if (database == null) {
            synchronized (MongoConfig.class) {
                if (database == null) {
                    try {
                        client   = MongoClients.create(EnvConfig.getMongoUrl());
                        database = client.getDatabase(EnvConfig.getMongoDatabase());
                        logger.info("MongoDB connected: " + EnvConfig.getMongoDatabase());
                    } catch (Exception e) {
                        logger.warn("MongoDB unavailable — NoSQL writes will be skipped: " + e.getMessage());
                    }
                }
            }
        }
        return database;
    }

    public static void close() {
        if (client != null) {
            try { client.close(); } catch (Exception ignored) {}
        }
    }
}