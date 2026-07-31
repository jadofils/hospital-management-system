package hospital.management.backend.service.auth.interfaces;

import hospital.management.backend.dto.auth.CreateRoleDTO;
import hospital.management.backend.dto.auth.RoleDTO;

import java.util.List;

public interface RoleService {
    RoleDTO create(CreateRoleDTO dto) throws Exception;
    RoleDTO findById(String roleId) throws Exception;
    List<RoleDTO> findAll() throws Exception;
    void assignToUser(String userId, String roleId) throws Exception;
    void revokeFromUser(String userId, String roleId) throws Exception;
    void delete(String roleId) throws Exception;
}