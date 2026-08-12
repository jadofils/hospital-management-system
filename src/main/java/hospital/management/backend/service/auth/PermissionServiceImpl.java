package hospital.management.backend.service.auth;

import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.auth.interfaces.PermissionDAO;
import hospital.management.backend.dto.auth.CreatePermissionDTO;
import hospital.management.backend.dto.auth.PermissionDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.auth.PermissionMapper;
import hospital.management.backend.model.user.Permission;
import hospital.management.backend.service.auth.interfaces.PermissionService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PermissionServiceImpl implements PermissionService {

    private final PermissionDAO permissionDAO;

    public PermissionServiceImpl(PermissionDAO permissionDAO) {
        this.permissionDAO = permissionDAO;
    }

    @Override
    public PermissionDTO create(CreatePermissionDTO dto) throws Exception {
        String resource = ValidatorUtils.requireNonBlank(dto.getResource(), "resource");
        ValidatorUtils.requireMaxLength(resource, 50, "resource");
        String action   = ValidatorUtils.requireNonBlank(dto.getAction(), "action");
        ValidatorUtils.requireMaxLength(action, 50, "action");

        if (permissionDAO.findByResourceAndAction(resource, action).isPresent()) {
            throw new ValidationException("action",
                "Permission \"" + action + "\" on \"" + resource + "\" already exists.");
        }
        CacheService.evict(CacheKey.permissionList());
        Permission saved = permissionDAO.save(PermissionMapper.toEntity(dto));
        EventBus.publish(AppEventType.PERMISSION_CREATED, saved.getPermissionId());
        return PermissionMapper.toDTO(saved);
    }

    @Override
    public PermissionDTO findById(String permissionId) throws Exception {
        Optional<PermissionDTO> cached = CacheService.get(CacheKey.permission(permissionId), PermissionDTO.class);
        if (cached.isPresent()) return cached.get();

        Permission permission = permissionDAO.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId));
        PermissionDTO dto = PermissionMapper.toDTO(permission);
        CacheService.set(CacheKey.permission(permissionId), dto, CacheDomain.ROLE);
        return dto;
    }

    @Override
    public List<PermissionDTO> findAll() throws Exception {
        List<PermissionDTO> dtos = new ArrayList<>();
        for (Permission permission : permissionDAO.findAll()) dtos.add(PermissionMapper.toDTO(permission));
        return dtos;
    }

    @Override
    public void delete(String permissionId) throws Exception {
        CacheService.evict(CacheKey.permission(permissionId));
        CacheService.evict(CacheKey.permissionList());
        permissionDAO.softDelete(permissionId);
        EventBus.publish(AppEventType.PERMISSION_DELETED, permissionId);
    }
}
