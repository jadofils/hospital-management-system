package hospital.management.backend.dao.patient;

import hospital.management.backend.dao.patient.interfaces.PatientFeedbackDAO;
import hospital.management.backend.model.patient.PatientFeedback;

import java.util.List;
import java.util.Optional;

public class PatientFeedbackDAOImpl implements PatientFeedbackDAO {

    @Override
    public PatientFeedback save(PatientFeedback feedback) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<PatientFeedback> findById(String feedbackId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<PatientFeedback> findByPatientId(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<PatientFeedback> findByAppointmentId(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String feedbackId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}