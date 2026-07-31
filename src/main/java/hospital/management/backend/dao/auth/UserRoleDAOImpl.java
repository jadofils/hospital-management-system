package hospital.management.backend.dao.auth;

import hospital.management.backend.dao.auth.interfaces.UserRoleDAO;
import hospital.management.backend.model.user.UserRole;

import java.util.List;

public class UserRoleDAOImpl implements UserRoleDAO {

    @Override
    public void assign(String userId, String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void revoke(String userId, String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<UserRole> findByUserId(String userId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<UserRole> findByRoleId(String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean exists(String userId, String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}