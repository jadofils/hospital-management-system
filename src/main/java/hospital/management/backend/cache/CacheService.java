package hospital.management.backend.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.cache.RedisConfig;
import hospital.management.backend.config.cache.RedisConnection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Two-tier cache facade: L1 (in-process ConcurrentHashMap) → L2 (Redis).
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  READ PATH                                                              │
 * │                                                                         │
 * │  CacheService.get(key, type)                                            │
 * │      │                                                                  │
 * │      ├─ L1Cache.get(key)   ──hit──▶ touch lastAccessAt → return value  │
 * │      │       miss ↓                                                     │
 * │      ├─ Redis.get(key)     ──hit──▶ backfill L1 (≤60 s) → return value │
 * │      │       miss ↓                                                     │
 * │      └─ Optional.empty()   ──────▶ caller queries DB                   │
 * │                                         │                              │
 * │                                         └─ CacheService.set(key, val,  │
 * │                                                domain) → L1 + L2       │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  WRITE-INVALIDATE (create / update / delete)                            │
 * │                                                                         │
 * │  CacheService.evict(key)              ← removes from L1 + L2 FIRST     │
 * │  CacheService.evictByPattern(pattern) ← for list/search caches         │
 * │  DB.write(entity)                     ← then persist                   │
 * │                                                                         │
 * │  Rule: NEVER write-through on mutations.                                │
 * │  Delete-Before-Write prevents Write-After-Write (WAW) races:            │
 * │  if two threads both update the same record concurrently, the cache     │
 * │  is empty after both evictions so the next read always fetches fresh    │
 * │  data from the DB rather than a stale in-flight value.                  │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Pool exhaustion / Redis / L1 errors are treated as cache misses — never
 * propagated to callers.  A cache miss is always acceptable; a thrown exception
 * during a DB read or write is not.
 *
 * Redis server recommendation:
 *   maxmemory-policy = allkeys-lru
 * This lets Redis automatically evict the least-recently-used L2 keys when
 * memory pressure grows, complementing L1's in-process LRU eviction.
 */
public final class CacheService {

    private static final AppLogger    logger = AppLogger.getLogger(CacheService.class);
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private CacheService() {}

    // ── Read ──────────────────────────────────────────────────────────────

    /**
     * L1 → L2 → empty.
     * On an L2 hit the value is backfilled into L1 with a TTL of at most 60 s
     * so the in-process tier never drifts far from the Redis source of truth.
     */
    public static <T> Optional<T> get(String key, Class<T> type) {
        Optional<T> l1 = L1Cache.get(key);
        if (l1.isPresent()) return l1;

        try (Jedis jedis = RedisConnection.getJedis()) {
            String json = jedis.get(key);
            if (json == null) return Optional.empty();

            recordAccess(jedis, key);
            T value = MAPPER.readValue(json, type);

            long remainingTtl = jedis.ttl(key);
            L1Cache.put(key, value, l1Ttl(remainingTtl));
            return Optional.of(value);
        } catch (Exception e) {
            logger.warn("Cache GET failed [key=" + key + "]: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * L1 → L2 → empty, for generic types such as {@code List<PatientDTO>}.
     *
     * <pre>
     *   Optional<List<PatientDTO>> result = CacheService.get(
     *       CacheKey.patientList(page, size),
     *       new TypeReference<List<PatientDTO>>() {}
     *   );
     * </pre>
     */
    public static <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        Optional<T> l1 = L1Cache.get(key);
        if (l1.isPresent()) return l1;

        try (Jedis jedis = RedisConnection.getJedis()) {
            String json = jedis.get(key);
            if (json == null) return Optional.empty();

            recordAccess(jedis, key);
            T value = MAPPER.readValue(json, typeRef);

            long remainingTtl = jedis.ttl(key);
            L1Cache.put(key, value, l1Ttl(remainingTtl));
            return Optional.of(value);
        } catch (Exception e) {
            logger.warn("Cache GET failed [key=" + key + "]: " + e.getMessage());
            return Optional.empty();
        }
    }

    // ── Write ─────────────────────────────────────────────────────────────

    /**
     * Populates both tiers.
     * L1 is stored with a TTL of {@code min(ttlSeconds, 60)} to cap in-process staleness.
     * L2 (Redis) is stored with the full TTL.
     */
    public static <T> void set(String key, T value, int ttlSeconds) {
        L1Cache.put(key, value, Math.min(ttlSeconds, 60L));

        try (Jedis jedis = RedisConnection.getJedis()) {
            String json = MAPPER.writeValueAsString(value);
            jedis.setex(key, ttlSeconds, json);
        } catch (Exception e) {
            logger.warn("Cache SET failed [key=" + key + "]: " + e.getMessage());
        }
    }

    /**
     * Populates both tiers using the standard TTL for the given domain.
     * Prefer this over the raw-seconds overload so all domain TTLs stay in one place.
     */
    public static <T> void set(String key, T value, CacheDomain domain) {
        set(key, value, domain.getTtlSeconds());
    }

    // ── Eviction (Write-Invalidate) ───────────────────────────────────────

    /**
     * Removes a single key from both L1 and L2.
     * Always call this BEFORE the DB write — Delete-Before-Write pattern.
     */
    public static void evict(String key) {
        L1Cache.evict(key);
        try (Jedis jedis = RedisConnection.getJedis()) {
            jedis.del(key);
        } catch (Exception e) {
            logger.warn("Cache EVICT failed [key=" + key + "]: " + e.getMessage());
        }
    }

    /**
     * Removes all keys matching {@code pattern} (e.g. {@code "patient:list:*"}) from both tiers.
     * L1 uses prefix matching; L2 uses Redis SCAN (non-blocking, production-safe).
     * Always call this BEFORE the DB write.
     */
    public static void evictByPattern(String pattern) {
        String[] patterns = pattern.split("[\\s,]+");
        for (String p : patterns) {
            if (p == null || p.isBlank()) continue;
            evictSinglePattern(p.trim());
        }
    }

    private static void evictSinglePattern(String pattern) {
        L1Cache.evictByPattern(pattern);
        try (Jedis jedis = RedisConnection.getJedis()) {
            ScanParams params = new ScanParams().match(pattern).count(100);
            String cursor = ScanParams.SCAN_POINTER_START;
            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                cursor = result.getCursor();
                List<String> keys = result.getResult();
                if (!keys.isEmpty()) {
                    jedis.del(keys.toArray(new String[0]));
                }
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        } catch (Exception e) {
            logger.warn("Cache EVICT-BY-PATTERN failed [pattern=" + pattern + "]: " + e.getMessage());
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────

    /** Returns true if the key is present in L1 or L2 (not expired). */
    public static boolean exists(String key) {
        if (L1Cache.<Object>get(key).isPresent()) return true;
        try (Jedis jedis = RedisConnection.getJedis()) {
            return jedis.exists(key);
        } catch (Exception e) {
            logger.warn("Cache EXISTS failed [key=" + key + "]: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the TTL remaining for a key in Redis in seconds.
     * Returns -1 if the key has no expiry, -2 if the key does not exist.
     */
    public static long ttl(String key) {
        try (Jedis jedis = RedisConnection.getJedis()) {
            return jedis.ttl(key);
        } catch (Exception e) {
            logger.warn("Cache TTL failed [key=" + key + "]: " + e.getMessage());
            return -2;
        }
    }

    // ── Frequency tracking ────────────────────────────────────────────────

    /**
     * Returns the {@code topN} most frequently accessed L2 keys, highest score first.
     * Backed by the Redis sorted set {@link RedisConfig#FREQ_KEY}.
     * Use in a monitoring or admin panel to understand which data is hottest
     * and whether TTLs need tuning.
     */
    public static List<String> getHottestKeys(int topN) {
        try (Jedis jedis = RedisConnection.getJedis()) {
            return new ArrayList<>(jedis.zrevrange(RedisConfig.FREQ_KEY, 0, topN - 1));
        } catch (Exception e) {
            logger.warn("Cache FREQ-READ failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Returns the cumulative Redis-hit count for a key, or 0 if not tracked. */
    public static long getAccessCount(String key) {
        try (Jedis jedis = RedisConnection.getJedis()) {
            Double score = jedis.zscore(RedisConfig.FREQ_KEY, key);
            return score == null ? 0L : score.longValue();
        } catch (Exception e) {
            logger.warn("Cache FREQ-SCORE failed [key=" + key + "]: " + e.getMessage());
            return 0L;
        }
    }

    /** Resets the frequency leaderboard. Call periodically (e.g. daily) to prevent unbounded growth. */
    public static void resetFrequency() {
        try (Jedis jedis = RedisConnection.getJedis()) {
            jedis.del(RedisConfig.FREQ_KEY);
        } catch (Exception e) {
            logger.warn("Cache FREQ-RESET failed: " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private static void recordAccess(Jedis jedis, String key) {
        try {
            jedis.zincrby(RedisConfig.FREQ_KEY, 1, key);
        } catch (Exception e) {
            // frequency tracking is best-effort
        }
    }

    /**
     * Converts a Redis TTL value to a safe L1 TTL capped at 60 s.
     * -1 (no expiry) and -2 (key not found) both fall back to 60 s.
     */
    private static long l1Ttl(long redisTtl) {
        return Math.min(redisTtl > 0 ? redisTtl : 60L, 60L);
    }
}