package hospital.management.backend.config.db;

import hospital.management.backend.config.EnvConfig;

/**
 * Database configuration constants.
 * Separates WHAT the connection looks like from HOW the pool is managed.
 *
 * Credentials come from .env via EnvConfig.
 * Pool tuning constants are fixed here — sensible defaults for a single-user desktop app.
 * If you move to a multi-user server, raise MAX_POOL_SIZE and lower MIN_IDLE.
 */
public final class DBConfig {

    // ── Credentials (from .env) ───────────────────────────────────────────────
    public static String getUrl()      { return EnvConfig.getUrl(); }
    public static String getUsername() { return EnvConfig.getUser(); }
    public static String getPassword() { return EnvConfig.getPassword(); }

    // ── Pool sizing ───────────────────────────────────────────────────────────
    public static final int  MAX_POOL_SIZE = 10;
    public static final int  MIN_IDLE      = 2;

    // ── Timeouts (milliseconds) ───────────────────────────────────────────────
    /** How long to wait for a connection from the pool before throwing. */
    public static final long CONNECTION_TIMEOUT_MS = 20_000L;
    /** How long an idle connection stays in the pool before being evicted. */
    public static final long IDLE_TIMEOUT_MS       = 30_000L;
    /** Maximum lifetime of any connection, regardless of idle state. */
    public static final long MAX_LIFETIME_MS       = 1_800_000L; // 30 min

    // ── Identity ──────────────────────────────────────────────────────────────
    public static final String POOL_NAME = "HMS-Pool";

    private DBConfig() {}
}