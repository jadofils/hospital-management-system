package hospital.management.backend.service.auth;

import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.config.security.PasswordConfig;
import hospital.management.backend.dao.auth.interfaces.UserDAO;
import hospital.management.backend.dto.auth.CreateUserDTO;
import hospital.management.backend.dto.auth.UpdateUserDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.auth.UserMapper;
import hospital.management.backend.model.user.User;
import hospital.management.backend.service.auth.interfaces.UserService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.Optional;

public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;

    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public UserDTO create(CreateUserDTO dto) throws Exception {
        String username = ValidatorUtils.requireNonBlank(dto.getUsername(), "username");
        ValidatorUtils.requireMinLength(username, 3, "username");
        ValidatorUtils.requireMaxLength(username, 50, "username");
        ValidatorUtils.requireNonBlank(dto.getPassword(), "password");
        ValidatorUtils.requireMinLength(dto.getPassword(), 8, "password");

        if (userDAO.existsByUsername(username)) {
            throw new ValidationException("username", "Username \"" + username + "\" is already taken.");
        }
        if (dto.getEmail() != null && userDAO.existsByEmail(dto.getEmail())) {
            throw new ValidationException("email", "Email \"" + dto.getEmail() + "\" is already registered.");
        }

        User user = UserMapper.toEntity(dto);
        user.setPasswordHash(PasswordConfig.hash(dto.getPassword()));
        User saved = userDAO.save(user);
        EventBus.publish(AppEventType.USER_CREATED, saved.getUserId());
        return UserMapper.toDTO(saved);
    }

    @Override
    public UserDTO findById(String userId) throws Exception {
        Optional<UserDTO> cached = CacheService.get(CacheKey.user(userId), UserDTO.class);
        if (cached.isPresent()) return cached.get();

        User user = userDAO.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        UserDTO dto = UserMapper.toDTO(user);
        CacheService.set(CacheKey.user(userId), dto, CacheDomain.USER);
        return dto;
    }

    @Override
    public PageResult<UserDTO> findAll(PageRequest request) throws Exception {
        return userDAO.findAll(request).map(UserMapper::toDTO);
    }

    @Override
    public UserDTO update(UpdateUserDTO dto) throws Exception {
        User user = userDAO.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", dto.getUserId()));

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())
                && userDAO.existsByEmail(dto.getEmail())) {
            throw new ValidationException("email", "Email \"" + dto.getEmail() + "\" is already registered.");
        }

        user.setEmail(dto.getEmail());
        user.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : user.getIsActive());

        // Delete-before-write: evict first so a concurrent reader never sees a stale hit.
        CacheService.evict(CacheKey.user(user.getUserId()));
        User saved = userDAO.update(user);
        EventBus.publish(AppEventType.USER_UPDATED, saved.getUserId());
        return UserMapper.toDTO(saved);
    }

    @Override
    public void deactivate(String userId) throws Exception {
        User user = userDAO.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setIsActive(false);
        CacheService.evict(CacheKey.user(userId));
        userDAO.update(user);
        EventBus.publish(AppEventType.USER_UPDATED, userId);
    }

    @Override
    public void delete(String userId) throws Exception {
        CacheService.evict(CacheKey.user(userId));
        userDAO.softDelete(userId);
        EventBus.publish(AppEventType.USER_DELETED, userId);
    }
}
