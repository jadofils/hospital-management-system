package hospital.management.backend.dto.clinical;

import java.time.LocalDateTime;

public class CreateAppointmentDTO {

    private String        patientId;
    private String        doctorId;
    private LocalDateTime appointmentDate;
    private String        reason;

    public CreateAppointmentDTO() {}

    public CreateAppointmentDTO(String patientId, String doctorId,
                                LocalDateTime appointmentDate, String reason) {
        this.patientId       = patientId;
        this.doctorId        = doctorId;
        this.appointmentDate = appointmentDate;
        this.reason          = reason;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDateTime appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "CreateAppointmentDTO{patientId='" + patientId + "', doctorId='" + doctorId + "'}";
    }
}