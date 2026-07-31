package hospital.management.backend.dao.auth.interfaces;

import hospital.management.backend.model.user.UserSession;

import java.util.List;
import java.util.Optional;

public interface UserSessionDAO {
    UserSession save(UserSession session) throws Exception;
    Optional<UserSession> findById(String sessionId) throws Exception;
    List<UserSession> findActiveByUserId(String userId) throws Exception;
    void deactivate(String sessionId) throws Exception;
    void deactivateAll(String userId) throws Exception;
}