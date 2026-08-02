package hospital.management.backend.mapper.patient;

import hospital.management.backend.dto.patient.CreatePatientFeedbackDTO;
import hospital.management.backend.dto.patient.PatientFeedbackDTO;
import hospital.management.backend.model.patient.PatientFeedback;

public class PatientFeedbackMapper {

    public static PatientFeedbackDTO toDTO(PatientFeedback f) {
        if (f == null) return null;
        return new PatientFeedbackDTO(
            f.getFeedbackId(),
            f.getPatientId(),
            f.getAppointmentId(),
            f.getRating(),
            f.getComments(),
            f.getDateSubmitted(),
            f.getCreatedAt()
        );
    }

    public static PatientFeedback toEntity(CreatePatientFeedbackDTO dto) {
        if (dto == null) return null;
        PatientFeedback f = new PatientFeedback();
        f.setPatientId(dto.getPatientId());
        f.setAppointmentId(dto.getAppointmentId());
        f.setRating(dto.getRating());
        f.setComments(dto.getComments());
        f.setDateSubmitted(dto.getDateSubmitted());
        return f;
    }
}