package hospital.management.backend.config.cache;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.exceptions.ConfigurationException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * Jedis connection pool lifecycle.
 *
 * Pool exhaustion behaviour: blocks the calling thread for at most
 * {@link RedisConfig#MAX_WAIT_MS} ms, then throws {@code JedisException}.
 * {@link hospital.management.backend.cache.CacheService} catches that exception
 * and treats it as a cache miss so the caller falls through to the database —
 * a Redis hiccup never breaks a live request.
 *
 * Usage:
 *   try (Jedis jedis = RedisConnection.getJedis()) {
 *       jedis.set("key", "value");
 *   }
 *
 * Call {@link #shutdown()} during application teardown.
 */
public final class RedisConnection {

    private static final AppLogger logger = AppLogger.getLogger(RedisConnection.class);
    private static final JedisPool POOL;

    static {
        try {
            JedisPoolConfig cfg = new JedisPoolConfig();
            cfg.setMaxTotal(RedisConfig.MAX_POOL_SIZE);
            cfg.setMaxIdle(RedisConfig.MAX_IDLE);
            cfg.setMinIdle(RedisConfig.MIN_IDLE);
            cfg.setTestOnBorrow(true);
            cfg.setTestOnReturn(false);
            cfg.setTestWhileIdle(true);
            cfg.setBlockWhenExhausted(true);
            cfg.setMaxWait(Duration.ofMillis(RedisConfig.MAX_WAIT_MS));

            POOL = new JedisPool(
                    cfg,
                    RedisConfig.getHost(),
                    RedisConfig.getPort(),
                    RedisConfig.CONNECTION_TIMEOUT_MS,
                    RedisConfig.SOCKET_TIMEOUT_MS,
                    RedisConfig.getPassword(),
                    0,    // default database
                    null  // client name
            );

            logger.info("Redis pool ready — " + RedisConfig.getHost() + ":" + RedisConfig.getPort()
                    + " (max " + RedisConfig.MAX_POOL_SIZE + " connections, wait " + RedisConfig.MAX_WAIT_MS + " ms)");
        } catch (Exception e) {
            throw new ConfigurationException("Failed to initialise Redis connection pool: " + e.getMessage(), e);
        }
    }

    private RedisConnection() {}

    /**
     * Borrows a Jedis instance from the pool.
     * Always use in a try-with-resources block so the connection is returned automatically.
     * Throws {@code JedisException} if the pool is exhausted after {@link RedisConfig#MAX_WAIT_MS}.
     */
    public static Jedis getJedis() {
        return POOL.getResource();
    }

    /**
     * Returns true if Redis is reachable and responds to PING.
     * Safe to call at startup as a health check.
     */
    public static boolean isHealthy() {
        try (Jedis jedis = POOL.getResource()) {
            return "PONG".equalsIgnoreCase(jedis.ping());
        } catch (Exception e) {
            logger.warn("Redis health check failed: " + e.getMessage());
            return false;
        }
    }

    /** Closes the pool — call this on application shutdown. */
    public static void shutdown() {
        if (POOL != null && !POOL.isClosed()) {
            POOL.close();
            logger.info("Redis pool shut down.");
        }
    }
}