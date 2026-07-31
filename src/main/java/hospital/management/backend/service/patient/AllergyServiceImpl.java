package hospital.management.backend.service.patient;

import hospital.management.backend.dao.patient.interfaces.PatientAllergyDAO;
import hospital.management.backend.dto.patient.CreatePatientAllergyDTO;
import hospital.management.backend.dto.patient.PatientAllergyDTO;
import hospital.management.backend.service.patient.interfaces.AllergyService;

import java.util.List;

public class AllergyServiceImpl implements AllergyService {

    private final PatientAllergyDAO allergyDAO;

    public AllergyServiceImpl(PatientAllergyDAO allergyDAO) {
        this.allergyDAO = allergyDAO;
    }

    @Override
    public PatientAllergyDTO add(CreatePatientAllergyDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<PatientAllergyDTO> findByPatient(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String allergyId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}