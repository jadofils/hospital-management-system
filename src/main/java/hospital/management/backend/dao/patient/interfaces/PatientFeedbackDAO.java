package hospital.management.backend.dao.patient.interfaces;

import hospital.management.backend.model.patient.PatientFeedback;

import java.util.List;
import java.util.Optional;

public interface PatientFeedbackDAO {
    PatientFeedback save(PatientFeedback feedback) throws Exception;
    Optional<PatientFeedback> findById(String feedbackId) throws Exception;
    List<PatientFeedback> findAll() throws Exception;
    List<PatientFeedback> findByPatientId(String patientId) throws Exception;
    List<PatientFeedback> findByAppointmentId(String appointmentId) throws Exception;
    void softDelete(String feedbackId) throws Exception;
}