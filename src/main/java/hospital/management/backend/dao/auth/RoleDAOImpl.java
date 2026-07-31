package hospital.management.backend.dao.auth;

import hospital.management.backend.dao.auth.interfaces.RoleDAO;
import hospital.management.backend.model.user.Role;

import java.util.List;
import java.util.Optional;

public class RoleDAOImpl implements RoleDAO {

    @Override
    public Role save(Role role) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Role> findById(String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Role> findByName(String roleName) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Role> findAll() throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}