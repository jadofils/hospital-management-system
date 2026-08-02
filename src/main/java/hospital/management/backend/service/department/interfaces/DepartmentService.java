package hospital.management.backend.service.department.interfaces;

import hospital.management.backend.dto.doctor.CreateDepartmentDTO;
import hospital.management.backend.dto.doctor.DepartmentDTO;

import java.util.List;

public interface DepartmentService {
    DepartmentDTO create(CreateDepartmentDTO dto) throws Exception;
    DepartmentDTO findById(String departmentId) throws Exception;
    List<DepartmentDTO> findAll() throws Exception;
    DepartmentDTO update(String departmentId, CreateDepartmentDTO dto) throws Exception;
    void delete(String departmentId) throws Exception;
}