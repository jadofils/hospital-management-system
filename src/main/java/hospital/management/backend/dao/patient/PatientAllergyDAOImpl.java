package hospital.management.backend.dao.patient;

import hospital.management.backend.dao.patient.interfaces.PatientAllergyDAO;
import hospital.management.backend.model.patient.PatientAllergy;

import java.util.List;
import java.util.Optional;

public class PatientAllergyDAOImpl implements PatientAllergyDAO {

    @Override
    public PatientAllergy save(PatientAllergy allergy) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<PatientAllergy> findById(String allergyId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<PatientAllergy> findByPatientId(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String allergyId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}