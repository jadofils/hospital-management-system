package hospital.management.backend.service.department;

import hospital.management.backend.dao.department.interfaces.DepartmentDAO;
import hospital.management.backend.dto.doctor.CreateDepartmentDTO;
import hospital.management.backend.dto.doctor.DepartmentDTO;
import hospital.management.backend.service.department.interfaces.DepartmentService;

import java.util.List;

public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentDAO departmentDAO;

    public DepartmentServiceImpl(DepartmentDAO departmentDAO) {
        this.departmentDAO = departmentDAO;
    }

    @Override
    public DepartmentDTO create(CreateDepartmentDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public DepartmentDTO findById(String departmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<DepartmentDTO> findAll() throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public DepartmentDTO update(String departmentId, CreateDepartmentDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String departmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}