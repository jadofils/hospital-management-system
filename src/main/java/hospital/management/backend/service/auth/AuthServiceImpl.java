package hospital.management.backend.service.auth;

import hospital.management.backend.dao.auth.interfaces.UserDAO;
import hospital.management.backend.dao.auth.interfaces.UserSessionDAO;
import hospital.management.backend.dto.auth.LoginRequestDTO;
import hospital.management.backend.dto.auth.LoginResponseDTO;
import hospital.management.backend.service.auth.interfaces.AuthService;

public class AuthServiceImpl implements AuthService {

    private final UserDAO        userDAO;
    private final UserSessionDAO userSessionDAO;

    public AuthServiceImpl(UserDAO userDAO, UserSessionDAO userSessionDAO) {
        this.userDAO        = userDAO;
        this.userSessionDAO = userSessionDAO;
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void logout(String sessionId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}