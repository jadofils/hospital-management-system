package hospital.management.backend.model.user;

import java.time.LocalDateTime;

/**
 * Entity model for the `user_roles` table.
 * Every column in the schema is represented as a field below.
 */
public class UserRole {

    /** FK -> users, part of composite PK */
    private String userId;
    /** FK -> roles, part of composite PK */
    private String roleId;
    /** acts as this table's created_at */
    private LocalDateTime assignedAt;
    private LocalDateTime updatedAt;
    /** soft-revoke marker, null = currently active */
    private LocalDateTime revokedAt;

    public UserRole() {
    }

    public UserRole(String userId, String roleId, LocalDateTime assignedAt, LocalDateTime updatedAt, LocalDateTime revokedAt) {
        this.userId = userId;
        this.roleId = roleId;
        this.assignedAt = assignedAt;
        this.updatedAt = updatedAt;
        this.revokedAt = revokedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    @Override
    public String toString() {
        return "UserRole{" + "userId=" + userId + ", " + "roleId=" + roleId + ", " + "assignedAt=" + assignedAt + ", " + "updatedAt=" + updatedAt + ", " + "revokedAt=" + revokedAt + "}";
    }
}