package hospital.management.backend.dto.auth;

import java.time.LocalDateTime;

public class UserDTO {

    private String        userId;
    private String        doctorId;
    private String        username;
    private String        email;
    private Boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDTO() {}

    public UserDTO(String userId, String doctorId, String username,
                   String email, Boolean isActive,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId    = userId;
        this.doctorId  = doctorId;
        this.username  = username;
        this.email     = email;
        this.isActive  = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "UserDTO{userId='" + userId + "', username='" + username + "', email='" + email + "'}";
    }
}