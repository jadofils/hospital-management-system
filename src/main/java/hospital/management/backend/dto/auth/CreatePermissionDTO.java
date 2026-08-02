package hospital.management.backend.dto.auth;

public class CreatePermissionDTO {

    private String resource;
    private String action;

    public CreatePermissionDTO() {}

    public CreatePermissionDTO(String resource, String action) {
        this.resource = resource;
        this.action   = action;
    }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    @Override
    public String toString() {
        return "CreatePermissionDTO{resource='" + resource + "', action='" + action + "'}";
    }
}
