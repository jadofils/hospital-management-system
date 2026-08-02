package hospital.management.backend.dto.log;

import java.time.LocalDateTime;

public class AuditLogDTO {

    private String        logId;
    private String        userId;
    private String        action;
    private String        tableAffected;
    private String        recordId;
    private LocalDateTime createdAt;

    public AuditLogDTO() {}

    public AuditLogDTO(String logId, String userId, String action,
                       String tableAffected, String recordId, LocalDateTime createdAt) {
        this.logId         = logId;
        this.userId        = userId;
        this.action        = action;
        this.tableAffected = tableAffected;
        this.recordId      = recordId;
        this.createdAt     = createdAt;
    }

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTableAffected() { return tableAffected; }
    public void setTableAffected(String tableAffected) { this.tableAffected = tableAffected; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "AuditLogDTO{logId='" + logId + "', action='" + action + "', table='" + tableAffected + "'}";
    }
}