package hospital.management.backend.service.auth.interfaces;

import hospital.management.backend.dto.auth.CreateRoleDTO;
import hospital.management.backend.dto.auth.PermissionDTO;
import hospital.management.backend.dto.auth.RoleDTO;

import java.util.List;

public interface RoleService {
    RoleDTO create(CreateRoleDTO dto) throws Exception;
    RoleDTO findById(String roleId) throws Exception;
    List<RoleDTO> findAll() throws Exception;
    void delete(String roleId) throws Exception;

    // ── User <-> Role assignment ────────────────────────────────────────────
    void assignToUser(String userId, String roleId) throws Exception;
    void revokeFromUser(String userId, String roleId) throws Exception;
    List<RoleDTO> findRolesForUser(String userId) throws Exception;

    /** Reverse lookup of {@link #findRolesForUser}: every user id currently holding
     *  the named role (e.g. "Admin", "Pharmacist"). Empty list if the role doesn't
     *  exist or has no members — never throws for that case. */
    List<String> findUserIdsForRole(String roleName) throws Exception;

    // ── Role <-> Permission assignment ──────────────────────────────────────
    void assignPermission(String roleId, String permissionId) throws Exception;
    void revokePermission(String roleId, String permissionId) throws Exception;
    List<PermissionDTO> findPermissionsForRole(String roleId) throws Exception;
}
