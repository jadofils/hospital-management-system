package hospital.management.backend.dao.patient.interfaces;

import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.Optional;

public interface PatientDAO {
    Patient save(Patient patient) throws Exception;
    Optional<Patient> findById(String patientId) throws Exception;
    Optional<Patient> findByEmail(String email) throws Exception;
    PageResult<Patient> findAll(PageRequest request) throws Exception;
    PageResult<Patient> search(String query, PageRequest request) throws Exception;
    Patient update(Patient patient) throws Exception;
    Patient updateStatus(String patientId, String status) throws Exception;
    void softDelete(String patientId) throws Exception;
}