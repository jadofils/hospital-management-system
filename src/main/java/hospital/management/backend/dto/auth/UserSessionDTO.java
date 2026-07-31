package hospital.management.backend.dto.auth;

import java.time.LocalDateTime;

public class UserSessionDTO {

    private String        sessionId;
    private String        userId;
    private LocalDateTime loginAt;
    private LocalDateTime logoutAt;
    private LocalDateTime expiresAt;
    private String        ipAddress;
    private String        userAgent;
    private Boolean       isActive;

    public UserSessionDTO() {}

    public UserSessionDTO(String sessionId, String userId, LocalDateTime loginAt,
                          LocalDateTime logoutAt, LocalDateTime expiresAt,
                          String ipAddress, String userAgent, Boolean isActive) {
        this.sessionId = sessionId;
        this.userId    = userId;
        this.loginAt   = loginAt;
        this.logoutAt  = logoutAt;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.isActive  = isActive;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getLoginAt() { return loginAt; }
    public void setLoginAt(LocalDateTime loginAt) { this.loginAt = loginAt; }

    public LocalDateTime getLogoutAt() { return logoutAt; }
    public void setLogoutAt(LocalDateTime logoutAt) { this.logoutAt = logoutAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    @Override
    public String toString() {
        return "UserSessionDTO{sessionId='" + sessionId + "', userId='" + userId + "', isActive=" + isActive + "}";
    }
}