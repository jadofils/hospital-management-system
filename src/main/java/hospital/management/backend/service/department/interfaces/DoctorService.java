package hospital.management.backend.service.department.interfaces;

import hospital.management.backend.dto.doctor.CreateDoctorDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.dto.doctor.DoctorSummaryDTO;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;

public interface DoctorService {
    DoctorDTO create(CreateDoctorDTO dto) throws Exception;
    DoctorDTO findById(String doctorId) throws Exception;
    DoctorDTO findByEmail(String email) throws Exception;
    PageResult<DoctorDTO> findAll(PageRequest request) throws Exception;
    List<DoctorSummaryDTO> findByDepartment(String departmentId) throws Exception;
    DoctorDTO update(String doctorId, CreateDoctorDTO dto) throws Exception;
    void delete(String doctorId) throws Exception;
}