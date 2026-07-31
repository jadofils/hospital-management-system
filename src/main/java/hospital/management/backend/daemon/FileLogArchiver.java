package hospital.management.backend.daemon;

import hospital.management.backend.config.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Manages log files on disk in three passes each cycle:
 *
 *   Pass 1 — ARCHIVE: any .log file larger than fileLogMaxSizeMb gets compressed to .log.gz
 *   Pass 2 — RE-ZIP:  if a .log.gz is still larger than the threshold (rare but possible
 *                     with incompressible data), it is recompressed at maximum level
 *   Pass 3 — DELETE:  any .log.gz whose last-access time is older than archiveRetentionDays
 *                     is permanently deleted
 *
 * Log directory: ~/.hms/logs/
 * If the directory does not exist the archiver skips silently.
 */
public class FileLogArchiver implements CleanupTask {

    private static final AppLogger logger = AppLogger.getLogger(FileLogArchiver.class);
    static final Path LOG_DIR = Paths.get(System.getProperty("user.home"), ".hms", "logs");

    @Override
    public String getName() { return "file-log-archiver"; }

    @Override
    public String run(RetentionPolicy policy) throws Exception {
        if (!Files.isDirectory(LOG_DIR)) {
            return "Log directory not found — skipped.";
        }

        long maxBytes      = (long) policy.getFileLogMaxSizeMb() * 1024 * 1024;
        int  retentionDays = policy.getArchiveRetentionDays();

        int archived = 0, deleted = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(LOG_DIR)) {
            for (Path file : stream) {
                String name = file.getFileName().toString();

                // Pass 1: archive large .log files
                if (name.endsWith(".log") && Files.size(file) > maxBytes) {
                    compress(file);
                    Files.delete(file);
                    archived++;
                    continue;
                }

                // Pass 2: re-zip large .log.gz files
                if (name.endsWith(".log.gz") && Files.size(file) > maxBytes) {
                    recompress(file);
                    logger.info("[" + getName() + "] Re-compressed: " + name);
                }

                // Pass 3: delete stale archives
                if (name.endsWith(".log.gz") && isStale(file, retentionDays)) {
                    Files.delete(file);
                    deleted++;
                    logger.info("[" + getName() + "] Deleted stale archive: " + name);
                }
            }
        }

        String summary = "Archived " + archived + " log file(s), deleted " + deleted + " stale archive(s).";
        logger.info("[" + getName() + "] " + summary);
        return summary;
    }

    // ── Compression helpers ───────────────────────────────────────────────────

    private void compress(Path source) throws IOException {
        Path dest = source.resolveSibling(source.getFileName() + ".gz");
        try (InputStream in  = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(dest);
             GZIPOutputStream gz = new GZIPOutputStream(out) {{
                 // maximum compression level
                 def.setLevel(java.util.zip.Deflater.BEST_COMPRESSION);
             }}) {
            in.transferTo(gz);
        }
        logger.info("[" + getName() + "] Archived: " + source.getFileName()
            + " → " + dest.getFileName()
            + " (" + toMb(Files.size(dest)) + " MB)");
    }

    private void recompress(Path archive) throws IOException {
        Path temp = archive.resolveSibling(archive.getFileName() + ".tmp");
        compress(archive);              // compress the already-compressed file
        Files.move(temp, archive, StandardCopyOption.REPLACE_EXISTING);
    }

    // ── Staleness check ───────────────────────────────────────────────────────

    private boolean isStale(Path file, int retentionDays) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        Instant lastAccess = attrs.lastAccessTime().toInstant();
        long daysSinceAccess = ChronoUnit.DAYS.between(lastAccess, Instant.now());
        return daysSinceAccess >= retentionDays;
    }

    private String toMb(long bytes) {
        return String.format("%.2f", bytes / (1024.0 * 1024.0));
    }
}