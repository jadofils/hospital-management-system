package hospital.management.backend.dao.auth.interfaces;

import hospital.management.backend.model.user.User;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.Optional;

public interface UserDAO {
    User save(User user) throws Exception;
    Optional<User> findById(String userId) throws Exception;
    Optional<User> findByUsername(String username) throws Exception;
    Optional<User> findByEmail(String email) throws Exception;
    PageResult<User> findAll(PageRequest request) throws Exception;
    User update(User user) throws Exception;
    void softDelete(String userId) throws Exception;
    boolean existsByUsername(String username) throws Exception;
    boolean existsByEmail(String email) throws Exception;
}