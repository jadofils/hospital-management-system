package hospital.management.backend.dao.department.interfaces;

import hospital.management.backend.model.doctor.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentDAO {
    Department save(Department department) throws Exception;
    Optional<Department> findById(String departmentId) throws Exception;
    Optional<Department> findByName(String name) throws Exception;
    List<Department> findAll() throws Exception;
    Department update(Department department) throws Exception;
    void softDelete(String departmentId) throws Exception;
}