package hospital.management.backend.cache;

import hospital.management.backend.config.EnvConfig;

/**
 * TTL (time-to-live) policy per logical domain.
 *
 * Two TTLs per domain:
 *  - {@link #getTtlSeconds()}  — L2 (Redis) TTL; full lifetime for the cached value.
 *  - {@link #getL1TtlSeconds()} — L1 (in-process) TTL; capped at 60 s to bound
 *    how stale in-process data can become relative to Redis.
 *
 * Eviction order (shortest-lived → longest-lived):
 *   LIST → APPOINTMENT/LAB → PATIENT/INVOICE/MEDICAL_RECORD →
 *   PRESCRIPTION → USER/PHARMACY → DOCTOR → ROLE → DEPARTMENT → SESSION
 */
public enum CacheDomain {

    /** Paginated list results; invalidated on every write in that domain. */
    LIST(3 * 60),

    /** Active appointments — change frequently. */
    APPOINTMENT(5 * 60),

    /** Lab orders and results. */
    LAB(5 * 60),

    /** Patient profile; changes on admission / update. */
    PATIENT(10 * 60),

    /** Invoice records; change on payment. */
    INVOICE(10 * 60),

    /** Medical records; rarely updated after creation. */
    MEDICAL_RECORD(10 * 60),

    /** Prescriptions. */
    PRESCRIPTION(15 * 60),

    /** User account; changes on password-reset or deactivation. */
    USER(20 * 60),

    /** Medications and inventory; stock changes on dispense. */
    PHARMACY(20 * 60),

    /** Doctor profile; changes rarely. */
    DOCTOR(30 * 60),

    /** Role list; changes only on admin action. */
    ROLE(60 * 60),

    /** Department list; extremely stable. */
    DEPARTMENT(2 * 60 * 60),

    /**
     * Logged-in session; TTL matches JWT_EXPIRY_HOURS so the cache entry
     * naturally expires at the same time as the token.
     */
    SESSION(EnvConfig.getJwtExpiryHours() * 3600);

    private final int ttlSeconds;

    CacheDomain(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    /** L2 (Redis) TTL in seconds. */
    public int getTtlSeconds() {
        return ttlSeconds;
    }

    /**
     * L1 (in-process) TTL in seconds — capped at 60 s regardless of the L2 TTL.
     * Prevents in-process data from drifting too far from Redis when writes happen
     * on other nodes or threads that bypass this JVM's L1.
     */
    public int getL1TtlSeconds() {
        return Math.min(ttlSeconds, 60);
    }
}