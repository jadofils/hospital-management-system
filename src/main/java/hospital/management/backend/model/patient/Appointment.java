package hospital.management.backend.model.patient;

import hospital.management.backend.model.base.BaseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Appointment extends BaseEntity {

    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String        patientId;
    private String        doctorId;
    private LocalDateTime appointmentDate;
    private String        status;
    private String        reason;

    public Appointment() {}

    public Appointment(String appointmentId, String patientId, String doctorId,
                       LocalDateTime appointmentDate, String status, String reason,
                       LocalDateTime createdAt, LocalDateTime updatedAt,
                       LocalDateTime deletedAt) {
        super(appointmentId);
        this.patientId       = patientId;
        this.doctorId        = doctorId;
        this.appointmentDate = appointmentDate;
        this.status          = status;
        this.reason          = reason;
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
        setDeletedAt(deletedAt);
    }

    // ── BaseEntity contracts ──────────────────────────────────────────────────

    @Override
    public String getEntityType() { return "appointment"; }

    @Override
    public String getSummary() {
        String date = appointmentDate != null ? appointmentDate.format(DISPLAY_FMT) : "?";
        return "Appointment[" + status + "] — " + date;
    }

    // ── Domain alias for ID ───────────────────────────────────────────────────

    public String getAppointmentId() { return getId(); }
    public void setAppointmentId(String id) { setId(id); }

    // ── Appointment-specific fields ───────────────────────────────────────────

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
}