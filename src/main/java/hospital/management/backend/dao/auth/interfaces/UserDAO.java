package hospital.management.backend.dao.auth.interfaces;

import hospital.management.backend.model.user.User;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.sql.Connection;
import java.util.Optional;

public interface UserDAO {
    User save(User user) throws Exception;

    /** Same as {@link #save(User)} but runs on a caller-supplied connection, so a
     *  service can compose it into a larger transaction (e.g. create user + assign role). */
    User save(User user, Connection conn) throws Exception;
    Optional<User> findById(String userId) throws Exception;
    Optional<User> findByUsername(String username) throws Exception;
    Optional<User> findByEmail(String email) throws Exception;

    /** Looks up the login account linked to a doctor (via users.doctor_id), if any.
     *  Not every doctor has a login account — an empty Optional means "nothing to notify". */
    Optional<User> findByDoctorId(String doctorId) throws Exception;
    PageResult<User> findAll(PageRequest request) throws Exception;
    User update(User user) throws Exception;
    User update(User user, Connection conn) throws Exception;

    /** Password changes are kept separate from the general update() so they can't be
     *  silently overwritten by an unrelated profile edit, and so the audit trail (via the
     *  DB trigger on password_hash changes) always reflects an explicit intent to rotate it. */
    void updatePasswordHash(String userId, String newPasswordHash) throws Exception;
    void updatePasswordHash(String userId, String newPasswordHash, Connection conn) throws Exception;

    void softDelete(String userId) throws Exception;
    boolean existsByUsername(String username) throws Exception;
    boolean existsByEmail(String email) throws Exception;
}