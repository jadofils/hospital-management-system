package hospital.management.backend.mongo.benchmark;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.mongo.config.MongoConfig;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Real MongoDB benchmark methods that mirror the PostgreSQL benchmarks in
 * PerformanceBenchmarkService, enabling a genuine before/after comparison.
 *
 * Collection used: benchmark_temp — created on demand and dropped after each run
 * so benchmarks leave no permanent data in the database.
 *
 * If MongoDB is unavailable, all BenchmarkResult objects return avgMs = -1
 * (sentinel value meaning "not run") and the caller should display "N/A".
 */
public class MongoBenchmarkService {

    private static final AppLogger logger = AppLogger.getLogger(MongoBenchmarkService.class);
    private static final String TEMP_COLLECTION = "benchmark_temp";

    // ── Public API ────────────────────────────────────────────────────────────

    /** Runs all MongoDB benchmarks in sequence and returns their results. */
    public List<BenchmarkResult> runAll() {
        List<BenchmarkResult> results = new ArrayList<>();
        results.add(benchmarkInsertSingle());
        results.add(benchmarkInsertBatch());
        results.add(benchmarkFetchByLevel());
        results.add(benchmarkKeywordSearch());
        results.add(benchmarkFetchRecent());
        return results;
    }

    // ── Benchmarks ────────────────────────────────────────────────────────────

    /** 100 individual insertOne calls — measures single-document write latency. */
    public BenchmarkResult benchmarkInsertSingle() {
        BenchmarkResult result = new BenchmarkResult("[Mongo] Insert Single (100 iterations)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return result;
            MongoCollection<Document> col = db.getCollection(TEMP_COLLECTION);

            long start = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                col.insertOne(new Document()
                    .append("level",      "INFO")
                    .append("source",     "BENCHMARK")
                    .append("message",    "Performance test " + i)
                    .append("created_at", new Date()));
            }
            result.avgMs     = (System.nanoTime() - start) / 100_000_000.0;
            result.throughput = 1000.0 / result.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo insert single benchmark failed: " + e.getMessage());
        } finally {
            dropTemp();
        }
        return result;
    }

    /** One insertMany of 100 documents — measures bulk write throughput. */
    public BenchmarkResult benchmarkInsertBatch() {
        BenchmarkResult result = new BenchmarkResult("[Mongo] Insert Batch (100 records)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return result;
            MongoCollection<Document> col = db.getCollection(TEMP_COLLECTION);

            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                batch.add(new Document()
                    .append("level",      "INFO")
                    .append("source",     "BENCHMARK_BATCH")
                    .append("message",    "Batch test " + i)
                    .append("created_at", new Date()));
            }
            long start = System.nanoTime();
            col.insertMany(batch);
            result.avgMs     = (System.nanoTime() - start) / 1_000_000.0;
            result.throughput = 100_000.0 / result.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo insert batch benchmark failed: " + e.getMessage());
        } finally {
            dropTemp();
        }
        return result;
    }

    /** 50 equality-filter queries on the level field — measures indexed read latency. */
    public BenchmarkResult benchmarkFetchByLevel() {
        BenchmarkResult result = new BenchmarkResult("[Mongo] Fetch by Level Field (50 iterations)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return result;

            // Pre-populate so the queries have documents to read
            MongoCollection<Document> col = seedTemp(db, 200);

            long start = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                col.find(Filters.eq("level", "INFO")).limit(20).forEach(d -> {});
            }
            result.avgMs     = (System.nanoTime() - start) / 50_000_000.0;
            result.throughput = 50_000.0 / result.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo fetch by level benchmark failed: " + e.getMessage());
        } finally {
            dropTemp();
        }
        return result;
    }

    /** 50 regex queries on the message field — measures unindexed text scan. */
    public BenchmarkResult benchmarkKeywordSearch() {
        BenchmarkResult result = new BenchmarkResult("[Mongo] Keyword Search — regex on message (50 iterations)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return result;

            MongoCollection<Document> col = seedTemp(db, 200);

            long start = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                col.find(Filters.regex("message", "test", "i")).limit(20).forEach(d -> {});
            }
            result.avgMs     = (System.nanoTime() - start) / 50_000_000.0;
            result.throughput = 50_000.0 / result.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo keyword search benchmark failed: " + e.getMessage());
        } finally {
            dropTemp();
        }
        return result;
    }

    /** 50 sort-by-created_at DESC queries — measures cursor sort performance. */
    public BenchmarkResult benchmarkFetchRecent() {
        BenchmarkResult result = new BenchmarkResult("[Mongo] Fetch Recent 50 Records (50 iterations)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return result;

            MongoCollection<Document> col = seedTemp(db, 200);

            long start = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                col.find()
                   .sort(Sorts.descending("created_at"))
                   .limit(50)
                   .forEach(d -> {});
            }
            result.avgMs     = (System.nanoTime() - start) / 50_000_000.0;
            result.throughput = 50_000.0 / result.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo fetch recent benchmark failed: " + e.getMessage());
        } finally {
            dropTemp();
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MongoCollection<Document> seedTemp(MongoDatabase db, int count) {
        MongoCollection<Document> col = db.getCollection(TEMP_COLLECTION);
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            docs.add(new Document()
                .append("level",      i % 3 == 0 ? "WARNING" : "INFO")
                .append("source",     "BENCHMARK_SEED")
                .append("message",    "Benchmark seed test entry " + i)
                .append("created_at", new Date()));
        }
        col.insertMany(docs);
        return col;
    }

    private void dropTemp() {
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db != null) db.getCollection(TEMP_COLLECTION).drop();
        } catch (Exception ignored) {}
    }

    // ── Result DTO ────────────────────────────────────────────────────────────

    public static class BenchmarkResult {
        public final String operation;
        public double avgMs     = -1;
        public double throughput = 0;

        public BenchmarkResult(String operation) { this.operation = operation; }
    }
}