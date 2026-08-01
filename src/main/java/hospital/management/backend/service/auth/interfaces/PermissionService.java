package hospital.management.backend.service.auth.interfaces;

import hospital.management.backend.dto.auth.CreatePermissionDTO;
import hospital.management.backend.dto.auth.PermissionDTO;

import java.util.List;

public interface PermissionService {
    PermissionDTO create(CreatePermissionDTO dto) throws Exception;
    PermissionDTO findById(String permissionId) throws Exception;
    List<PermissionDTO> findAll() throws Exception;
    void delete(String permissionId) throws Exception;
}
