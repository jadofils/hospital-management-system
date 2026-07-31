package hospital.management.backend.dto.auth;

public class CreateRoleDTO {

    private String roleName;

    public CreateRoleDTO() {}

    public CreateRoleDTO(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    @Override
    public String toString() {
        return "CreateRoleDTO{roleName='" + roleName + "'}";
    }
}