package hospital.management.backend.model.user;

import hospital.management.backend.model.base.BaseEntity;

import java.time.LocalDateTime;

public class User extends BaseEntity {

    private String  doctorId;
    private String  username;
    private String  passwordHash;
    private String  email;
    private Boolean isActive;

    public User() {}

    public User(String userId, String doctorId, String username,
                String passwordHash, String email, Boolean isActive,
                LocalDateTime createdAt, LocalDateTime updatedAt,
                LocalDateTime deletedAt) {
        super(userId);
        this.doctorId     = doctorId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.email        = email;
        this.isActive     = isActive;
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
        setDeletedAt(deletedAt);
    }

    // ── BaseEntity contracts ──────────────────────────────────────────────────

    @Override
    public String getEntityType() { return "user"; }

    @Override
    public String getSummary() {
        return "User: " + username + (Boolean.TRUE.equals(isActive) ? " [active]" : " [inactive]");
    }

    // ── Domain alias for ID ───────────────────────────────────────────────────

    public String getUserId() { return getId(); }
    public void setUserId(String id) { setId(id); }

    // ── User-specific fields ──────────────────────────────────────────────────

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}