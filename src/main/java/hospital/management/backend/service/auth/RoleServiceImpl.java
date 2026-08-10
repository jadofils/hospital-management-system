package hospital.management.backend.service.auth;

import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.auth.interfaces.PermissionDAO;
import hospital.management.backend.dao.auth.interfaces.RoleDAO;
import hospital.management.backend.dao.auth.interfaces.RolePermissionDAO;
import hospital.management.backend.dao.auth.interfaces.UserRoleDAO;
import hospital.management.backend.dto.auth.CreateRoleDTO;
import hospital.management.backend.dto.auth.PermissionDTO;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.auth.PermissionMapper;
import hospital.management.backend.mapper.auth.RoleMapper;
import hospital.management.backend.model.user.Permission;
import hospital.management.backend.model.user.Role;
import hospital.management.backend.model.user.RolePermission;
import hospital.management.backend.model.user.UserRole;
import hospital.management.backend.service.auth.interfaces.RoleService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoleServiceImpl implements RoleService {

    private final RoleDAO           roleDAO;
    private final UserRoleDAO       userRoleDAO;
    private final RolePermissionDAO rolePermissionDAO;
    private final PermissionDAO     permissionDAO;

    public RoleServiceImpl(RoleDAO roleDAO, UserRoleDAO userRoleDAO,
                            RolePermissionDAO rolePermissionDAO, PermissionDAO permissionDAO) {
        this.roleDAO           = roleDAO;
        this.userRoleDAO       = userRoleDAO;
        this.rolePermissionDAO = rolePermissionDAO;
        this.permissionDAO     = permissionDAO;
    }

    @Override
    public RoleDTO create(CreateRoleDTO dto) throws Exception {
        String roleName = ValidatorUtils.requireNonBlank(dto.getRoleName(), "roleName");
        if (roleDAO.findByName(roleName).isPresent()) {
            throw new ValidationException("roleName", "Role \"" + roleName + "\" already exists.");
        }
        CacheService.evict(CacheKey.roleList());
        Role saved = roleDAO.save(RoleMapper.toEntity(dto));
        EventBus.publish(AppEventType.ROLE_CREATED, saved.getRoleId());
        return RoleMapper.toDTO(saved);
    }

    @Override
    public RoleDTO findById(String roleId) throws Exception {
        Optional<RoleDTO> cached = CacheService.get(CacheKey.role(roleId), RoleDTO.class);
        if (cached.isPresent()) return cached.get();

        Role role = roleDAO.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        RoleDTO dto = RoleMapper.toDTO(role);
        CacheService.set(CacheKey.role(roleId), dto, CacheDomain.ROLE);
        return dto;
    }

    @Override
    public List<RoleDTO> findAll() throws Exception {
        List<RoleDTO> dtos = new ArrayList<>();
        for (Role role : roleDAO.findAll()) dtos.add(RoleMapper.toDTO(role));
        return dtos;
    }

    @Override
    public void delete(String roleId) throws Exception {
        CacheService.evict(CacheKey.role(roleId));
        CacheService.evict(CacheKey.roleList());
        roleDAO.softDelete(roleId);
        EventBus.publish(AppEventType.ROLE_DELETED, roleId);
    }

    @Override
    public void assignToUser(String userId, String roleId) throws Exception {
        roleDAO.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        CacheService.evict(CacheKey.userRoles(userId));
        userRoleDAO.assign(userId, roleId);
        EventBus.publish(AppEventType.ROLE_UPDATED, roleId);
    }

    @Override
    public void revokeFromUser(String userId, String roleId) throws Exception {
        CacheService.evict(CacheKey.userRoles(userId));
        userRoleDAO.revoke(userId, roleId);
        EventBus.publish(AppEventType.ROLE_UPDATED, roleId);
    }

    @Override
    public List<RoleDTO> findRolesForUser(String userId) throws Exception {
        List<RoleDTO> dtos = new ArrayList<>();
        for (UserRole assignment : userRoleDAO.findByUserId(userId)) {
            roleDAO.findById(assignment.getRoleId()).ifPresent(role -> dtos.add(RoleMapper.toDTO(role)));
        }
        return dtos;
    }

    @Override
    public List<String> findUserIdsForRole(String roleName) throws Exception {
        Optional<Role> role = roleDAO.findByName(roleName);
        if (role.isEmpty()) return List.of();
        List<String> userIds = new ArrayList<>();
        for (UserRole assignment : userRoleDAO.findByRoleId(role.get().getRoleId())) {
            userIds.add(assignment.getUserId());
        }
        return userIds;
    }

    @Override
    public void assignPermission(String roleId, String permissionId) throws Exception {
        permissionDAO.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId));
        CacheService.evict(CacheKey.rolePermissions(roleId));
        rolePermissionDAO.assign(roleId, permissionId);
        EventBus.publish(AppEventType.ROLE_UPDATED, roleId);
    }

    @Override
    public void revokePermission(String roleId, String permissionId) throws Exception {
        CacheService.evict(CacheKey.rolePermissions(roleId));
        rolePermissionDAO.revoke(roleId, permissionId);
        EventBus.publish(AppEventType.ROLE_UPDATED, roleId);
    }

    @Override
    public List<PermissionDTO> findPermissionsForRole(String roleId) throws Exception {
        Optional<List<PermissionDTO>> cached = CacheService.get(
            CacheKey.rolePermissions(roleId),
            new com.fasterxml.jackson.core.type.TypeReference<List<PermissionDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<PermissionDTO> dtos = new ArrayList<>();
        for (RolePermission rp : rolePermissionDAO.findByRoleId(roleId)) {
            Permission permission = permissionDAO.findById(rp.getPermissionId()).orElse(null);
            if (permission != null) dtos.add(PermissionMapper.toDTO(permission));
        }
        CacheService.set(CacheKey.rolePermissions(roleId), dtos, CacheDomain.ROLE);
        return dtos;
    }
}
