package hospital.management.backend.dto.auth;

import java.time.LocalDateTime;

public class RoleDTO {

    private String        roleId;
    private String        roleName;
    private LocalDateTime createdAt;

    public RoleDTO() {}

    public RoleDTO(String roleId, String roleName, LocalDateTime createdAt) {
        this.roleId    = roleId;
        this.roleName  = roleName;
        this.createdAt = createdAt;
    }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "RoleDTO{roleId='" + roleId + "', roleName='" + roleName + "'}";
    }
}