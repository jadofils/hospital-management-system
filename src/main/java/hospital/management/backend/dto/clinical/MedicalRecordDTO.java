package hospital.management.backend.dto.clinical;

import java.time.LocalDateTime;

public class MedicalRecordDTO {

    private String        recordId;
    private String        appointmentId;
    private String        diagnosis;
    private String        symptoms;
    private String        notes;
    private LocalDateTime createdAt;

    public MedicalRecordDTO() {}

    public MedicalRecordDTO(String recordId, String appointmentId, String diagnosis,
                            String symptoms, String notes, LocalDateTime createdAt) {
        this.recordId      = recordId;
        this.appointmentId = appointmentId;
        this.diagnosis     = diagnosis;
        this.symptoms      = symptoms;
        this.notes         = notes;
        this.createdAt     = createdAt;
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "MedicalRecordDTO{recordId='" + recordId + "', appointmentId='" + appointmentId + "'}";
    }
}