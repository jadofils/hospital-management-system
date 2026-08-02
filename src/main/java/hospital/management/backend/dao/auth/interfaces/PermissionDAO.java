package hospital.management.backend.dao.auth.interfaces;

import hospital.management.backend.model.user.Permission;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface PermissionDAO {
    Permission save(Permission permission) throws Exception;

    /** Same as {@link #save(Permission)} but composable into a caller-managed transaction. */
    Permission save(Permission permission, Connection conn) throws Exception;

    Optional<Permission> findById(String permissionId) throws Exception;
    Optional<Permission> findByResourceAndAction(String resource, String action) throws Exception;
    List<Permission> findAll() throws Exception;
    void softDelete(String permissionId) throws Exception;
}
