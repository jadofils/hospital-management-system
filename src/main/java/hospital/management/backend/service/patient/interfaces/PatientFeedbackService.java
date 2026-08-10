package hospital.management.backend.service.patient.interfaces;

import hospital.management.backend.dto.patient.PatientFeedbackDTO;

import java.util.List;

/**
 * Service for managing patient feedback
 */
public interface PatientFeedbackService {
    
    /**
     * Submit new patient feedback
     */
    PatientFeedbackDTO submitFeedback(PatientFeedbackDTO feedback) throws Exception;
    
    /**
     * Get feedback by ID
     */
    PatientFeedbackDTO findById(String feedbackId) throws Exception;
    
    /**
     * Get all feedback (for admin/doctor view)
     */
    List<PatientFeedbackDTO> findAll() throws Exception;
    
    /**
     * Get feedback for a specific patient
     */
    List<PatientFeedbackDTO> findByPatientId(String patientId) throws Exception;
    
    /**
     * Get feedback for a specific appointment
     */
    List<PatientFeedbackDTO> findByAppointmentId(String appointmentId) throws Exception;
}
