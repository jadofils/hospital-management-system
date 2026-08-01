package hospital.management.backend.dao.auth.interfaces;

import hospital.management.backend.model.user.RolePermission;

import java.sql.Connection;
import java.util.List;

public interface RolePermissionDAO {
    void assign(String roleId, String permissionId) throws Exception;

    /** Same as {@link #assign(String, String)} but composable into a caller-managed transaction. */
    void assign(String roleId, String permissionId, Connection conn) throws Exception;

    void revoke(String roleId, String permissionId) throws Exception;
    List<RolePermission> findByRoleId(String roleId) throws Exception;
    List<RolePermission> findByPermissionId(String permissionId) throws Exception;
    boolean exists(String roleId, String permissionId) throws Exception;
}
