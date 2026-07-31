package hospital.management.backend.dao.patient.interfaces;

import hospital.management.backend.model.patient.PatientAllergy;

import java.util.List;
import java.util.Optional;

public interface PatientAllergyDAO {
    PatientAllergy save(PatientAllergy allergy) throws Exception;
    Optional<PatientAllergy> findById(String allergyId) throws Exception;
    List<PatientAllergy> findByPatientId(String patientId) throws Exception;
    void softDelete(String allergyId) throws Exception;
}