package hospital.management.backend.service.backup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoDatabase;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.daemon.BackupPolicy;
import hospital.management.backend.daemon.BackupType;
import hospital.management.backend.mongo.config.MongoConfig;
import org.bson.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Data-only backup/restore-source generator, pure Java/JDBC (no {@code pg_dump}
 * / {@code mongodump} dependency). Schema stays version-controlled in
 * {@code hospital_schema.sql} (tables, constraints, indexes, trigger functions),
 * which {@link hospital.management.backend.service.analytics.DatabaseInspectionService}
 * already knows how to replay — this class only carries data that would
 * otherwise be lost.
 *
 * <p>Output layout: one directory per run under {@code ~/.hms/backups/},
 * named {@code <yyyy-MM-dd_HHmmss>_<full|partial>}, containing one
 * {@code .jsonl} file per table/collection (one JSON object per line) plus a
 * {@code manifest.json} describing the run.
 */
public class BackupService {

    private static final AppLogger logger = AppLogger.getLogger(BackupService.class);

    private static final DateTimeFormatter DIR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private static final List<String> PARTIAL_PG_TABLES =
        List.of("patients", "medical_records", "prescriptions", "lab_orders", "invoices");
    private static final List<String> PARTIAL_MONGO_COLLECTIONS =
        List.of("patient_notes", "notifications");

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Runs one full backup pipeline (Postgres data dump + Mongo data dump),
     * writes {@code manifest.json}, and returns it. Synchronous — callers on
     * the FX thread must wrap this in {@code AsyncJobRunner.submit(...)}.
     */
    public BackupManifest runBackup(BackupType type) throws Exception {
        return runBackup(type, null);
    }

    /**
     * Same as {@link #runBackup(BackupType)}, but reports one progress message
     * per table/collection completed via {@code progressCallback} (may be
     * {@code null}). Callers on the FX thread must marshal the callback's
     * argument back to the FX thread themselves — this method never touches
     * JavaFX.
     */
    public BackupManifest runBackup(BackupType type, Consumer<String> progressCallback) throws Exception {
        BackupManifest manifest = new BackupManifest();
        manifest.type = type;
        manifest.startedAt = LocalDateTime.now();
        manifest.backupId = manifest.startedAt.format(DIR_FMT) + "_" + type.name().toLowerCase();
        manifest.status = "SUCCESS";

        Path dir = backupsRootDir().resolve(manifest.backupId);
        Files.createDirectories(dir);
        manifest.directoryPath = dir.toAbsolutePath().toString();
        logger.info("Backup started: " + manifest.backupId + " at " + manifest.directoryPath);

        try {
            List<String> tables = resolvePostgresTables(type);
            try (Connection conn = DBConnection.getConnection()) {
                for (String table : tables) {
                    BackupManifest.TableEntry entry = dumpPostgresTable(conn, table, dir);
                    manifest.postgresTables.add(entry);
                    report(progressCallback, "Backed up table " + table + " (" + entry.rowCount() + " rows)");
                }
            }

            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) {
                manifest.mongoSkipped = true;
                manifest.status = "PARTIAL_FAILURE";
                manifest.errorMessage = "MongoDB was unavailable during this backup — only PostgreSQL data was captured.";
                logger.warn("Backup " + manifest.backupId + ": MongoDB unavailable, Postgres-only.");
            } else {
                List<String> collections = resolveMongoCollections(type, db);
                for (String collection : collections) {
                    BackupManifest.CollectionEntry entry = dumpMongoCollection(db, collection, dir);
                    manifest.mongoCollections.add(entry);
                    report(progressCallback, "Backed up collection " + collection + " (" + entry.docCount() + " docs)");
                }
            }
        } catch (Exception e) {
            manifest.status = "FAILED";
            manifest.errorMessage = e.getMessage();
            logger.error("Backup " + manifest.backupId + " failed: " + e.getMessage(), e);
            manifest.finishedAt = LocalDateTime.now();
            writeManifest(dir, manifest);
            throw e;
        }

        manifest.finishedAt = LocalDateTime.now();
        writeManifest(dir, manifest);
        logger.info("Backup finished: " + manifest.backupId + " — status=" + manifest.status);
        return manifest;
    }

    /**
     * Lists completed backups under {@code ~/.hms/backups/}, newest first, by
     * reading each subdirectory's {@code manifest.json}. Corrupt/unreadable
     * manifests are skipped, not thrown — the history UI must never fail to
     * render because one backup's manifest is bad.
     */
    public List<BackupManifest> listBackups() {
        List<BackupManifest> result = new ArrayList<>();
        Path root = backupsRootDir();
        if (!Files.isDirectory(root)) return result;

        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(root)) {
            for (Path dir : dirs) {
                if (!Files.isDirectory(dir)) continue;
                Path manifestPath = dir.resolve("manifest.json");
                if (!Files.exists(manifestPath)) continue;
                try {
                    BackupManifest manifest = MAPPER.readValue(manifestPath.toFile(), BackupManifest.class);
                    result.add(manifest);
                } catch (Exception e) {
                    logger.warn("Skipping unreadable backup manifest: " + manifestPath + " — " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.warn("Could not list backups: " + e.getMessage());
        }

        result.sort((a, b) -> {
            LocalDateTime ta = a.startedAt != null ? a.startedAt : LocalDateTime.MIN;
            LocalDateTime tb = b.startedAt != null ? b.startedAt : LocalDateTime.MIN;
            return tb.compareTo(ta);
        });
        return result;
    }

    /**
     * Deletes backup directories older than {@code policy.getBackupRetentionDays()}
     * (by manifest {@code finishedAt}, falling back to directory mtime if the
     * manifest is missing/corrupt). Returns the count pruned. Intended to run
     * at the end of every scheduled cycle — not after a manual "Backup Now",
     * so an admin who just asked for one backup doesn't have it immediately
     * eligible for pruning on the same run.
     */
    public int pruneOldBackups(BackupPolicy policy) throws IOException {
        Path root = backupsRootDir();
        if (!Files.isDirectory(root)) return 0;

        LocalDateTime cutoff = LocalDateTime.now().minusDays(policy.getBackupRetentionDays());
        int pruned = 0;

        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(root)) {
            for (Path dir : dirs) {
                if (!Files.isDirectory(dir)) continue;
                LocalDateTime finishedAt = readFinishedAt(dir);
                boolean stale = finishedAt != null
                    ? finishedAt.isBefore(cutoff)
                    : Files.getLastModifiedTime(dir).toInstant()
                        .isBefore(cutoff.atZone(java.time.ZoneId.systemDefault()).toInstant());
                if (stale) {
                    deleteRecursively(dir);
                    pruned++;
                    logger.info("Pruned old backup: " + dir.getFileName());
                }
            }
        }
        return pruned;
    }

    // ── Postgres ──────────────────────────────────────────────────────────────

    private List<String> resolvePostgresTables(BackupType type) throws Exception {
        if (type == BackupType.PARTIAL) return PARTIAL_PG_TABLES;

        List<String> tables = new ArrayList<>();
        String sql = """
                SELECT table_name FROM information_schema.tables
                 WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                 ORDER BY table_name
                """;
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) tables.add(rs.getString("table_name"));
        }
        return tables;
    }

    /**
     * Dumps every row of {@code table} to {@code <dir>/<table>.jsonl}, one JSON
     * object per line. Column values are converted generically from JDBC's
     * {@code ResultSetMetaData} rather than by generating type-correct SQL
     * {@code INSERT} statements (that's most of what {@code pg_dump} exists to
     * get right) — this stays fully restorable later by reading each line and
     * binding its fields via {@code PreparedStatement}.
     */
    private BackupManifest.TableEntry dumpPostgresTable(Connection conn, String table, Path dir) throws Exception {
        String safeName = table.replace("\"", "\"\"");
        long rowCount = 0;
        Path outFile = dir.resolve(table + ".jsonl");

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM \"" + safeName + "\"");
             ResultSet rs = ps.executeQuery();
             java.io.BufferedWriter writer = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    String colName = meta.getColumnLabel(i);
                    row.put(colName, convertColumnValue(rs, meta, i));
                }
                writer.write(MAPPER.writeValueAsString(row));
                writer.newLine();
                rowCount++;
            }
        }
        return new BackupManifest.TableEntry(table, rowCount);
    }

    private Object convertColumnValue(ResultSet rs, ResultSetMetaData meta, int i) throws Exception {
        if (rs.getObject(i) == null) return null;
        String typeName = meta.getColumnTypeName(i).toLowerCase();

        switch (typeName) {
            case "jsonb":
            case "json": {
                String raw = rs.getString(i);
                return raw == null ? null : MAPPER.readTree(raw);
            }
            case "bytea": {
                byte[] bytes = rs.getBytes(i);
                return bytes == null ? null : Base64.getEncoder().encodeToString(bytes);
            }
            case "timestamp":
            case "timestamptz": {
                java.sql.Timestamp ts = rs.getTimestamp(i);
                return ts == null ? null : ts.toLocalDateTime().toString();
            }
            case "date": {
                java.sql.Date d = rs.getDate(i);
                return d == null ? null : d.toLocalDate().toString();
            }
            default:
                // Numbers, strings, booleans, UUID — Jackson serializes these natively.
                return rs.getObject(i);
        }
    }

    // ── MongoDB ───────────────────────────────────────────────────────────────

    private List<String> resolveMongoCollections(BackupType type, MongoDatabase db) {
        if (type == BackupType.PARTIAL) return PARTIAL_MONGO_COLLECTIONS;
        List<String> names = new ArrayList<>();
        db.listCollectionNames().forEach(names::add);
        return names;
    }

    private BackupManifest.CollectionEntry dumpMongoCollection(MongoDatabase db, String collection, Path dir) throws IOException {
        long docCount = 0;
        Path outFile = dir.resolve("mongo_" + collection + ".jsonl");
        try (java.io.BufferedWriter writer = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
            for (Document doc : db.getCollection(collection).find()) {
                writer.write(doc.toJson());
                writer.newLine();
                docCount++;
            }
        }
        return new BackupManifest.CollectionEntry(collection, docCount);
    }

    // ── Manifest I/O ──────────────────────────────────────────────────────────

    private void writeManifest(Path dir, BackupManifest manifest) {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(dir.resolve("manifest.json").toFile(), manifest);
        } catch (IOException e) {
            logger.error("Failed to write manifest for backup " + manifest.backupId + ": " + e.getMessage(), e);
        }
    }

    private LocalDateTime readFinishedAt(Path dir) {
        Path manifestPath = dir.resolve("manifest.json");
        if (!Files.exists(manifestPath)) return null;
        try {
            BackupManifest manifest = MAPPER.readValue(manifestPath.toFile(), BackupManifest.class);
            return manifest.finishedAt;
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteRecursively(Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        }
    }

    private static void report(Consumer<String> progressCallback, String message) {
        if (progressCallback != null) progressCallback.accept(message);
    }

    private static Path backupsRootDir() {
        return Paths.get(System.getProperty("user.home"), ".hms", "backups");
    }
}
