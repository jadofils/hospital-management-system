package hospital.management.backend.cache;

import hospital.management.backend.config.AppLogger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe in-process L1 cache backed by a {@link ConcurrentHashMap}.
 *
 * Capacity: {@value #MAX_ENTRIES} entries. When the map is full, the
 * least-recently-used entry is evicted before inserting the new one.
 *
 * Idle eviction: a single daemon thread sweeps every {@value #SWEEP_INTERVAL_S}
 * seconds and removes entries that have either expired or not been accessed
 * for more than {@value #IDLE_EVICT_MS} ms (5 minutes).
 *
 * All reads are lock-free (ConcurrentHashMap.get + AtomicLong.set).
 * LRU eviction on put is O(n) — only triggered when the map is at capacity,
 * which is rare relative to the read frequency.
 */
final class L1Cache {

    private static final AppLogger logger = AppLogger.getLogger(L1Cache.class);

    static final int  MAX_ENTRIES      = 500;
    static final long SWEEP_INTERVAL_S = 60L;
    static final long IDLE_EVICT_MS    = 5 * 60 * 1_000L;

    private static final ConcurrentHashMap<String, CacheEntry<?>> STORE =
            new ConcurrentHashMap<>(MAX_ENTRIES + 1, 0.75f, 16);

    static {
        ScheduledExecutorService sweeper =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "l1-cache-sweeper");
                    t.setDaemon(true);
                    return t;
                });
        sweeper.scheduleAtFixedRate(
                L1Cache::sweep,
                SWEEP_INTERVAL_S,
                SWEEP_INTERVAL_S,
                TimeUnit.SECONDS
        );
    }

    private L1Cache() {}

    // ── Read ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    static <T> Optional<T> get(String key) {
        CacheEntry<?> entry = STORE.get(key);
        if (entry == null) return Optional.empty();
        if (entry.isExpired()) {
            STORE.remove(key, entry);  // conditional remove — avoids racing a concurrent put
            return Optional.empty();
        }
        entry.touch();
        return Optional.of((T) entry.value);
    }

    // ── Write ─────────────────────────────────────────────────────────────

    static <T> void put(String key, T value, long ttlSeconds) {
        if (ttlSeconds <= 0) return;
        enforceCapacity();
        STORE.put(key, new CacheEntry<>(value, ttlSeconds));
    }

    // ── Eviction ──────────────────────────────────────────────────────────

    static void evict(String key) {
        STORE.remove(key);
    }

    /**
     * Removes all keys whose prefix matches the pattern (pattern must end with {@code *}).
     * E.g., {@code "patient:list:*"} removes every key starting with {@code "patient:list:"}.
     */
    static void evictByPattern(String pattern) {
        String prefix = pattern.endsWith("*")
                ? pattern.substring(0, pattern.length() - 1)
                : pattern;
        STORE.keySet().removeIf(k -> k.startsWith(prefix));
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * If the store is at capacity, evict the least-recently-used entry.
     * O(n) scan — acceptable because it fires only when the map is full (rare).
     */
    private static void enforceCapacity() {
        if (STORE.size() < MAX_ENTRIES) return;
        STORE.entrySet().stream()
                .min(Map.Entry.comparingByValue(
                        (a, b) -> Long.compare(a.lastAccessAtMs.get(), b.lastAccessAtMs.get())
                ))
                .ifPresent(lru -> STORE.remove(lru.getKey(), lru.getValue()));
    }

    /** Daemon sweep: remove expired entries and entries idle for more than 5 minutes. */
    private static void sweep() {
        long now     = System.currentTimeMillis();
        int  removed = 0;
        for (Map.Entry<String, CacheEntry<?>> e : STORE.entrySet()) {
            CacheEntry<?> v = e.getValue();
            if (v.isExpired() || (now - v.lastAccessAtMs.get()) >= IDLE_EVICT_MS) {
                if (STORE.remove(e.getKey(), v)) removed++;
            }
        }
        if (removed > 0) {
            logger.info("L1 sweep: evicted " + removed + " entries (" + STORE.size() + " remaining)");
        }
    }
}