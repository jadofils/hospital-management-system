package hospital.management.backend.dto.auth;

public class UpdateUserDTO {

    private String  userId;
    private String  email;
    private Boolean isActive;

    public UpdateUserDTO() {}

    public UpdateUserDTO(String userId, String email, Boolean isActive) {
        this.userId   = userId;
        this.email    = email;
        this.isActive = isActive;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    @Override
    public String toString() {
        return "UpdateUserDTO{userId='" + userId + "', email='" + email + "'}";
    }
}