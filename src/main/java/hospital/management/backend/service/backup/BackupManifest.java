package hospital.management.backend.service.backup;

import hospital.management.backend.daemon.BackupType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes one completed (or failed) backup run. Serialized to
 * {@code manifest.json} inside the backup's own directory by
 * {@link BackupService}, and read back by {@link BackupService#listBackups()}
 * to populate the Developer Dashboard's backup history table.
 */
public class BackupManifest {

    /** One PostgreSQL table captured in this backup. */
    public record TableEntry(String name, long rowCount) {}

    /** One MongoDB collection captured in this backup. */
    public record CollectionEntry(String name, long docCount) {}

    /** Directory name for this backup, e.g. {@code "2026-08-06_143000_partial"}. */
    public String backupId;

    /** Absolute path to this backup's directory on disk, e.g.
     *  {@code C:\Users\...\.hms\backups\2026-08-06_143000_partial} — shown to the
     *  admin right after a manual backup so they know exactly where it landed. */
    public String directoryPath;

    public BackupType type;
    public LocalDateTime startedAt;
    public LocalDateTime finishedAt;

    public List<TableEntry> postgresTables = new ArrayList<>();
    public List<CollectionEntry> mongoCollections = new ArrayList<>();

    /** True if MongoDB was unavailable during this run — the Postgres portion still completed. */
    public boolean mongoSkipped;

    /** {@code "SUCCESS"}, {@code "FAILED"}, or {@code "PARTIAL_FAILURE"} (Mongo skipped). */
    public String status;

    /** Nullable — set when status is FAILED or PARTIAL_FAILURE. */
    public String errorMessage;

    public BackupManifest() {}
}
