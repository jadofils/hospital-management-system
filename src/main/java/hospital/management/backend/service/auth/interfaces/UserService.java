package hospital.management.backend.service.auth.interfaces;

import hospital.management.backend.dto.auth.CreateUserDTO;
import hospital.management.backend.dto.auth.UpdateUserDTO;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

public interface UserService {
    UserDTO create(CreateUserDTO dto) throws Exception;
    UserDTO findById(String userId) throws Exception;
    PageResult<UserDTO> findAll(PageRequest request) throws Exception;
    UserDTO update(UpdateUserDTO dto) throws Exception;
    void deactivate(String userId) throws Exception;
    void delete(String userId) throws Exception;
}