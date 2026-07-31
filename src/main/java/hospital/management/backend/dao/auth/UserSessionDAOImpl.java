package hospital.management.backend.dao.auth;

import hospital.management.backend.dao.auth.interfaces.UserSessionDAO;
import hospital.management.backend.model.user.UserSession;

import java.util.List;
import java.util.Optional;

public class UserSessionDAOImpl implements UserSessionDAO {

    @Override
    public UserSession save(UserSession session) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<UserSession> findById(String sessionId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<UserSession> findActiveByUserId(String userId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deactivate(String sessionId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deactivateAll(String userId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}