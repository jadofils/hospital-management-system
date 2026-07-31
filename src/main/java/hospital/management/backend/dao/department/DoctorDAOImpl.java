package hospital.management.backend.dao.department;

import hospital.management.backend.dao.department.interfaces.DoctorDAO;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public class DoctorDAOImpl implements DoctorDAO {

    @Override
    public Doctor save(Doctor doctor) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Doctor> findById(String doctorId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Doctor> findByEmail(String email) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<Doctor> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Doctor> findByDepartmentId(String departmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Doctor update(Doctor doctor) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String doctorId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}