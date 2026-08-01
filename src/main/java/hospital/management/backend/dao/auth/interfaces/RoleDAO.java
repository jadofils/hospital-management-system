package hospital.management.backend.dao.auth.interfaces;

import hospital.management.backend.model.user.Role;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface RoleDAO {
    Role save(Role role) throws Exception;

    /** Same as {@link #save(Role)} but composable into a caller-managed transaction. */
    Role save(Role role, Connection conn) throws Exception;

    Optional<Role> findById(String roleId) throws Exception;
    Optional<Role> findByName(String roleName) throws Exception;
    List<Role> findAll() throws Exception;
    void softDelete(String roleId) throws Exception;
}