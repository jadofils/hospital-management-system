package hospital.management.backend.service.analytics;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.config.db.TransactionManager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides runtime inspection and management of PostgreSQL database objects:
 * indexes, views, stored procedures, and functions.
 *
 * <p>Also exposes an {@link #benchmarkIndexComparison()} method that measures
 * the real-world query speed improvement conferred by GIN trigram indexes on
 * the {@code patients} table.
 *
 * <p>This class is not {@code final} so it can be extended in tests.
 */
public class DatabaseInspectionService {

    private static final AppLogger logger = AppLogger.getLogger(DatabaseInspectionService.class);

    /**
     * The single consolidated DDL script. After the SQL-script cleanup, the old
     * {@code hospital_objects.sql} / {@code hospital_indexes_postgresql.sql} split
     * no longer exists — indexes, the {@code set_updated_at()} trigger function,
     * and (if any are ever added) views all live inline in the schema file, so the
     * regenerate actions extract their statements from here.
     */
    private static final String SCHEMA_SQL_PATH = "/hospital/management/sql/hospital_schema.sql";

    // ── Public records ─────────────────────────────────────────────────────

    /**
     * A PostgreSQL index as returned by {@link #listIndexes()}.
     *
     * @param indexName the index name
     * @param tableName the table the index belongs to
     * @param indexDef  the full CREATE INDEX statement stored in pg_indexes
     * @param isUnique  true when the definition contains "UNIQUE"
     * @param isPrimary true when the name matches the primary-key naming
     *                  convention ({@code pk_*} or {@code *_pkey})
     * @param isConstraintBacked true when this index is owned by a table
     *                  {@code PRIMARY KEY} or {@code UNIQUE} constraint (per
     *                  {@code pg_constraint}, not a naming-convention guess).
     *                  Postgres refuses {@code DROP INDEX} on these — even with
     *                  {@code CASCADE} — with "cannot drop index because
     *                  constraint X requires it"; the constraint must be
     *                  dropped instead. Every {@code isPrimary} index is also
     *                  {@code isConstraintBacked}, but so are plain
     *                  {@code UNIQUE} column constraints, which don't match the
     *                  {@code pk_*}/{@code *_pkey} naming check — this is the
     *                  field that actually determines "safe to bulk-drop".
     */
    public record DbIndex(
            String indexName,
            String tableName,
            String indexDef,
            boolean isUnique,
            boolean isPrimary,
            boolean isConstraintBacked) {}

    /**
     * A PostgreSQL view as returned by {@link #listViews()}.
     *
     * @param viewName   the view name
     * @param definition the view SQL body stored in information_schema
     */
    public record DbView(
            String viewName,
            String definition) {}

    /**
     * A PostgreSQL routine (function or procedure) as returned by
     * {@link #listRoutines()}.
     *
     * @param routineName the routine name
     * @param routineType {@code "FUNCTION"} or {@code "PROCEDURE"}
     * @param language    the implementation language (e.g. {@code "plpgsql"})
     * @param definition  the routine body
     */
    public record DbRoutine(
            String routineName,
            String routineType,
            String language,
            String definition) {}

    /**
     * The result of {@link #benchmarkIndexComparison()}.
     *
     * @param withoutIndexMs average query time in ms measured WITHOUT the GIN
     *                       trigram index
     * @param withIndexMs    average query time in ms measured WITH the GIN
     *                       trigram index
     * @param speedupFactor  {@code withoutIndexMs / withIndexMs} — a value
     *                       greater than 1 indicates the index is beneficial
     */
    public record IndexBenchmarkComparison(
            double withoutIndexMs,
            double withIndexMs,
            double speedupFactor) {}

    // ── Index management ───────────────────────────────────────────────────

    /**
     * Lists all indexes in the {@code public} schema, ordered by table then
     * index name.
     *
     * @return a list of {@link DbIndex} records; never {@code null}
     * @throws Exception if the query fails
     */
    public List<DbIndex> listIndexes() throws Exception {
        String sql = """
                SELECT pi.indexname,
                       pi.tablename,
                       pi.indexdef,
                       pi.indexdef ILIKE '%unique%'                           AS is_unique,
                       (pi.indexname LIKE 'pk_%' OR pi.indexname LIKE '%_pkey') AS is_primary,
                       EXISTS (
                           SELECT 1 FROM pg_constraint c
                            JOIN pg_class idx ON idx.oid = c.conindid
                           WHERE idx.relname = pi.indexname
                             AND c.contype IN ('p', 'u')
                       )                                                       AS is_constraint_backed
                  FROM pg_indexes pi
                 WHERE pi.schemaname = 'public'
                 ORDER BY pi.tablename, pi.indexname
                """;

        List<DbIndex> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new DbIndex(
                        rs.getString("indexname"),
                        rs.getString("tablename"),
                        rs.getString("indexdef"),
                        rs.getBoolean("is_unique"),
                        rs.getBoolean("is_primary"),
                        rs.getBoolean("is_constraint_backed")));
            }
        }
        logger.info("Listed " + result.size() + " indexes.");
        return result;
    }

    /**
     * Drops the index with the given name using {@code DROP INDEX IF EXISTS}.
     *
     * @param indexName the index to drop (must not be {@code null} or blank)
     * @throws Exception if the DDL statement fails
     */
    public void dropIndex(String indexName) throws Exception {
        // Quote-escape any embedded double-quotes to prevent SQL injection.
        String safeName = indexName.replace("\"", "\"\"");
        String ddl = "DROP INDEX IF EXISTS \"" + safeName + "\"";
        logger.info("Dropping index: " + indexName);
        TransactionManager.executeInTransaction(conn -> {
            try (Statement st = conn.createStatement()) {
                st.execute(ddl);
            }
        });
        logger.info("Index dropped: " + indexName);
    }

    /**
     * Drops every named index in a single transaction (all-or-nothing): if any
     * statement fails, none of the batch is applied.
     *
     * @param indexNames the indexes to drop; a {@code null} or empty list is a no-op
     * @throws Exception if any DDL statement fails
     */
    public void dropIndexes(List<String> indexNames) throws Exception {
        if (indexNames == null || indexNames.isEmpty()) return;
        logger.info("Bulk-dropping " + indexNames.size() + " indexes: " + indexNames);
        TransactionManager.executeInTransaction(conn -> {
            try (Statement st = conn.createStatement()) {
                for (String indexName : indexNames) {
                    String safeName = indexName.replace("\"", "\"\"");
                    st.execute("DROP INDEX IF EXISTS \"" + safeName + "\"");
                }
            }
        });
        logger.info("Bulk drop complete: " + indexNames.size() + " indexes dropped.");
    }

    /**
     * Re-creates all indexes by extracting the {@code CREATE INDEX} statements
     * from {@code hospital_schema.sql} and executing them. Each statement is
     * normalized to {@code CREATE INDEX IF NOT EXISTS …} so re-running after a
     * bulk drop (or a no-op refresh) is safe.
     *
     * @throws Exception if the SQL file cannot be read or any statement fails
     */
    public void regenerateIndexes() throws Exception {
        logger.info("Regenerating all indexes from hospital_schema.sql");
        String schemaSql = readSqlFile(SCHEMA_SQL_PATH);
        List<String> stmts = statementsStartingWith(schemaSql,
                "CREATE UNIQUE INDEX", "CREATE INDEX");
        List<String> idempotent = new ArrayList<>();
        for (String stmt : stmts) idempotent.add(withIndexIfNotExists(stmt));
        executeStatements(idempotent);
        logger.info("Indexes regenerated (" + stmts.size() + " statements executed).");
    }

    // ── View management ────────────────────────────────────────────────────

    /**
     * Lists all views in the {@code public} schema, ordered by name.
     *
     * @return a list of {@link DbView} records; never {@code null}
     * @throws Exception if the query fails
     */
    public List<DbView> listViews() throws Exception {
        String sql = """
                SELECT table_name,
                       view_definition
                  FROM information_schema.views
                 WHERE table_schema = 'public'
                 ORDER BY table_name
                """;

        List<DbView> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new DbView(
                        rs.getString("table_name"),
                        rs.getString("view_definition")));
            }
        }
        logger.info("Listed " + result.size() + " views.");
        return result;
    }

    /**
     * Drops the view with the given name ({@code DROP VIEW IF EXISTS … CASCADE}).
     *
     * @param viewName the view to drop
     * @throws Exception if the DDL statement fails
     */
    public void dropView(String viewName) throws Exception {
        String safeName = viewName.replace("\"", "\"\"");
        String ddl = "DROP VIEW IF EXISTS \"" + safeName + "\" CASCADE";
        logger.info("Dropping view: " + viewName);
        TransactionManager.executeInTransaction(conn -> {
            try (Statement st = conn.createStatement()) {
                st.execute(ddl);
            }
        });
        logger.info("View dropped: " + viewName);
    }

    /**
     * Drops every named view (CASCADE) in a single transaction (all-or-nothing).
     *
     * @param viewNames the views to drop; a {@code null} or empty list is a no-op
     * @throws Exception if any DDL statement fails
     */
    public void dropViews(List<String> viewNames) throws Exception {
        if (viewNames == null || viewNames.isEmpty()) return;
        logger.info("Bulk-dropping " + viewNames.size() + " views: " + viewNames);
        TransactionManager.executeInTransaction(conn -> {
            try (Statement st = conn.createStatement()) {
                for (String viewName : viewNames) {
                    String safeName = viewName.replace("\"", "\"\"");
                    st.execute("DROP VIEW IF EXISTS \"" + safeName + "\" CASCADE");
                }
            }
        });
        logger.info("Bulk drop complete: " + viewNames.size() + " views dropped.");
    }

    /**
     * Re-creates all views by extracting the {@code CREATE VIEW} statements from
     * {@code hospital_schema.sql} and executing them. The schema currently defines
     * no views, so this is a safe no-op until one is added — it never errors out.
     *
     * @throws Exception if the SQL file cannot be read or any statement fails
     */
    public void regenerateViews() throws Exception {
        logger.info("Regenerating all views from hospital_schema.sql.");
        String schemaSql = readSqlFile(SCHEMA_SQL_PATH);
        List<String> stmts = statementsStartingWith(schemaSql,
                "CREATE OR REPLACE VIEW", "CREATE VIEW");
        executeStatements(stmts);
        logger.info("Views regenerated (" + stmts.size() + " statements executed).");
    }

    // ── Routine management ─────────────────────────────────────────────────

    /**
     * Lists all functions and procedures in the {@code public} schema, ordered
     * by type then name.
     *
     * @return a list of {@link DbRoutine} records; never {@code null}
     * @throws Exception if the query fails
     */
    public List<DbRoutine> listRoutines() throws Exception {
        String sql = """
                SELECT routine_name,
                       routine_type,
                       external_language   AS language,
                       routine_definition  AS definition
                  FROM information_schema.routines
                 WHERE routine_schema = 'public'
                   AND routine_type IN ('FUNCTION', 'PROCEDURE')
                 ORDER BY routine_type, routine_name
                """;

        List<DbRoutine> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new DbRoutine(
                        rs.getString("routine_name"),
                        rs.getString("routine_type"),
                        rs.getString("language"),
                        rs.getString("definition")));
            }
        }
        logger.info("Listed " + result.size() + " routines.");
        return result;
    }

    /**
     * Drops the named function or procedure
     * ({@code DROP FUNCTION/PROCEDURE IF EXISTS … CASCADE}).
     *
     * @param name the routine name
     * @param type {@code "FUNCTION"} or {@code "PROCEDURE"} (case-insensitive)
     * @throws Exception if the DDL statement fails
     */
    public void dropRoutine(String name, String type) throws Exception {
        String safeName = name.replace("\"", "\"\"");
        String keyword  = "PROCEDURE".equalsIgnoreCase(type) ? "PROCEDURE" : "FUNCTION";
        String ddl      = "DROP " + keyword + " IF EXISTS \"" + safeName + "\" CASCADE";
        logger.info("Dropping " + keyword + ": " + name);
        TransactionManager.executeInTransaction(conn -> {
            try (Statement st = conn.createStatement()) {
                st.execute(ddl);
            }
        });
        logger.info(keyword + " dropped: " + name);
    }

    /**
     * Drops every named routine (CASCADE) in a single transaction (all-or-nothing).
     * Takes the full {@link DbRoutine} record (not bare names) because dropping
     * requires knowing FUNCTION vs PROCEDURE per item.
     *
     * @param routines the routines to drop; a {@code null} or empty list is a no-op
     * @throws Exception if any DDL statement fails
     */
    public void dropRoutines(List<DbRoutine> routines) throws Exception {
        if (routines == null || routines.isEmpty()) return;
        logger.info("Bulk-dropping " + routines.size() + " routines.");
        TransactionManager.executeInTransaction(conn -> {
            try (Statement st = conn.createStatement()) {
                for (DbRoutine routine : routines) {
                    String safeName = routine.routineName().replace("\"", "\"\"");
                    String keyword  = "PROCEDURE".equalsIgnoreCase(routine.routineType()) ? "PROCEDURE" : "FUNCTION";
                    st.execute("DROP " + keyword + " IF EXISTS \"" + safeName + "\" CASCADE");
                }
            }
        });
        logger.info("Bulk drop complete: " + routines.size() + " routines dropped.");
    }

    /**
     * Re-creates all stored functions and procedures by extracting the
     * {@code CREATE [OR REPLACE] FUNCTION/PROCEDURE} statements from
     * {@code hospital_schema.sql} and executing them. The only routine today is the
     * {@code set_updated_at()} trigger function; its {@code CREATE OR REPLACE} form
     * makes this idempotent.
     *
     * @throws Exception if the file cannot be read or any statement fails
     */
    public void regenerateRoutines() throws Exception {
        logger.info("Regenerating all routines from hospital_schema.sql.");
        String schemaSql = readSqlFile(SCHEMA_SQL_PATH);
        List<String> stmts = statementsStartingWith(schemaSql,
                "CREATE OR REPLACE FUNCTION", "CREATE FUNCTION",
                "CREATE OR REPLACE PROCEDURE", "CREATE PROCEDURE");
        executeStatements(stmts);
        logger.info("Routines regenerated (" + stmts.size() + " statements executed).");
    }

    // ── Index benchmark ────────────────────────────────────────────────────

    /**
     * Benchmarks the effect of GIN trigram indexes on patient name search.
     *
     * <ol>
     *   <li>Runs 50 iterations of an ILIKE search WITH the existing index in
     *       place and records the average time.</li>
     *   <li>Drops the {@code trgm_patients_name} and {@code trgm_doctors_name}
     *       GIN indexes.</li>
     *   <li>Runs the same 50 iterations WITHOUT an index and records the
     *       average time.</li>
     *   <li>Recreates both GIN indexes.</li>
     * </ol>
     *
     * @return an {@link IndexBenchmarkComparison} with the measured times and
     *         the computed speedup factor
     * @throws Exception if any SQL operation fails
     */
    public IndexBenchmarkComparison benchmarkIndexComparison() throws Exception {
        final String searchSql =
                "SELECT patient_id, first_name, last_name " +
                "FROM patients " +
                "WHERE (first_name ILIKE ? OR last_name ILIKE ?) " +
                "  AND deleted_at IS NULL " +
                "LIMIT 20";

        logger.info("Starting index benchmark comparison (with-index phase).");

        // Phase 1: measure WITH index
        double withIndexMs = runSearchIterations(searchSql, 50);
        logger.info("With-index avg: " + withIndexMs + " ms");

        // Phase 2: drop GIN trigram indexes — DDL must run outside a transaction
        // (PostgreSQL DDL is transactional but HikariCP hands us an autocommit
        //  connection by default, so a plain Statement suffices here).
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            conn.setAutoCommit(true);
            st.execute("DROP INDEX IF EXISTS trgm_patients_name");
            st.execute("DROP INDEX IF EXISTS trgm_doctors_name");
        }
        logger.info("GIN trigram indexes dropped for benchmark.");

        // Phase 3: measure WITHOUT index
        double withoutIndexMs = runSearchIterations(searchSql, 50);
        logger.info("Without-index avg: " + withoutIndexMs + " ms");

        // Phase 4: recreate GIN trigram indexes
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            conn.setAutoCommit(true);
            st.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            st.execute(
                "CREATE INDEX IF NOT EXISTS trgm_patients_name " +
                "ON patients USING gin((first_name || ' ' || last_name) gin_trgm_ops) " +
                "WHERE deleted_at IS NULL");
            st.execute(
                "CREATE INDEX IF NOT EXISTS trgm_doctors_name " +
                "ON doctors USING gin((first_name || ' ' || last_name) gin_trgm_ops) " +
                "WHERE deleted_at IS NULL");
        }
        logger.info("GIN trigram indexes recreated.");

        double speedup = withIndexMs > 0 ? withoutIndexMs / withIndexMs : 0.0;
        logger.info(String.format(
                "Index benchmark complete — withIndex=%.3f ms, withoutIndex=%.3f ms, speedup=%.2fx",
                withIndexMs, withoutIndexMs, speedup));

        return new IndexBenchmarkComparison(withoutIndexMs, withIndexMs, speedup);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /**
     * Runs {@code iterations} prepared-statement executions of {@code sql}
     * (binding {@code "%a%"} to parameters 1 and 2) and returns the average
     * elapsed time in milliseconds.
     */
    private double runSearchIterations(String sql, int iterations) throws Exception {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Warm-up: one run before timing
            ps.setString(1, "%a%");
            ps.setString(2, "%a%");
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { /* drain */ } }

            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                ps.setString(1, "%a%");
                ps.setString(2, "%a%");
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { /* drain */ } }
            }
            long elapsed = System.nanoTime() - start;
            return elapsed / (iterations * 1_000_000.0);
        }
    }

    /**
     * Returns the statements from {@code sql} whose trimmed, upper-cased text
     * starts with any of the given prefixes (e.g. {@code "CREATE INDEX"}). Used
     * by the regenerate actions to replay just one object kind from the
     * consolidated schema file without executing table/trigger/transaction
     * statements that would conflict with the live schema.
     */
    private List<String> statementsStartingWith(String sql, String... prefixes) {
        List<String> result = new ArrayList<>();
        for (String stmt : splitSqlStatements(sql)) {
            String upper = stmt.trim().toUpperCase();
            for (String prefix : prefixes) {
                if (upper.startsWith(prefix)) {
                    result.add(stmt);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Injects {@code IF NOT EXISTS} into a {@code CREATE INDEX} statement (e.g.
     * {@code CREATE INDEX idx_x …} → {@code CREATE INDEX IF NOT EXISTS idx_x …})
     * so re-running regeneration over already-present indexes is a no-op rather
     * than a "relation already exists" failure.
     */
    private static String withIndexIfNotExists(String stmt) {
        String upper = stmt.trim().toUpperCase();
        for (String prefix : List.of("CREATE UNIQUE INDEX ", "CREATE INDEX ")) {
            if (upper.startsWith(prefix)) {
                return stmt.trim().substring(0, prefix.length())
                        + "IF NOT EXISTS "
                        + stmt.trim().substring(prefix.length());
            }
        }
        return stmt.trim();
    }

    /**
     * Executes each statement in a single database transaction via
     * {@link TransactionManager}.
     *
     * @param stmts the SQL statements to execute; empty list is a no-op
     * @throws Exception if any statement fails (the transaction is rolled back)
     */
    private void executeStatements(List<String> stmts) throws Exception {
        if (stmts.isEmpty()) return;
        TransactionManager.executeInTransaction(conn -> {
            try (Statement st = conn.createStatement()) {
                for (String s : stmts) {
                    logger.debug("Executing: " + s.substring(0, Math.min(80, s.length())).trim() + "…");
                    st.execute(s);
                }
            }
        });
    }

    /**
     * Reads a classpath resource as a UTF-8 string.
     *
     * @param classpathPath the absolute classpath path (e.g.
     *                      {@code "/hospital/management/sql/hospital_schema.sql"})
     * @return the file contents
     * @throws Exception if the resource cannot be found or read
     */
    private String readSqlFile(String classpathPath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(classpathPath)) {
            if (is == null) {
                throw new IllegalArgumentException(
                        "SQL file not found on classpath: " + classpathPath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Splits a SQL text into individual executable statements.
     *
     * <p>The splitter handles:
     * <ul>
     *   <li>Line comments ({@code --} to end of line) — stripped before
     *       processing.</li>
     *   <li>Dollar-quoted blocks ({@code $tag$...$tag$}) — the opening tag is
     *       detected and everything until the matching closing tag is treated as
     *       a single token, preserving semicolons inside procedure bodies.</li>
     *   <li>Semicolon-delimited statement boundaries.</li>
     *   <li>Empty or whitespace-only fragments — silently skipped.</li>
     * </ul>
     *
     * @param sql the full SQL text (may be multi-statement)
     * @return the individual statements, in order, without trailing semicolons
     */
    static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current   = new StringBuilder();
        int i = 0;
        int len = sql.length();

        while (i < len) {
            char c = sql.charAt(i);

            // ── Line comment ─────────────────────────────────────────────
            if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
                // Skip everything until end of line
                while (i < len && sql.charAt(i) != '\n') i++;
                current.append('\n');
                continue;
            }

            // ── Dollar-quote block ────────────────────────────────────────
            if (c == '$') {
                // Detect the closing $ of a dollar-quote tag: $tag$ or $$
                int tagEnd = sql.indexOf('$', i + 1);
                if (tagEnd >= 0) {
                    String tag = sql.substring(i, tagEnd + 1); // e.g. "$body$" or "$$"
                    // Only treat this as a dollar-quote if the tag is a valid
                    // identifier (letters, digits, underscore, or empty).
                    String inner = tag.substring(1, tag.length() - 1);
                    boolean validTag = inner.isEmpty() ||
                            inner.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '_');

                    if (validTag) {
                        // Find the matching closing tag
                        int closeIdx = sql.indexOf(tag, tagEnd + 1);
                        if (closeIdx >= 0) {
                            // Append the entire dollar-quoted block (including tags)
                            int blockEnd = closeIdx + tag.length();
                            current.append(sql, i, blockEnd);
                            i = blockEnd;
                            continue;
                        }
                    }
                }
            }

            // ── Statement boundary ────────────────────────────────────────
            if (c == ';') {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty() && !isOnlyComment(stmt)) {
                    statements.add(stmt);
                }
                current.setLength(0);
                i++;
                continue;
            }

            current.append(c);
            i++;
        }

        // Handle any trailing statement without a terminating semicolon
        String trailing = current.toString().trim();
        if (!trailing.isEmpty() && !isOnlyComment(trailing)) {
            statements.add(trailing);
        }

        return statements;
    }

    /**
     * Returns {@code true} when {@code s} contains only whitespace and/or
     * SQL line-comment text ({@code --}).
     */
    private static boolean isOnlyComment(String s) {
        for (String line : s.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                return false;
            }
        }
        return true;
    }
}