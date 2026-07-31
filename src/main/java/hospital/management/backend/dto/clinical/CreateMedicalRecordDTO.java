package hospital.management.backend.dto.clinical;

public class CreateMedicalRecordDTO {

    private String appointmentId;
    private String diagnosis;
    private String symptoms;
    private String notes;

    public CreateMedicalRecordDTO() {}

    public CreateMedicalRecordDTO(String appointmentId, String diagnosis,
                                  String symptoms, String notes) {
        this.appointmentId = appointmentId;
        this.diagnosis     = diagnosis;
        this.symptoms      = symptoms;
        this.notes         = notes;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "CreateMedicalRecordDTO{appointmentId='" + appointmentId + "'}";
    }
}