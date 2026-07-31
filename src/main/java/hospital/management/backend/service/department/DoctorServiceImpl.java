package hospital.management.backend.service.department;

import hospital.management.backend.dao.department.interfaces.DepartmentDAO;
import hospital.management.backend.dao.department.interfaces.DoctorDAO;
import hospital.management.backend.dto.doctor.CreateDoctorDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.dto.doctor.DoctorSummaryDTO;
import hospital.management.backend.service.department.interfaces.DoctorService;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public class DoctorServiceImpl implements DoctorService {

    private final DoctorDAO     doctorDAO;
    private final DepartmentDAO departmentDAO;

    public DoctorServiceImpl(DoctorDAO doctorDAO, DepartmentDAO departmentDAO) {
        this.doctorDAO     = doctorDAO;
        this.departmentDAO = departmentDAO;
    }

    @Override
    public DoctorDTO create(CreateDoctorDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public DoctorDTO findById(String doctorId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<DoctorDTO> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<DoctorSummaryDTO> findByDepartment(String departmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public DoctorDTO update(String doctorId, CreateDoctorDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String doctorId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}