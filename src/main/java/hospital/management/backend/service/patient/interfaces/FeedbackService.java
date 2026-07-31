package hospital.management.backend.service.patient.interfaces;

import hospital.management.backend.dto.patient.CreatePatientFeedbackDTO;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;

import java.util.List;

public interface FeedbackService {
    PatientFeedbackDTO submit(CreatePatientFeedbackDTO dto) throws Exception;
    List<PatientFeedbackDTO> findByPatient(String patientId) throws Exception;
    void delete(String feedbackId) throws Exception;
}