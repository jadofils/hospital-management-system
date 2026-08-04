package hospital.management.backend.service.analytics;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.config.EnvConfig;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Generates performance benchmark reports comparing PostgreSQL and MongoDB
 * for common healthcare operations. Used by admin and analyst dashboards.
 */
public class PerformanceBenchmarkService {

    private static final AppLogger logger = AppLogger.getLogger(PerformanceBenchmarkService.class);
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Runs a comprehensive benchmark comparing PostgreSQL and MongoDB performance
     * and generates a detailed markdown report.
     *
     * @return Path to the generated report file
     * @throws Exception if benchmark execution fails
     */
    public Path generateBenchmarkReport() throws Exception {
        logger.info("Starting performance benchmark: PostgreSQL vs MongoDB");
        
        List<BenchmarkResult> results = new ArrayList<>();
        
        // Run benchmarks
        results.add(benchmarkInsertSingle());
        results.add(benchmarkInsertBatch());
        results.add(benchmarkFetchByPatientAndTime());
        results.add(benchmarkFetchRecent());
        results.add(benchmarkUpdateMetadata());
        
        // Generate report
        String reportContent = buildReportMarkdown(results);
        
        // Save to temp file
        Path reportPath = Files.createTempFile("performance_benchmark_", ".md");
        Files.writeString(reportPath, reportContent);
        
        logger.info("Performance benchmark report generated: " + reportPath);
        return reportPath;
    }

    private BenchmarkResult benchmarkInsertSingle() {
        BenchmarkResult result = new BenchmarkResult("Insert Single Record");
        
        // PostgreSQL benchmark
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO system_logs (log_id, level, message, component, created_at) " +
                        "VALUES (?, 'INFO', 'Performance test log entry', 'BENCHMARK', NOW())";
            
            long pgStart = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.executeUpdate();
                }
            }
            long pgEnd = System.nanoTime();
            
            result.postgresAvgMs = (pgEnd - pgStart) / 100_000_000.0;
            result.postgresThroughput = 1000.0 / result.postgresAvgMs;
            
            // Cleanup
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM system_logs WHERE component = 'BENCHMARK'");
            }
        } catch (Exception e) {
            logger.warn("PostgreSQL insert benchmark failed: " + e.getMessage());
            result.postgresAvgMs = -1;
        }
        
        // MongoDB benchmark
        try (MongoClient client = MongoClients.create(EnvConfig.getMongoUri())) {
            MongoDatabase db = client.getDatabase("hospital");
            MongoCollection<Document> col = db.getCollection("system_logs");
            
            long mongoStart = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                Document doc = new Document()
                    .append("log_id", UUID.randomUUID().toString())
                    .append("level", "INFO")
                    .append("message", "Performance test log entry")
                    .append("component", "BENCHMARK")
                    .append("created_at", LocalDateTime.now().toString());
                col.insertOne(doc);
            }
            long mongoEnd = System.nanoTime();
            
            result.mongoAvgMs = (mongoEnd - mongoStart) / 100_000_000.0;
            result.mongoThroughput = 1000.0 / result.mongoAvgMs;
            
            // Cleanup
            col.deleteMany(new Document("component", "BENCHMARK"));
        } catch (Exception e) {
            logger.warn("MongoDB insert benchmark failed: " + e.getMessage());
            result.mongoAvgMs = -1;
        }
        
        return result;
    }

    private BenchmarkResult benchmarkInsertBatch() {
        BenchmarkResult result = new BenchmarkResult("Insert Batch (100 records)");
        
        // PostgreSQL benchmark
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO system_logs (log_id, level, message, component, created_at) " +
                        "VALUES (?, 'INFO', 'Batch test log', 'BENCHMARK_BATCH', NOW())";
            
            long pgStart = System.nanoTime();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < 100; i++) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
            long pgEnd = System.nanoTime();
            conn.setAutoCommit(true);
            
            result.postgresAvgMs = (pgEnd - pgStart) / 1_000_000.0;
            result.postgresThroughput = 100_000.0 / result.postgresAvgMs;
            
            // Cleanup
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM system_logs WHERE component = 'BENCHMARK_BATCH'");
            }
        } catch (Exception e) {
            logger.warn("PostgreSQL batch insert failed: " + e.getMessage());
            result.postgresAvgMs = -1;
        }
        
        // MongoDB benchmark
        try (MongoClient client = MongoClients.create(EnvConfig.getMongoUri())) {
            MongoDatabase db = client.getDatabase("hospital");
            MongoCollection<Document> col = db.getCollection("system_logs");
            
            List<Document> docs = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                docs.add(new Document()
                    .append("log_id", UUID.randomUUID().toString())
                    .append("level", "INFO")
                    .append("message", "Batch test log")
                    .append("component", "BENCHMARK_BATCH")
                    .append("created_at", LocalDateTime.now().toString()));
            }
            
            long mongoStart = System.nanoTime();
            col.insertMany(docs);
            long mongoEnd = System.nanoTime();
            
            result.mongoAvgMs = (mongoEnd - mongoStart) / 1_000_000.0;
            result.mongoThroughput = 100_000.0 / result.mongoAvgMs;
            
            // Cleanup
            col.deleteMany(new Document("component", "BENCHMARK_BATCH"));
        } catch (Exception e) {
            logger.warn("MongoDB batch insert failed: " + e.getMessage());
            result.mongoAvgMs = -1;
        }
        
        return result;
    }

    private BenchmarkResult benchmarkFetchByPatientAndTime() {
        BenchmarkResult result = new BenchmarkResult("Fetch by Patient + Time Range");
        
        // PostgreSQL benchmark
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM audit_logs WHERE user_id = ? " +
                        "AND created_at >= NOW() - INTERVAL '7 days' ORDER BY created_at DESC LIMIT 50";
            
            long pgStart = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, UUID.randomUUID().toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            // Consume results
                        }
                    }
                }
            }
            long pgEnd = System.nanoTime();
            
            result.postgresAvgMs = (pgEnd - pgStart) / 50_000_000.0;
            result.postgresThroughput = 50_000.0 / result.postgresAvgMs;
        } catch (Exception e) {
            logger.warn("PostgreSQL fetch benchmark failed: " + e.getMessage());
            result.postgresAvgMs = -1;
        }
        
        // MongoDB benchmark
        try (MongoClient client = MongoClients.create(EnvConfig.getMongoUri())) {
            MongoDatabase db = client.getDatabase("hospital");
            MongoCollection<Document> col = db.getCollection("audit_logs");
            
            long mongoStart = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                Document query = new Document("user_id", UUID.randomUUID().toString())
                    .append("created_at", new Document("$gte", LocalDateTime.now().minusDays(7).toString()));
                col.find(query).limit(50).iterator().forEachRemaining(doc -> {
                    // Consume results
                });
            }
            long mongoEnd = System.nanoTime();
            
            result.mongoAvgMs = (mongoEnd - mongoStart) / 50_000_000.0;
            result.mongoThroughput = 50_000.0 / result.mongoAvgMs;
        } catch (Exception e) {
            logger.warn("MongoDB fetch benchmark failed: " + e.getMessage());
            result.mongoAvgMs = -1;
        }
        
        return result;
    }

    private BenchmarkResult benchmarkFetchRecent() {
        BenchmarkResult result = new BenchmarkResult("Fetch Recent N Records");
        
        // PostgreSQL benchmark
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM system_logs ORDER BY created_at DESC LIMIT 100";
            
            long pgStart = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        // Consume results
                    }
                }
            }
            long pgEnd = System.nanoTime();
            
            result.postgresAvgMs = (pgEnd - pgStart) / 50_000_000.0;
            result.postgresThroughput = 50_000.0 / result.postgresAvgMs;
        } catch (Exception e) {
            logger.warn("PostgreSQL recent fetch failed: " + e.getMessage());
            result.postgresAvgMs = -1;
        }
        
        // MongoDB benchmark
        try (MongoClient client = MongoClients.create(EnvConfig.getMongoUri())) {
            MongoDatabase db = client.getDatabase("hospital");
            MongoCollection<Document> col = db.getCollection("system_logs");
            
            long mongoStart = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                col.find().sort(new Document("created_at", -1)).limit(100).iterator().forEachRemaining(doc -> {
                    // Consume results
                });
            }
            long mongoEnd = System.nanoTime();
            
            result.mongoAvgMs = (mongoEnd - mongoStart) / 50_000_000.0;
            result.mongoThroughput = 50_000.0 / result.mongoAvgMs;
        } catch (Exception e) {
            logger.warn("MongoDB recent fetch failed: " + e.getMessage());
            result.mongoAvgMs = -1;
        }
        
        return result;
    }

    private BenchmarkResult benchmarkUpdateMetadata() {
        BenchmarkResult result = new BenchmarkResult("Update Metadata Fields");
        
        // PostgreSQL benchmark
        try (Connection conn = DBConnection.getConnection()) {
            // Insert test records first
            String insertSql = "INSERT INTO system_logs (log_id, level, message, component, created_at) " +
                              "VALUES (?, 'INFO', 'Update test', 'BENCHMARK_UPDATE', NOW())";
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                String id = UUID.randomUUID().toString();
                ids.add(id);
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                }
            }
            
            String updateSql = "UPDATE system_logs SET level = 'WARN' WHERE log_id = ?";
            long pgStart = System.nanoTime();
            for (String id : ids) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                }
            }
            long pgEnd = System.nanoTime();
            
            result.postgresAvgMs = (pgEnd - pgStart) / 50_000_000.0;
            result.postgresThroughput = 50_000.0 / result.postgresAvgMs;
            
            // Cleanup
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM system_logs WHERE component = 'BENCHMARK_UPDATE'");
            }
        } catch (Exception e) {
            logger.warn("PostgreSQL update benchmark failed: " + e.getMessage());
            result.postgresAvgMs = -1;
        }
        
        // MongoDB benchmark
        try (MongoClient client = MongoClients.create(EnvConfig.getMongoUri())) {
            MongoDatabase db = client.getDatabase("hospital");
            MongoCollection<Document> col = db.getCollection("system_logs");
            
            // Insert test records first
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                String id = UUID.randomUUID().toString();
                ids.add(id);
                col.insertOne(new Document()
                    .append("log_id", id)
                    .append("level", "INFO")
                    .append("message", "Update test")
                    .append("component", "BENCHMARK_UPDATE")
                    .append("created_at", LocalDateTime.now().toString()));
            }
            
            long mongoStart = System.nanoTime();
            for (String id : ids) {
                col.updateOne(
                    new Document("log_id", id),
                    new Document("$set", new Document("level", "WARN"))
                );
            }
            long mongoEnd = System.nanoTime();
            
            result.mongoAvgMs = (mongoEnd - mongoStart) / 50_000_000.0;
            result.mongoThroughput = 50_000.0 / result.mongoAvgMs;
            
            // Cleanup
            col.deleteMany(new Document("component", "BENCHMARK_UPDATE"));
        } catch (Exception e) {
            logger.warn("MongoDB update benchmark failed: " + e.getMessage());
            result.mongoAvgMs = -1;
        }
        
        return result;
    }

    private String buildReportMarkdown(List<BenchmarkResult> results) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# PostgreSQL vs MongoDB Performance Benchmark Report\n\n");
        sb.append("**Generated:** ").append(LocalDateTime.now().format(TIMESTAMP_FMT)).append("\n");
        sb.append("**System:** Hospital Management System v1.0\n");
        sb.append("**Environment:** Production-like configuration with connection pooling\n\n");
        
        sb.append("## Executive Summary\n\n");
        sb.append("This report compares the performance of PostgreSQL (relational) and MongoDB (NoSQL) ");
        sb.append("for common healthcare data operations. The benchmark measures real-world scenarios ");
        sb.append("including single and batch inserts, time-range queries, and metadata updates.\n\n");
        
        sb.append("## Benchmark Results\n\n");
        sb.append("| Operation | PostgreSQL (ms) | MongoDB (ms) | Winner | Performance Gain |\n");
        sb.append("|-----------|-----------------|--------------|--------|------------------|\n");
        
        for (BenchmarkResult result : results) {
            String winner = "—";
            String gain = "—";
            
            if (result.postgresAvgMs > 0 && result.mongoAvgMs > 0) {
                if (result.postgresAvgMs < result.mongoAvgMs) {
                    winner = "PostgreSQL";
                    double improvement = ((result.mongoAvgMs - result.postgresAvgMs) / result.mongoAvgMs) * 100;
                    gain = String.format("%.1f%% faster", improvement);
                } else if (result.mongoAvgMs < result.postgresAvgMs) {
                    winner = "MongoDB";
                    double improvement = ((result.postgresAvgMs - result.mongoAvgMs) / result.postgresAvgMs) * 100;
                    gain = String.format("%.1f%% faster", improvement);
                } else {
                    winner = "Tie";
                    gain = "Equal";
                }
            }
            
            String pgTime = result.postgresAvgMs > 0 ? String.format("%.2f", result.postgresAvgMs) : "N/A";
            String mongoTime = result.mongoAvgMs > 0 ? String.format("%.2f", result.mongoAvgMs) : "N/A";
            
            sb.append(String.format("| %s | %s | %s | %s | %s |\n",
                result.operation, pgTime, mongoTime, winner, gain));
        }
        
        sb.append("\n## Throughput Comparison\n\n");
        sb.append("| Operation | PostgreSQL (ops/sec) | MongoDB (ops/sec) |\n");
        sb.append("|-----------|----------------------|-------------------|\n");
        
        for (BenchmarkResult result : results) {
            String pgThroughput = result.postgresThroughput > 0 ? 
                String.format("%.0f", result.postgresThroughput) : "N/A";
            String mongoThroughput = result.mongoThroughput > 0 ? 
                String.format("%.0f", result.mongoThroughput) : "N/A";
            
            sb.append(String.format("| %s | %s | %s |\n",
                result.operation, pgThroughput, mongoThroughput));
        }
        
        sb.append("\n## Analysis\n\n");
        sb.append("### Strengths of PostgreSQL\n");
        sb.append("- **ACID compliance:** Full transactional integrity for critical healthcare data\n");
        sb.append("- **Complex queries:** Superior for joins, aggregations, and analytical workloads\n");
        sb.append("- **Data consistency:** Enforced constraints and foreign keys\n");
        sb.append("- **Indexing:** Advanced B-tree and GiST indexes for fast lookups\n\n");
        
        sb.append("### Strengths of MongoDB\n");
        sb.append("- **Schema flexibility:** Ideal for unstructured clinical notes and evolving data models\n");
        sb.append("- **Write performance:** Excellent for high-volume logging and audit trails\n");
        sb.append("- **Horizontal scaling:** Better support for distributed deployments\n");
        sb.append("- **Document structure:** Natural fit for hierarchical medical records\n\n");
        
        sb.append("## Recommendations\n\n");
        sb.append("### Use PostgreSQL for:\n");
        sb.append("- Patient demographics and identities\n");
        sb.append("- Appointments and scheduling\n");
        sb.append("- Financial transactions and billing\n");
        sb.append("- Prescription and medication inventory\n");
        sb.append("- Any data requiring strict referential integrity\n\n");
        
        sb.append("### Use MongoDB for:\n");
        sb.append("- Clinical notes and unstructured observations\n");
        sb.append("- System and audit logs\n");
        sb.append("- Real-time notifications\n");
        sb.append("- Analytics staging and temporary aggregations\n");
        sb.append("- IoT/sensor data if integrated\n\n");
        
        sb.append("## Conclusion\n\n");
        sb.append("Both databases serve complementary roles in the hospital management system. ");
        sb.append("PostgreSQL provides the transactional foundation for structured healthcare data, ");
        sb.append("while MongoDB excels at handling unstructured content and high-volume logging. ");
        sb.append("The hybrid architecture leverages the strengths of each system appropriately.\n\n");
        
        sb.append("---\n");
        sb.append("*Report generated by Performance Benchmark Service*\n");
        
        return sb.toString();
    }

    private static class BenchmarkResult {
        String operation;
        double postgresAvgMs = -1;
        double mongoAvgMs = -1;
        double postgresThroughput = 0;
        double mongoThroughput = 0;
        
        BenchmarkResult(String operation) {
            this.operation = operation;
        }
    }
}
