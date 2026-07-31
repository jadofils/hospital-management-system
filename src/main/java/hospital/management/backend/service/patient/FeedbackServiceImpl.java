package hospital.management.backend.service.patient;

import hospital.management.backend.dao.patient.interfaces.PatientFeedbackDAO;
import hospital.management.backend.dto.patient.CreatePatientFeedbackDTO;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.service.patient.interfaces.FeedbackService;

import java.util.List;

public class FeedbackServiceImpl implements FeedbackService {

    private final PatientFeedbackDAO feedbackDAO;

    public FeedbackServiceImpl(PatientFeedbackDAO feedbackDAO) {
        this.feedbackDAO = feedbackDAO;
    }

    @Override
    public PatientFeedbackDTO submit(CreatePatientFeedbackDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<PatientFeedbackDTO> findByPatient(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String feedbackId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}