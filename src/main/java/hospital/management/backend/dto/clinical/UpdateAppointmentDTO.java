package hospital.management.backend.dto.clinical;

import java.time.LocalDateTime;

public class UpdateAppointmentDTO {

    private String        appointmentId;
    private LocalDateTime appointmentDate;
    private String        status;
    private String        reason;

    public UpdateAppointmentDTO() {}

    public UpdateAppointmentDTO(String appointmentId, LocalDateTime appointmentDate,
                                String status, String reason) {
        this.appointmentId   = appointmentId;
        this.appointmentDate = appointmentDate;
        this.status          = status;
        this.reason          = reason;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public LocalDateTime getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDateTime appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "UpdateAppointmentDTO{appointmentId='" + appointmentId + "', status='" + status + "'}";
    }
}