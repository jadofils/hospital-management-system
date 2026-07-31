package hospital.management.backend.dao.patient;

import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.Optional;

public class PatientDAOImpl implements PatientDAO {

    @Override
    public Patient save(Patient patient) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Patient> findById(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Patient> findByEmail(String email) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<Patient> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<Patient> search(String query, PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Patient update(Patient patient) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}