package hospital.management.backend.service.analytics;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.mongo.benchmark.MongoBenchmarkService;
import hospital.management.backend.mongo.benchmark.MongoNotificationBenchmarkService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Generates performance benchmark reports comparing PostgreSQL and MongoDB.
 * Each PG benchmark collects cold (first-run) and warm (repeated-run) samples
 * to show before/after improvement from JIT compilation and buffer-cache priming.
 */
public class PerformanceBenchmarkService {

    private static final AppLogger logger = AppLogger.getLogger(PerformanceBenchmarkService.class);
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Warmup iterations (discarded). */
    private static final int WARMUP  = 3;
    /** Cold-run sample count (before). */
    private static final int COLD    = 20;
    /** Warm-run sample count (after). */
    private static final int WARM    = 20;
    /** Batch-benchmark run count (each run = 100 records). */
    private static final int BATCH_N = 5;

    /** Generates a Markdown benchmark report (backward-compatible entry point). */
    public Path generateBenchmarkReport() throws Exception {
        logger.info("Starting performance benchmark");
        Path p = generateReport(ReportFormat.MARKDOWN);
        logger.info("Performance benchmark report generated: " + p);
        return p;
    }

    // ── PG benchmarks ─────────────────────────────────────────────────────────

    private BenchmarkResult benchmarkInsertSingle() {
        BenchmarkResult result = new BenchmarkResult("[PG] Insert Single Record");
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO system_logs (log_level, source, message) " +
                         "VALUES ('INFO', 'BENCHMARK', 'Performance test log entry')";
            for (int i = 0; i < WARMUP; i++) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }
            }
            long[] cold = new long[COLD];
            for (int i = 0; i < COLD; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }
                cold[i] = System.nanoTime() - s;
            }
            long[] warm = new long[WARM];
            for (int i = 0; i < WARM; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }
                warm[i] = System.nanoTime() - s;
            }
            result.setFromSamples(cold, warm);
            try (Statement st = conn.createStatement()) {
                st.execute("DELETE FROM system_logs WHERE source = 'BENCHMARK'");
            }
        } catch (Exception e) { logger.warn("Insert single benchmark failed: " + e.getMessage()); }
        return result;
    }

    private BenchmarkResult benchmarkInsertBatch() {
        BenchmarkResult result = new BenchmarkResult("[PG] Insert Batch (100 records)");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO system_logs (log_level, source, message) " +
                         "VALUES ('INFO', 'BENCHMARK_BATCH', 'Batch test log')";
            // Warmup: 1 batch
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int j = 0; j < 100; j++) ps.addBatch();
                ps.executeBatch();
            }
            conn.commit();
            long[] cold = new long[BATCH_N];
            for (int i = 0; i < BATCH_N; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int j = 0; j < 100; j++) ps.addBatch();
                    ps.executeBatch();
                }
                conn.commit();
                cold[i] = System.nanoTime() - s;
            }
            long[] warm = new long[BATCH_N];
            for (int i = 0; i < BATCH_N; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int j = 0; j < 100; j++) ps.addBatch();
                    ps.executeBatch();
                }
                conn.commit();
                warm[i] = System.nanoTime() - s;
            }
            conn.setAutoCommit(true);
            result.setFromSamples(cold, warm);
            try (Statement st = conn.createStatement()) {
                st.execute("DELETE FROM system_logs WHERE source = 'BENCHMARK_BATCH'");
            }
        } catch (Exception e) {
            logger.warn("Batch insert benchmark failed: " + e.getMessage());
            try (Connection conn = DBConnection.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute("DELETE FROM system_logs WHERE source = 'BENCHMARK_BATCH'");
            } catch (Exception ignore) {}
        }
        return result;
    }

    private BenchmarkResult benchmarkFetchByUserAndTime() {
        BenchmarkResult result = new BenchmarkResult("[PG] Fetch by User + Time Range");
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM audit_log WHERE user_id = ? " +
                         "AND created_at >= NOW() - INTERVAL '7 days' ORDER BY created_at DESC LIMIT 50";
            for (int i = 0; i < WARMUP; i++) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setObject(1, UUID.randomUUID());
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {} }
                }
            }
            long[] cold = new long[COLD];
            for (int i = 0; i < COLD; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setObject(1, UUID.randomUUID());
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {} }
                }
                cold[i] = System.nanoTime() - s;
            }
            long[] warm = new long[WARM];
            for (int i = 0; i < WARM; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setObject(1, UUID.randomUUID());
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {} }
                }
                warm[i] = System.nanoTime() - s;
            }
            result.setFromSamples(cold, warm);
        } catch (Exception e) { logger.warn("Fetch by user+time benchmark failed: " + e.getMessage()); }
        return result;
    }

    private BenchmarkResult benchmarkFetchRecent() {
        BenchmarkResult result = new BenchmarkResult("[PG] Fetch Recent 100 Records");
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM system_logs ORDER BY created_at DESC LIMIT 100";
            for (int i = 0; i < WARMUP; i++) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) { while (rs.next()) {} }
            }
            long[] cold = new long[COLD];
            for (int i = 0; i < COLD; i++) {
                long s = System.nanoTime();
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) { while (rs.next()) {} }
                cold[i] = System.nanoTime() - s;
            }
            long[] warm = new long[WARM];
            for (int i = 0; i < WARM; i++) {
                long s = System.nanoTime();
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) { while (rs.next()) {} }
                warm[i] = System.nanoTime() - s;
            }
            result.setFromSamples(cold, warm);
        } catch (Exception e) { logger.warn("Fetch recent benchmark failed: " + e.getMessage()); }
        return result;
    }

    private BenchmarkResult benchmarkUpdateField() {
        BenchmarkResult result = new BenchmarkResult("[PG] Update Field");
        try (Connection conn = DBConnection.getConnection()) {
            String insertSql = "INSERT INTO system_logs (log_level, source, message) " +
                               "VALUES ('INFO', 'BENCHMARK_UPDATE', 'Update test') RETURNING log_id";
            int n = COLD + WARM;
            List<UUID> ids = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                try (PreparedStatement ps = conn.prepareStatement(insertSql);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) ids.add(rs.getObject("log_id", UUID.class));
                }
            }
            String updateSql = "UPDATE system_logs SET log_level = 'WARNING' WHERE log_id = ?";
            long[] cold = new long[COLD];
            for (int i = 0; i < COLD; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setObject(1, ids.get(i)); ps.executeUpdate();
                }
                cold[i] = System.nanoTime() - s;
            }
            long[] warm = new long[WARM];
            for (int i = 0; i < WARM; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setObject(1, ids.get(COLD + i)); ps.executeUpdate();
                }
                warm[i] = System.nanoTime() - s;
            }
            result.setFromSamples(cold, warm);
            try (Statement st = conn.createStatement()) {
                st.execute("DELETE FROM system_logs WHERE source = 'BENCHMARK_UPDATE'");
            }
        } catch (Exception e) { logger.warn("Update benchmark failed: " + e.getMessage()); }
        return result;
    }

    private BenchmarkResult benchmarkPatientNameSearch() {
        BenchmarkResult result = new BenchmarkResult("[PG] Patient Name ILIKE Search");
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT patient_id, first_name, last_name FROM patients " +
                         "WHERE (first_name ILIKE ? OR last_name ILIKE ?) AND deleted_at IS NULL LIMIT 20";
            for (int i = 0; i < WARMUP; i++) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "%a%"); ps.setString(2, "%a%");
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {} }
                }
            }
            long[] cold = new long[COLD];
            for (int i = 0; i < COLD; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "%a%"); ps.setString(2, "%a%");
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {} }
                }
                cold[i] = System.nanoTime() - s;
            }
            long[] warm = new long[WARM];
            for (int i = 0; i < WARM; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "%a%"); ps.setString(2, "%a%");
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {} }
                }
                warm[i] = System.nanoTime() - s;
            }
            result.setFromSamples(cold, warm);
        } catch (Exception e) { logger.warn("Patient name search benchmark failed: " + e.getMessage()); }
        return result;
    }

    private BenchmarkResult benchmarkNotificationInsert() {
        BenchmarkResult result = new BenchmarkResult("[PG] Notification Insert JSONB");
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO notifications " +
                "(type, actor_user_id, recipients, payload, channels, status, priority) " +
                "VALUES ('BENCHMARK', NULL, '[]'::jsonb, '{\"detail\":\"benchmark\"}'::jsonb, " +
                "'[\"in_app\"]'::jsonb, '{\"in_app\":\"pending\"}'::jsonb, 'normal')";
            for (int i = 0; i < WARMUP; i++) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }
            }
            long[] cold = new long[COLD];
            for (int i = 0; i < COLD; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }
                cold[i] = System.nanoTime() - s;
            }
            long[] warm = new long[WARM];
            for (int i = 0; i < WARM; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.executeUpdate(); }
                warm[i] = System.nanoTime() - s;
            }
            result.setFromSamples(cold, warm);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM notifications WHERE type = 'BENCHMARK'")) {
                ps.executeUpdate();
            }
        } catch (Exception e) { logger.warn("Notification insert benchmark failed: " + e.getMessage()); }
        return result;
    }

    private BenchmarkResult benchmarkNotificationFetch() {
        BenchmarkResult result = new BenchmarkResult("[PG] Notification Fetch JSONB @>");
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT notification_id, type, created_at FROM notifications " +
                "WHERE recipients @> jsonb_build_array(?::text) AND deleted_at IS NULL " +
                "ORDER BY created_at DESC LIMIT 20";
            for (int i = 0; i < WARMUP; i++) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, UUID.randomUUID().toString());
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {} }
                }
            }
            long[] cold = new long[COLD];
            for (int i = 0; i < COLD; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, UUID.randomUUID().toString());
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {} }
                }
                cold[i] = System.nanoTime() - s;
            }
            long[] warm = new long[WARM];
            for (int i = 0; i < WARM; i++) {
                long s = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, UUID.randomUUID().toString());
                    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {} }
                }
                warm[i] = System.nanoTime() - s;
            }
            result.setFromSamples(cold, warm);
        } catch (Exception e) { logger.warn("Notification fetch benchmark failed: " + e.getMessage()); }
        return result;
    }

    // ── Report generation ─────────────────────────────────────────────────────

    public enum ReportFormat { MARKDOWN, CSV, PDF }

    /** Generates a report in the requested format and returns its temp path. Runs benchmarks first. */
    public Path generateReport(ReportFormat format) throws Exception {
        return generateReportFromResults(runBenchmarks(), format);
    }

    /** Generates a report from pre-computed results — avoids a second benchmark run. */
    public Path generateReportFromResults(List<BenchmarkResult> results, ReportFormat format) throws Exception {
        return generateReportFromResults(results, List.of(), format);
    }

    /** Same as {@link #generateReportFromResults(List, ReportFormat)}, but also includes a
     *  Developer Activity Log section — every other action on the Developer Dashboard
     *  (regenerate/drop indexes-views-routines, backups, test runs, etc.) besides the
     *  benchmark itself, so the downloaded report reflects the whole session, not just
     *  the benchmark table. */
    public Path generateReportFromResults(List<BenchmarkResult> results, List<String> activityLog, ReportFormat format) throws Exception {
        return switch (format) {
            case MARKDOWN -> writeTemp(buildReportMarkdown(results) + buildActivityLogMarkdown(activityLog), "performance_benchmark_", ".md");
            case CSV      -> writeTemp(buildReportCsv(results) + buildActivityLogCsv(activityLog), "performance_benchmark_", ".csv");
            case PDF      -> buildReportPdf(results, activityLog);
        };
    }

    private String buildActivityLogMarkdown(List<String> activityLog) {
        if (activityLog == null || activityLog.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n## Developer Activity Log\n\n");
        for (String entry : activityLog) {
            sb.append("- ").append(entry).append("\n");
        }
        return sb.toString();
    }

    private String buildActivityLogCsv(List<String> activityLog) {
        if (activityLog == null || activityLog.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\nDeveloper Activity Log\nEntry\n");
        for (String entry : activityLog) {
            sb.append('"').append(entry.replace("\"", "\"\"")).append("\"\n");
        }
        return sb.toString();
    }

    private Path writeTemp(String content, String prefix, String suffix) throws Exception {
        Path p = Files.createTempFile(prefix, suffix);
        Files.writeString(p, content);
        return p;
    }

    private String buildReportMarkdown(List<BenchmarkResult> results) {
        String ts = LocalDateTime.now().format(TIMESTAMP_FMT);
        StringBuilder sb = new StringBuilder();
        sb.append("# Performance Benchmark Report — PostgreSQL vs MongoDB\n\n");
        sb.append("**Generated:** ").append(ts).append("\n");
        sb.append("**System:** Hospital Management System\n\n");
        sb.append("> Cold = first ").append(COLD).append(" iterations  ·  ");
        sb.append("Warm = second ").append(WARM).append(" iterations (JIT + buffer-cache primed)\n\n");
        sb.append("## Results\n\n");
        sb.append("| Operation | Store | Avg Before (ms) | Avg After (ms) | P95 Before (ms) | P95 After (ms) | Tput Before (ops/s) | Tput After (ops/s) | Improvement % |\n");
        sb.append("|-----------|-------|-----------------|----------------|-----------------|----------------|---------------------|--------------------|---------------|\n");
        for (BenchmarkResult r : results) {
            sb.append(String.format("| %s | %s | %s | %s | %s | %s | %s | %s | %s |\n",
                r.operation, r.store,
                fmt(r.beforeAvgMs), fmt(r.afterAvgMs),
                fmt(r.beforeP95Ms), fmt(r.afterP95Ms),
                fmtTput(r.beforeThroughput), fmtTput(r.afterThroughput),
                fmtPct(r.improvementPct)));
        }
        sb.append("\n---\n*Generated by Performance Benchmark Service*\n");
        return sb.toString();
    }

    private String buildReportCsv(List<BenchmarkResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Operation,Store,Avg_Before_ms,Avg_After_ms,P95_Before_ms,P95_After_ms,Tput_Before_ops_s,Tput_After_ops_s,Improvement_pct\n");
        for (BenchmarkResult r : results) {
            sb.append(String.format("\"%s\",%s,%s,%s,%s,%s,%s,%s,%s\n",
                r.operation, r.store,
                fmt(r.beforeAvgMs), fmt(r.afterAvgMs),
                fmt(r.beforeP95Ms), fmt(r.afterP95Ms),
                fmtTput(r.beforeThroughput), fmtTput(r.afterThroughput),
                fmtPct(r.improvementPct)));
        }
        return sb.toString();
    }

    private Path buildReportPdf(List<BenchmarkResult> results, List<String> activityLog) throws Exception {
        String ts = LocalDateTime.now().format(TIMESTAMP_FMT);
        StringBuilder body = new StringBuilder();
        body.append("Performance Benchmark Report -- PostgreSQL vs MongoDB\n");
        body.append("Generated: ").append(ts).append("\n");
        body.append("System: Hospital Management System\n\n");
        body.append(String.format("%-48s  %-12s  %8s  %8s  %8s  %8s  %11s  %11s  %8s%n",
            "Operation", "Store", "AvgBef", "AvgAft", "P95Bef", "P95Aft", "TputBef", "TputAft", "Improv%"));
        body.append("-".repeat(136)).append("\n");
        for (BenchmarkResult r : results) {
            String op = r.operation.length() > 48 ? r.operation.substring(0, 45) + "..." : r.operation;
            body.append(String.format("%-48s  %-12s  %8s  %8s  %8s  %8s  %11s  %11s  %8s%n",
                op, r.store,
                fmt(r.beforeAvgMs), fmt(r.afterAvgMs),
                fmt(r.beforeP95Ms), fmt(r.afterP95Ms),
                fmtTput(r.beforeThroughput), fmtTput(r.afterThroughput),
                fmtPct(r.improvementPct)));
        }
        body.append("\nCold = first ").append(COLD)
            .append(" iterations  ·  Warm = second ").append(WARM)
            .append(" iterations\n* Generated by Performance Benchmark Service *\n");

        if (activityLog != null && !activityLog.isEmpty()) {
            body.append("\nDeveloper Activity Log\n");
            body.append("-".repeat(40)).append("\n");
            // This hand-rolled single-page PDF has no pagination, so cap the log to
            // whatever fits without running text off the bottom of the page.
            int max = Math.min(activityLog.size(), 15);
            for (int i = 0; i < max; i++) {
                body.append("- ").append(activityLog.get(i)).append("\n");
            }
            if (activityLog.size() > max) {
                body.append("... and ").append(activityLog.size() - max).append(" more (see Markdown/CSV export for the full log)\n");
            }
        }

        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 8 Tf\n30 750 Td\n10 TL\n");
        for (String line : body.toString().split("\n")) {
            String escaped = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            stream.append("(").append(escaped).append(") Tj\nT*\n");
        }
        stream.append("ET\n");

        byte[] streamBytes = stream.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        String fontObj = "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n";
        byte[] fontBytes = fontObj.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

        Path p = Files.createTempFile("performance_benchmark_", ".pdf");
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        baos.write(("%PDF-1.4\n").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        baos.write(("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
            .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        baos.write(("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
            .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        baos.write(("3 0 obj\n<< /Type /Page /Parent 2 0 R "
            + "/MediaBox [0 0 842 595] /Contents 4 0 R "
            + "/Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n")
            .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        baos.write(("4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n")
            .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        baos.write(streamBytes);
        baos.write("\nendstream\nendobj\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        baos.write(fontBytes);
        int startxref = baos.size();
        baos.write(("xref\n0 6\n0000000000 65535 f \n").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        baos.write(("trailer\n<< /Size 6 /Root 1 0 R >>\n").getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        baos.write(("startxref\n" + startxref + "\n%%EOF\n")
            .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        Files.write(p, baos.toByteArray());
        return p;
    }

    private String fmt(double ms)       { return ms < 0 ? "N/A" : String.format("%.2f", ms); }
    private String fmtTput(double tput) { return tput <= 0 ? "N/A" : String.format("%,.0f", tput); }
    private String fmtPct(double pct)   { return String.format("%.1f%%", pct); }

    /** Returns benchmark results without writing a file. Used by the Developer Dashboard. */
    public List<BenchmarkResult> runBenchmarks() {
        List<BenchmarkResult> results = new ArrayList<>();

        // PostgreSQL — core operations
        results.add(benchmarkInsertSingle());
        results.add(benchmarkInsertBatch());
        results.add(benchmarkFetchByUserAndTime());
        results.add(benchmarkFetchRecent());
        results.add(benchmarkUpdateField());
        results.add(benchmarkPatientNameSearch());

        // PostgreSQL — notifications (JSONB)
        results.add(benchmarkNotificationInsert());
        results.add(benchmarkNotificationFetch());

        // MongoDB — logs: run twice (cold then warm) to get before/after comparison
        MongoBenchmarkService mongoService = new MongoBenchmarkService();
        List<MongoBenchmarkService.BenchmarkResult> mongoCold = mongoService.runAll();
        List<MongoBenchmarkService.BenchmarkResult> mongoWarm = mongoService.runAll();
        for (int i = 0; i < mongoCold.size(); i++) {
            BenchmarkResult r = new BenchmarkResult(mongoCold.get(i).operation);
            r.setFromRunPair(mongoCold.get(i).avgMs, mongoWarm.get(i).avgMs);
            results.add(r);
        }

        // MongoDB — notifications: same two-run approach
        MongoNotificationBenchmarkService mongoNotifService = new MongoNotificationBenchmarkService();
        List<MongoNotificationBenchmarkService.BenchmarkResult> notifCold = mongoNotifService.runAll();
        List<MongoNotificationBenchmarkService.BenchmarkResult> notifWarm = mongoNotifService.runAll();
        for (int i = 0; i < notifCold.size(); i++) {
            BenchmarkResult r = new BenchmarkResult(notifCold.get(i).operation);
            r.setFromRunPair(notifCold.get(i).avgMs, notifWarm.get(i).avgMs);
            results.add(r);
        }

        return results;
    }

    // ── Result model ──────────────────────────────────────────────────────────

    public static class BenchmarkResult {
        public final String operation;
        public final String store;
        /** Backward-compatible: equals afterAvgMs once setFromSamples/setFromRunPair is called. */
        public double avgMs      = -1;
        public double throughput =  0;
        // Comparison fields
        public double beforeAvgMs      = -1;
        public double afterAvgMs       = -1;
        public double beforeP95Ms      = -1;
        public double afterP95Ms       = -1;
        public double beforeThroughput =  0;
        public double afterThroughput  =  0;
        public double improvementPct   =  0;

        BenchmarkResult(String operation) {
            this.operation = operation;
            this.store     = extractStore(operation);
        }

        BenchmarkResult(String operation, double avgMs, double throughput) {
            this.operation  = operation;
            this.store      = extractStore(operation);
            this.avgMs      = avgMs;
            this.throughput = throughput;
            // Single-run construction: before == after
            this.beforeAvgMs = avgMs;
            this.afterAvgMs  = avgMs;
            this.beforeP95Ms = avgMs > 0 ? avgMs * 1.3 : -1;
            this.afterP95Ms  = avgMs > 0 ? avgMs * 1.3 : -1;
            this.beforeThroughput = throughput;
            this.afterThroughput  = throughput;
        }

        /** Populate comparison stats from raw nanosecond sample arrays. */
        void setFromSamples(long[] coldNs, long[] warmNs) {
            if (coldNs == null || coldNs.length == 0 || warmNs == null || warmNs.length == 0) return;
            beforeAvgMs      = avgNs(coldNs) / 1_000_000.0;
            afterAvgMs       = avgNs(warmNs) / 1_000_000.0;
            beforeP95Ms      = p95Ns(coldNs) / 1_000_000.0;
            afterP95Ms       = p95Ns(warmNs) / 1_000_000.0;
            beforeThroughput = beforeAvgMs > 0 ? 1000.0 / beforeAvgMs : 0;
            afterThroughput  = afterAvgMs  > 0 ? 1000.0 / afterAvgMs  : 0;
            avgMs      = afterAvgMs;
            throughput = afterThroughput;
            improvementPct = beforeAvgMs > 0
                ? (beforeAvgMs - afterAvgMs) / beforeAvgMs * 100 : 0;
        }

        /** Populate from a pair of pre-aggregated averages (MongoDB sub-service results). */
        void setFromRunPair(double coldAvgMs, double warmAvgMs) {
            beforeAvgMs      = coldAvgMs < 0 ? -1 : coldAvgMs;
            afterAvgMs       = warmAvgMs < 0 ? -1 : warmAvgMs;
            // No per-sample data from MongoDB sub-services; estimate P95 ≈ 1.3× avg
            beforeP95Ms      = beforeAvgMs > 0 ? beforeAvgMs * 1.3 : -1;
            afterP95Ms       = afterAvgMs  > 0 ? afterAvgMs  * 1.3 : -1;
            beforeThroughput = beforeAvgMs > 0 ? 1000.0 / beforeAvgMs : 0;
            afterThroughput  = afterAvgMs  > 0 ? 1000.0 / afterAvgMs  : 0;
            avgMs      = afterAvgMs;
            throughput = afterThroughput;
            improvementPct = beforeAvgMs > 0 && afterAvgMs >= 0
                ? (beforeAvgMs - afterAvgMs) / beforeAvgMs * 100 : 0;
        }

        private static double avgNs(long[] ns) {
            long sum = 0;
            for (long n : ns) sum += n;
            return (double) sum / ns.length;
        }

        private static double p95Ns(long[] ns) {
            long[] sorted = ns.clone();
            Arrays.sort(sorted);
            int idx = (int) Math.ceil(0.95 * sorted.length) - 1;
            return sorted[Math.max(0, idx)];
        }

        private static String extractStore(String op) {
            if (op == null)                     return "—";
            if (op.startsWith("[PG]"))          return "PostgreSQL";
            if (op.startsWith("[Mongo-Notif]")) return "MongoDB";
            if (op.startsWith("[Mongo"))        return "MongoDB";
            return "—";
        }
    }
}