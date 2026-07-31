package hospital.management.backend.dao.department;

import hospital.management.backend.dao.department.interfaces.DepartmentDAO;
import hospital.management.backend.model.doctor.Department;

import java.util.List;
import java.util.Optional;

public class DepartmentDAOImpl implements DepartmentDAO {

    @Override
    public Department save(Department department) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Department> findById(String departmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Department> findByName(String name) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Department> findAll() throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Department update(Department department) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String departmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}