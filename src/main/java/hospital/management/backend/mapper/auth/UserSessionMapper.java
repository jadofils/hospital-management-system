package hospital.management.backend.mapper.auth;

import hospital.management.backend.dto.auth.UserSessionDTO;
import hospital.management.backend.model.user.UserSession;

public class UserSessionMapper {

    public static UserSessionDTO toDTO(UserSession session) {
        if (session == null) return null;
        return new UserSessionDTO(
            session.getSessionId(),
            session.getUserId(),
            session.getLoginAt(),
            session.getLogoutAt(),
            session.getExpiresAt(),
            session.getIpAddress(),
            session.getUserAgent(),
            session.isIsActive()
        );
    }

    public static UserSession toEntity(UserSessionDTO dto) {
        if (dto == null) return null;
        UserSession s = new UserSession();
        s.setSessionId(dto.getSessionId());
        s.setUserId(dto.getUserId());
        s.setLoginAt(dto.getLoginAt());
        s.setLogoutAt(dto.getLogoutAt());
        s.setExpiresAt(dto.getExpiresAt());
        s.setIpAddress(dto.getIpAddress());
        s.setUserAgent(dto.getUserAgent());
        s.setIsActive(dto.getIsActive());
        return s;
    }
}