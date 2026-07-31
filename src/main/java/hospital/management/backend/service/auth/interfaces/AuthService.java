package hospital.management.backend.service.auth.interfaces;

import hospital.management.backend.dto.auth.LoginRequestDTO;
import hospital.management.backend.dto.auth.LoginResponseDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO request) throws Exception;
    void logout(String sessionId) throws Exception;
    void changePassword(String userId, String oldPassword, String newPassword) throws Exception;
}