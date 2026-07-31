package hospital.management.backend.model.base;

import java.time.LocalDateTime;

/**
 * Abstract base for append-only log records.
 * Intentionally does NOT implement SoftDeletable — logs are immutable once written.
 *
 * The separation from BaseEntity demonstrates Interface Segregation:
 * log tables in the DB have no updated_at or deleted_at columns, so forcing
 * them to implement SoftDeletable would violate both ISP and LSP.
 */
public abstract class BaseLog implements Identifiable, Auditable {

    private String        logId;
    private String        userId;
    private LocalDateTime createdAt;

    protected BaseLog() {}

    protected BaseLog(String logId, String userId, LocalDateTime createdAt) {
        this.logId     = logId;
        this.userId    = userId;
        this.createdAt = createdAt;
    }

    // ── Identifiable ─────────────────────────────────────────────────────────

    @Override
    public String getId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    /** Short label that distinguishes log types: "audit", "system". */
    public abstract String getLogType();

    @Override
    public String getEntityType() { return getLogType() + "_log"; }

    // ── Auditable ────────────────────────────────────────────────────────────

    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── User link ─────────────────────────────────────────────────────────────

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    /** Human-readable summary of what this log entry records. */
    public abstract String getSummary();

    @Override
    public String toString() {
        return getLogType() + "_log[" + logId + "] @ " + createdAt;
    }
}