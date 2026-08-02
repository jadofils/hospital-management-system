package hospital.management.backend.mapper.auth;

import hospital.management.backend.dto.auth.CreateUserDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.model.user.User;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        if (user == null) return null;
        return new UserDTO(
            user.getUserId(),
            user.getDoctorId(),
            user.getUsername(),
            user.getEmail(),
            user.getIsActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public static User toEntity(CreateUserDTO dto) {
        if (dto == null) return null;
        User user = new User();
        user.setDoctorId(dto.getDoctorId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setIsActive(true);
        return user;
    }
}