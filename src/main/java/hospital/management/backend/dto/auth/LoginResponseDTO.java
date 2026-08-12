package hospital.management.backend.dto.auth;

import java.util.List;

public class LoginResponseDTO {

    private String token;
    private String sessionId;
    private String userId;
    private String username;
    private String role;
    private List<String> roles;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String token, String sessionId, String userId, String username, String role) {
        this(token, sessionId, userId, username, role, List.of(role));
    }

    public LoginResponseDTO(String token, String sessionId, String userId, String username, String role, List<String> roles) {
        this.token     = token;
        this.sessionId = sessionId;
        this.userId    = userId;
        this.username  = username;
        this.role      = role;
        this.roles     = roles;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    @Override
    public String toString() {
        return "LoginResponseDTO{userId='" + userId + "', username='" + username + "', role='" + role + "'}";
    }
}