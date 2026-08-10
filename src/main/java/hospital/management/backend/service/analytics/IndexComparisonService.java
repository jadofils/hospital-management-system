package hospital.management.backend.service.analytics;

import hospital.management.backend.mongo.benchmark.MongoIndexBenchmarkService;

/**
 * Composes the PostgreSQL and MongoDB index-impact benchmarks into a single
 * cross-store comparison, the same way {@link PerformanceBenchmarkService}
 * already composes cross-store results elsewhere on the Developer Dashboard.
 * Does not modify either underlying single-store benchmark.
 */
public class IndexComparisonService {

    /** One store's before/after-indexing measurement. */
    public record StoreIndexComparison(String store, double beforeMs, double afterMs, double speedupFactor) {}

    /** Both stores' measurements, for a side-by-side chart. */
    public record CrossStoreIndexComparison(StoreIndexComparison postgres, StoreIndexComparison mongo) {}

    private final DatabaseInspectionService dbInspectionService;
    private final MongoIndexBenchmarkService mongoIndexBenchmarkService;

    public IndexComparisonService() {
        this(new DatabaseInspectionService(), new MongoIndexBenchmarkService());
    }

    public IndexComparisonService(DatabaseInspectionService dbInspectionService,
                                   MongoIndexBenchmarkService mongoIndexBenchmarkService) {
        this.dbInspectionService = dbInspectionService;
        this.mongoIndexBenchmarkService = mongoIndexBenchmarkService;
    }

    /**
     * Runs the existing, unmodified PostgreSQL index benchmark and the new
     * MongoDB index benchmark, and returns both results together.
     */
    public CrossStoreIndexComparison benchmarkAcrossStores() throws Exception {
        DatabaseInspectionService.IndexBenchmarkComparison pg = dbInspectionService.benchmarkIndexComparison();
        MongoIndexBenchmarkService.IndexBenchmarkResult mongo = mongoIndexBenchmarkService.benchmarkPatientNotesIndexImpact();

        return new CrossStoreIndexComparison(
            new StoreIndexComparison("PostgreSQL", pg.withoutIndexMs(), pg.withIndexMs(), pg.speedupFactor()),
            new StoreIndexComparison("MongoDB", mongo.withoutIndexMs(), mongo.withIndexMs(), mongo.speedupFactor())
        );
    }
}
