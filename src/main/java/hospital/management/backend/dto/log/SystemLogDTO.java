package hospital.management.backend.dto.log;

import java.time.LocalDateTime;

public class SystemLogDTO {

    private String        logId;
    private String        logLevel;
    private String        source;
    private String        message;
    private String        userId;
    private LocalDateTime createdAt;

    public SystemLogDTO() {}

    public SystemLogDTO(String logId, String logLevel, String source,
                        String message, String userId, LocalDateTime createdAt) {
        this.logId     = logId;
        this.logLevel  = logLevel;
        this.source    = source;
        this.message   = message;
        this.userId    = userId;
        this.createdAt = createdAt;
    }

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "SystemLogDTO{logId='" + logId + "', level='" + logLevel + "', source='" + source + "'}";
    }
}