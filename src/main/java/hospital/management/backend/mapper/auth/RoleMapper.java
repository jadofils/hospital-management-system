package hospital.management.backend.mapper.auth;

import hospital.management.backend.dto.auth.CreateRoleDTO;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.model.user.Role;

public class RoleMapper {

    public static RoleDTO toDTO(Role role) {
        if (role == null) return null;
        return new RoleDTO(
            role.getRoleId(),
            role.getRoleName(),
            role.getCreatedAt()
        );
    }

    public static Role toEntity(CreateRoleDTO dto) {
        if (dto == null) return null;
        Role role = new Role();
        role.setRoleName(dto.getRoleName());
        return role;
    }
}