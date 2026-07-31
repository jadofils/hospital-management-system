package hospital.management.backend.model.user;

import hospital.management.backend.model.base.BaseLog;

import java.time.LocalDateTime;

public class SystemLog extends BaseLog {

    private String logLevel;
    private String source;
    private String message;

    public SystemLog() {}

    public SystemLog(String logId, String logLevel, String source,
                     String message, String userId, LocalDateTime createdAt) {
        super(logId, userId, createdAt);
        this.logLevel = logLevel;
        this.source   = source;
        this.message  = message;
    }

    // ── BaseLog contracts ─────────────────────────────────────────────────────

    @Override
    public String getLogType() { return "system"; }

    @Override
    public String getSummary() {
        return "[" + logLevel + "] " + source + ": " + message;
    }

    // ── SystemLog-specific fields ─────────────────────────────────────────────

    public String getLogId() { return getId(); }
    public void setLogId(String id) { super.setLogId(id); }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}