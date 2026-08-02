package hospital.management.backend.service.auth.interfaces;

import hospital.management.backend.dto.auth.LoginRequestDTO;
import hospital.management.backend.dto.auth.LoginResponseDTO;
import hospital.management.backend.dto.auth.UserSessionDTO;

import java.util.List;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO request) throws Exception;
    void logout(String sessionId) throws Exception;

    /** Revokes every active session for a user (e.g. "sign out of all devices"). */
    void logoutAllSessions(String userId) throws Exception;

    List<UserSessionDTO> findActiveSessions(String userId) throws Exception;

    void changePassword(String userId, String oldPassword, String newPassword) throws Exception;
}