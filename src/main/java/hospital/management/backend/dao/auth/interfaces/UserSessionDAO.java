package hospital.management.backend.dao.auth.interfaces;

import hospital.management.backend.model.user.UserSession;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface UserSessionDAO {
    UserSession save(UserSession session) throws Exception;

    /** Same as {@link #save(UserSession)} but composable into a caller-managed transaction. */
    UserSession save(UserSession session, Connection conn) throws Exception;
    Optional<UserSession> findById(String sessionId) throws Exception;
    List<UserSession> findActiveByUserId(String userId) throws Exception;
    void deactivate(String sessionId) throws Exception;
    void deactivate(String sessionId, Connection conn) throws Exception;
    void deactivateAll(String userId) throws Exception;
    void deactivateAll(String userId, Connection conn) throws Exception;
}