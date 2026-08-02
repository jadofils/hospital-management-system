package hospital.management.backend.config.cache;

import hospital.management.backend.config.EnvConfig;

public final class RedisConfig {

    // ── Credentials (from .env) ───────────────────────────────────────────
    public static String getHost()     { return EnvConfig.getRedisHost(); }
    public static int    getPort()     { return EnvConfig.getRedisPort(); }
    public static String getPassword() { return EnvConfig.getRedisPassword(); }

    // ── Pool sizing ───────────────────────────────────────────────────────
    public static final int  MAX_POOL_SIZE = 10;
    public static final int  MAX_IDLE      = 5;
    public static final int  MIN_IDLE      = 1;
    /**
     * Maximum time (ms) a caller blocks waiting for a free connection before
     * the borrow is treated as a cache miss and control returns to the caller.
     * Keeps pool exhaustion from stalling the request thread.
     */
    public static final long MAX_WAIT_MS   = 1_000L;

    // ── Timeouts (milliseconds) ───────────────────────────────────────────
    public static final int CONNECTION_TIMEOUT_MS = 2_000;
    public static final int SOCKET_TIMEOUT_MS     = 2_000;

    // ── Internal keys ─────────────────────────────────────────────────────
    /** Sorted-set key used to rank cached keys by access frequency. */
    public static final String FREQ_KEY = "__cache:freq__";

    private RedisConfig() {}
}