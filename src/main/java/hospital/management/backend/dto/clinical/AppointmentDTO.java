package hospital.management.backend.dto.clinical;

import java.time.LocalDateTime;

public class AppointmentDTO {

    private String        appointmentId;
    private String        patientId;
    private String        doctorId;
    private LocalDateTime appointmentDate;
    private String        status;
    private String        reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AppointmentDTO() {}

    public AppointmentDTO(String appointmentId, String patientId, String doctorId,
                          LocalDateTime appointmentDate, String status, String reason,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.appointmentId   = appointmentId;
        this.patientId       = patientId;
        this.doctorId        = doctorId;
        this.appointmentDate = appointmentDate;
        this.status          = status;
        this.reason          = reason;
        this.createdAt       = createdAt;
        this.updatedAt       = updatedAt;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDateTime appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "AppointmentDTO{appointmentId='" + appointmentId + "', status='" + status + "'}";
    }
}