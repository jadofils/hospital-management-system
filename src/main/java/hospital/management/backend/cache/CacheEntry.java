package hospital.management.backend.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Value wrapper stored in the L1 in-process cache.
 *
 * Tracks absolute expiry time and last-access timestamp so the L1 sweeper
 * can perform both TTL eviction and idle eviction without extra metadata.
 * {@code lastAccessAtMs} uses an AtomicLong so concurrent readers can touch
 * it without locking the containing map entry.
 */
final class CacheEntry<T> {

    final T           value;
    final long        expiresAtMs;
    final AtomicLong  lastAccessAtMs;

    CacheEntry(T value, long ttlSeconds) {
        long now           = System.currentTimeMillis();
        this.value         = value;
        this.expiresAtMs   = now + ttlSeconds * 1_000L;
        this.lastAccessAtMs = new AtomicLong(now);
    }

    boolean isExpired() {
        return System.currentTimeMillis() >= expiresAtMs;
    }

    void touch() {
        lastAccessAtMs.set(System.currentTimeMillis());
    }
}