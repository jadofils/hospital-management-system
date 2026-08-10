package hospital.management.backend.mongo.benchmark;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.mongo.config.MongoConfig;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.UUID;

/**
 * Benchmarks the effect of a single-field index on the {@code patient_notes}
 * collection, mirroring {@link
 * hospital.management.backend.service.analytics.DatabaseInspectionService#benchmarkIndexComparison()}
 * on the PostgreSQL side so the two results can be shown side by side.
 *
 * Uses a plain equality filter on {@code patient_id} for both phases (not
 * Mongo's {@code $text} operator) — {@code $text} requires the index to
 * already exist to run at all, so it can't produce a genuine "without index"
 * baseline on the *same* query shape, which would break the apples-to-apples
 * comparison the Postgres side already guarantees (same ILIKE query, index
 * just toggled).
 *
 * If MongoDB is unavailable, returns a result with {@code -1} timings —
 * callers should display "N/A".
 */
public class MongoIndexBenchmarkService {

    private static final AppLogger logger = AppLogger.getLogger(MongoIndexBenchmarkService.class);
    private static final String COLLECTION = "patient_notes";
    private static final String BENCH_INDEX_NAME = "idx_patient_notes_patient_id_bench";
    private static final int ITERATIONS = 50;

    public record IndexBenchmarkResult(double withoutIndexMs, double withIndexMs, double speedupFactor) {}

    /**
     * Times {@code ITERATIONS} equality lookups on {@code patient_id} before
     * and after creating a single-field index, then drops the created index
     * in a {@code finally} so the collection is left exactly as found — the
     * benchmark is repeatable and leaves no permanent Mongo-side change.
     */
    public IndexBenchmarkResult benchmarkPatientNotesIndexImpact() {
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return new IndexBenchmarkResult(-1, -1, 0);

            MongoCollection<Document> col = db.getCollection(COLLECTION);
            String sampleId = sampleExistingPatientId(col);
            Bson filter = Filters.eq("patient_id", sampleId);

            logger.info("Starting Mongo index benchmark (patient_notes.patient_id) — without-index phase.");
            double withoutIndexMs = runIterations(col, filter);

            col.createIndex(Indexes.ascending("patient_id"), new IndexOptions().name(BENCH_INDEX_NAME));
            try {
                logger.info("Index created — with-index phase.");
                double withIndexMs = runIterations(col, filter);
                double speedup = withIndexMs > 0 ? withoutIndexMs / withIndexMs : 0.0;
                logger.info(String.format(
                    "Mongo index benchmark complete — withoutIndex=%.3f ms, withIndex=%.3f ms, speedup=%.2fx",
                    withoutIndexMs, withIndexMs, speedup));
                return new IndexBenchmarkResult(withoutIndexMs, withIndexMs, speedup);
            } finally {
                col.dropIndex(BENCH_INDEX_NAME);
            }
        } catch (Exception e) {
            logger.warn("Mongo index benchmark failed: " + e.getMessage());
            return new IndexBenchmarkResult(-1, -1, 0);
        }
    }

    private String sampleExistingPatientId(MongoCollection<Document> col) {
        Document first = col.find().first();
        String patientId = first == null ? null : first.getString("patient_id");
        return patientId != null ? patientId : UUID.randomUUID().toString();
    }

    private double runIterations(MongoCollection<Document> col, Bson filter) {
        // Warm-up: one run before timing
        col.find(filter).forEach(d -> {});

        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            col.find(filter).forEach(d -> {});
        }
        long elapsed = System.nanoTime() - start;
        return elapsed / (ITERATIONS * 1_000_000.0);
    }
}
