package hospital.management.backend.dto.clinical;

import java.time.LocalDateTime;

public class AppointmentSummaryDTO {

    private String        appointmentId;
    private String        patientName;
    private String        doctorName;
    private LocalDateTime appointmentDate;
    private String        status;

    public AppointmentSummaryDTO() {}

    public AppointmentSummaryDTO(String appointmentId, String patientName,
                                 String doctorName, LocalDateTime appointmentDate,
                                 String status) {
        this.appointmentId   = appointmentId;
        this.patientName     = patientName;
        this.doctorName      = doctorName;
        this.appointmentDate = appointmentDate;
        this.status          = status;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public LocalDateTime getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDateTime appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "AppointmentSummaryDTO{appointmentId='" + appointmentId + "', status='" + status + "'}";
    }
}