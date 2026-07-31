package hospital.management.backend.service.auth;

import hospital.management.backend.dao.auth.interfaces.RoleDAO;
import hospital.management.backend.dao.auth.interfaces.UserRoleDAO;
import hospital.management.backend.dto.auth.CreateRoleDTO;
import hospital.management.backend.dto.auth.RoleDTO;
import hospital.management.backend.service.auth.interfaces.RoleService;

import java.util.List;

public class RoleServiceImpl implements RoleService {

    private final RoleDAO     roleDAO;
    private final UserRoleDAO userRoleDAO;

    public RoleServiceImpl(RoleDAO roleDAO, UserRoleDAO userRoleDAO) {
        this.roleDAO     = roleDAO;
        this.userRoleDAO = userRoleDAO;
    }

    @Override
    public RoleDTO create(CreateRoleDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public RoleDTO findById(String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<RoleDTO> findAll() throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void assignToUser(String userId, String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void revokeFromUser(String userId, String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String roleId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}