package hospital.management.backend.service.patient;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.dao.patient.interfaces.PatientFeedbackDAO;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.mapper.patient.PatientFeedbackMapper;
import hospital.management.backend.model.patient.PatientFeedback;
import hospital.management.backend.service.log.ServiceAudit;
import hospital.management.backend.service.patient.interfaces.PatientFeedbackService;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for patient feedback
 */
public class PatientFeedbackServiceImpl implements PatientFeedbackService {
    
    private static final AppLogger logger = AppLogger.getLogger(PatientFeedbackServiceImpl.class);
    private final PatientFeedbackDAO feedbackDAO;
    
    public PatientFeedbackServiceImpl(PatientFeedbackDAO feedbackDAO) {
        this.feedbackDAO = feedbackDAO;
    }
    
    @Override
    public PatientFeedbackDTO submitFeedback(PatientFeedbackDTO dto) throws Exception {
        logger.info("Submitting feedback from user: " + dto.getSubmittedBy());
        
        // Convert DTO to entity manually
        PatientFeedback feedback = new PatientFeedback();
        feedback.setSubmittedBy(dto.getSubmittedBy());
        feedback.setPatientId(dto.getPatientId());
        feedback.setAppointmentId(dto.getAppointmentId());
        feedback.setRating(dto.getRating());
        feedback.setComments(dto.getComments());
        feedback.setDateSubmitted(dto.getDateSubmitted());
        
        PatientFeedback saved = feedbackDAO.save(feedback);
        PatientFeedbackDTO result = PatientFeedbackMapper.toDTO(saved);
        
        // Publish event for notifications
        EventBus.publish(AppEventType.PATIENT_FEEDBACK_SUBMITTED, result.getFeedbackId());
        
        ServiceAudit.record("patient_feedback", "create", result.getFeedbackId());
        logger.info("Feedback submitted successfully: " + result.getFeedbackId());
        
        return result;
    }
    
    @Override
    public PatientFeedbackDTO findById(String feedbackId) throws Exception {
        logger.info("Finding feedback: " + feedbackId);
        return feedbackDAO.findById(feedbackId)
            .map(PatientFeedbackMapper::toDTO)
            .orElseThrow(() -> new Exception("Feedback not found: " + feedbackId));
    }
    
    @Override
    public List<PatientFeedbackDTO> findAll() throws Exception {
        logger.info("Finding all feedback");
        return feedbackDAO.findAll().stream()
            .map(PatientFeedbackMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PatientFeedbackDTO> findByPatientId(String patientId) throws Exception {
        logger.info("Finding feedback for patient: " + patientId);
        return feedbackDAO.findByPatientId(patientId).stream()
            .map(PatientFeedbackMapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PatientFeedbackDTO> findByAppointmentId(String appointmentId) throws Exception {
        logger.info("Finding feedback for appointment: " + appointmentId);
        return feedbackDAO.findByAppointmentId(appointmentId).stream()
            .map(PatientFeedbackMapper::toDTO)
            .collect(Collectors.toList());
    }
}
