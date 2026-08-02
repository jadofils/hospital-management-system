package hospital.management.backend.dto.auth;

import java.time.LocalDateTime;

public class PermissionDTO {

    private String        permissionId;
    private String        resource;
    private String        action;
    private LocalDateTime createdAt;

    public PermissionDTO() {}

    public PermissionDTO(String permissionId, String resource, String action, LocalDateTime createdAt) {
        this.permissionId = permissionId;
        this.resource     = resource;
        this.action       = action;
        this.createdAt    = createdAt;
    }

    public String getPermissionId() { return permissionId; }
    public void setPermissionId(String permissionId) { this.permissionId = permissionId; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "PermissionDTO{permissionId='" + permissionId + "', resource='" + resource + "', action='" + action + "'}";
    }
}