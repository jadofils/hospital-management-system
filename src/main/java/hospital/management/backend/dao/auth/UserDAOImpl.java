package hospital.management.backend.dao.auth;

import hospital.management.backend.dao.auth.interfaces.UserDAO;
import hospital.management.backend.model.user.User;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.Optional;

public class UserDAOImpl implements UserDAO {

    @Override
    public User save(User user) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<User> findById(String userId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<User> findByUsername(String username) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<User> findByEmail(String email) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<User> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public User update(User user) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String userId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsByUsername(String username) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsByEmail(String email) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}