package hospital.management.backend.model.base;

import java.time.LocalDateTime;

/**
 * Abstract root for every persistent entity that has a UUID primary key,
 * audit timestamps, and supports soft-delete.
 *
 * SOLID applied:
 *  - Single Responsibility: only owns identity + audit state
 *  - Open/Closed: extend to add domain fields; never modify this class for new entities
 *  - Liskov: any subclass can be substituted wherever BaseEntity is expected
 *
 * Subclasses must implement:
 *  - getEntityType() — short type label, e.g. "patient", "doctor"
 *  - getSummary()    — human-readable one-liner for logs and display
 */
public abstract class BaseEntity implements Identifiable, Auditable, SoftDeletable {

    private String          id;
    private LocalDateTime   createdAt;
    private LocalDateTime   updatedAt;
    private LocalDateTime   deletedAt;

    protected BaseEntity() {}

    protected BaseEntity(String id) {
        this.id = id;
    }

    // ── Identifiable ─────────────────────────────────────────────────────────

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public abstract String getEntityType();

    // ── Auditable ────────────────────────────────────────────────────────────

    @Override
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ── SoftDeletable ────────────────────────────────────────────────────────

    @Override
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public boolean isDeleted() { return deletedAt != null; }

    @Override
    public void markDeleted() { this.deletedAt = LocalDateTime.now(); }

    // ── Polymorphic display ───────────────────────────────────────────────────

    /** Human-readable one-liner used in logs, toasts, and debug output. */
    public abstract String getSummary();

    @Override
    public String toString() {
        return getEntityType() + "[" + id + "]" + (isDeleted() ? " (deleted)" : "");
    }
}
