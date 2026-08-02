package hospital.management.backend.dao.department.interfaces;

import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public interface DoctorDAO {
    Doctor save(Doctor doctor) throws Exception;
    Optional<Doctor> findById(String doctorId) throws Exception;
    Optional<Doctor> findByEmail(String email) throws Exception;
    PageResult<Doctor> findAll(PageRequest request) throws Exception;
    List<Doctor> findByDepartmentId(String departmentId) throws Exception;
    Doctor update(Doctor doctor) throws Exception;
    void softDelete(String doctorId) throws Exception;
}