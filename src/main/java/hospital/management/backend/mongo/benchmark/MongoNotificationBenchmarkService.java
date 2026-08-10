package hospital.management.backend.mongo.benchmark;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.mongo.config.MongoConfig;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB benchmarks for the notifications collection.
 *
 * Uses a dedicated temporary collection ({@code notif_bench_temp}) that is
 * dropped after each benchmark so no permanent data is written.
 *
 * Returns avgMs = -1 (sentinel for "skipped") when MongoDB is unavailable.
 */
public class MongoNotificationBenchmarkService {

    private static final AppLogger logger =
        AppLogger.getLogger(MongoNotificationBenchmarkService.class);
    private static final String TEMP = "notif_bench_temp";

    // ── Public API ────────────────────────────────────────────────────────────

    public List<BenchmarkResult> runAll() {
        List<BenchmarkResult> results = new ArrayList<>();
        results.add(benchmarkInsertSingle());
        results.add(benchmarkInsertBatch());
        results.add(benchmarkFetchByType());
        results.add(benchmarkFetchByRecipient());
        results.add(benchmarkFetchRecent());
        return results;
    }

    // ── Benchmarks ────────────────────────────────────────────────────────────

    /** 100 individual insertOne calls — single-document notification write latency. */
    public BenchmarkResult benchmarkInsertSingle() {
        BenchmarkResult r = new BenchmarkResult("[Mongo-Notif] Insert Single (100 iterations)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return r;
            MongoCollection<Document> col = db.getCollection(TEMP);

            long start = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                col.insertOne(buildDoc("APPOINTMENT_BOOKED",
                    UUID.randomUUID().toString(), "normal"));
            }
            r.avgMs     = (System.nanoTime() - start) / 100_000_000.0;
            r.throughput = 1000.0 / r.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo-Notif insert single failed: " + e.getMessage());
        } finally { dropTemp(); }
        return r;
    }

    /** One insertMany of 100 notification documents — bulk write throughput. */
    public BenchmarkResult benchmarkInsertBatch() {
        BenchmarkResult r = new BenchmarkResult("[Mongo-Notif] Insert Batch (100 records)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return r;
            MongoCollection<Document> col = db.getCollection(TEMP);

            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                batch.add(buildDoc(i % 2 == 0 ? "PATIENT_CREATED" : "BILLING_ISSUED",
                    UUID.randomUUID().toString(), "normal"));
            }
            long start = System.nanoTime();
            col.insertMany(batch);
            r.avgMs     = (System.nanoTime() - start) / 1_000_000.0;
            r.throughput = 100_000.0 / r.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo-Notif insert batch failed: " + e.getMessage());
        } finally { dropTemp(); }
        return r;
    }

    /** 50 equality queries on the type field — measures notification type filter. */
    public BenchmarkResult benchmarkFetchByType() {
        BenchmarkResult r = new BenchmarkResult("[Mongo-Notif] Fetch by Type (50 iterations)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return r;
            MongoCollection<Document> col = seedTemp(db, 200);

            long start = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                col.find(Filters.eq("type", "APPOINTMENT_BOOKED")).limit(20).forEach(d -> {});
            }
            r.avgMs     = (System.nanoTime() - start) / 50_000_000.0;
            r.throughput = 50_000.0 / r.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo-Notif fetch by type failed: " + e.getMessage());
        } finally { dropTemp(); }
        return r;
    }

    /** 50 array-containment queries on recipients — mirrors the PostgreSQL JSONB @> operator. */
    public BenchmarkResult benchmarkFetchByRecipient() {
        BenchmarkResult r = new BenchmarkResult("[Mongo-Notif] Fetch by Recipient (50 iterations)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return r;

            String fixedRecipient = UUID.randomUUID().toString();
            MongoCollection<Document> col = db.getCollection(TEMP);
            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                // half the documents include the fixed recipient
                String recipient = i % 2 == 0 ? fixedRecipient : UUID.randomUUID().toString();
                batch.add(buildDoc("PATIENT_CREATED", recipient, "normal"));
            }
            col.insertMany(batch);

            long start = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                col.find(Filters.eq("recipients", fixedRecipient)).limit(20).forEach(d -> {});
            }
            r.avgMs     = (System.nanoTime() - start) / 50_000_000.0;
            r.throughput = 50_000.0 / r.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo-Notif fetch by recipient failed: " + e.getMessage());
        } finally { dropTemp(); }
        return r;
    }

    /** 50 sort-descending queries — measures notification feed retrieval speed. */
    public BenchmarkResult benchmarkFetchRecent() {
        BenchmarkResult r = new BenchmarkResult("[Mongo-Notif] Fetch Recent 50 (50 iterations)");
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return r;
            MongoCollection<Document> col = seedTemp(db, 200);

            long start = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                col.find()
                   .sort(Sorts.descending("created_at"))
                   .limit(50)
                   .forEach(d -> {});
            }
            r.avgMs     = (System.nanoTime() - start) / 50_000_000.0;
            r.throughput = 50_000.0 / r.avgMs;
        } catch (Exception e) {
            logger.warn("Mongo-Notif fetch recent failed: " + e.getMessage());
        } finally { dropTemp(); }
        return r;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Document buildDoc(String type, String recipientId, String priority) {
        return new Document()
            .append("notification_id", UUID.randomUUID().toString())
            .append("type",            type)
            .append("actor_user_id",   UUID.randomUUID().toString())
            .append("recipients",      Arrays.asList(recipientId))
            .append("payload",         new Document("detail", "benchmark test"))
            .append("channels",        Arrays.asList("in_app"))
            .append("priority",        priority)
            .append("created_at",      new Date());
    }

    private MongoCollection<Document> seedTemp(MongoDatabase db, int count) {
        MongoCollection<Document> col = db.getCollection(TEMP);
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            docs.add(buildDoc(
                i % 3 == 0 ? "APPOINTMENT_BOOKED" : "PATIENT_CREATED",
                UUID.randomUUID().toString(), "normal"));
        }
        col.insertMany(docs);
        return col;
    }

    private void dropTemp() {
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db != null) db.getCollection(TEMP).drop();
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