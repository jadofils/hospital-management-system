package hospital.management.backend.model.user;

import hospital.management.backend.model.base.BaseLog;

import java.time.LocalDateTime;

public class AuditLog extends BaseLog {

    private String action;
    private String tableAffected;
    private String recordId;

    public AuditLog() {}

    public AuditLog(String logId, String userId, String action,
                    String tableAffected, String recordId,
                    LocalDateTime createdAt) {
        super(logId, userId, createdAt);
        this.action        = action;
        this.tableAffected = tableAffected;
        this.recordId      = recordId;
    }

    // ── BaseLog contracts ─────────────────────────────────────────────────────

    @Override
    public String getLogType() { return "audit"; }

    @Override
    public String getSummary() {
        return action + " on " + tableAffected
            + (recordId != null ? " [" + recordId + "]" : "");
    }

    // ── AuditLog-specific fields ──────────────────────────────────────────────

    public String getLogId() { return getId(); }
    public void setLogId(String id) { super.setLogId(id); }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTableAffected() { return tableAffected; }
    public void setTableAffected(String tableAffected) { this.tableAffected = tableAffected; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
}