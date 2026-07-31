package hospital.management.backend.dao.auth.interfaces;

import hospital.management.backend.model.user.UserRole;

import java.util.List;

public interface UserRoleDAO {
    void assign(String userId, String roleId) throws Exception;
    void revoke(String userId, String roleId) throws Exception;
    List<UserRole> findByUserId(String userId) throws Exception;
    List<UserRole> findByRoleId(String roleId) throws Exception;
    boolean exists(String userId, String roleId) throws Exception;
}