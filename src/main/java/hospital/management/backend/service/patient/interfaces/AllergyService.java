package hospital.management.backend.service.patient.interfaces;

import hospital.management.backend.dto.patient.CreatePatientAllergyDTO;
import hospital.management.backend.dto.patient.PatientAllergyDTO;

import java.util.List;

public interface AllergyService {
    PatientAllergyDTO add(CreatePatientAllergyDTO dto) throws Exception;
    List<PatientAllergyDTO> findByPatient(String patientId) throws Exception;
    void delete(String allergyId) throws Exception;
}