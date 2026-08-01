package hospital.management.backend.mapper.auth;

import hospital.management.backend.dto.auth.CreatePermissionDTO;
import hospital.management.backend.dto.auth.PermissionDTO;
import hospital.management.backend.model.user.Permission;

public class PermissionMapper {

    public static PermissionDTO toDTO(Permission permission) {
        if (permission == null) return null;
        return new PermissionDTO(
            permission.getPermissionId(),
            permission.getResource(),
            permission.getAction(),
            permission.getCreatedAt()
        );
    }

    public static Permission toEntity(CreatePermissionDTO dto) {
        if (dto == null) return null;
        Permission permission = new Permission();
        permission.setResource(dto.getResource());
        permission.setAction(dto.getAction());
        return permission;
    }
}
