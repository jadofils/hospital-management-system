package hospital.management.backend.model.user;

import java.time.LocalDateTime;

/**
 * Entity model for the `role_permissions` table.
 * Every column in the schema is represented as a field below.
 */
public class RolePermission {

    /** FK -> roles, part of composite PK */
    private String roleId;
    /** FK -> permissions, part of composite PK */
    private String permissionId;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public RolePermission() {
    }

    public RolePermission(String roleId, String permissionId, LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "RolePermission{" + "roleId=" + roleId + ", " + "permissionId=" + permissionId + ", " + "createdAt=" + createdAt + ", " + "deletedAt=" + deletedAt + "}";
    }
}