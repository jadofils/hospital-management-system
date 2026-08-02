package hospital.management.backend.dto.doctor;

import java.time.LocalDateTime;

public class ReferralDTO {

    private String        referralId;
    private String        appointmentId;
    private String        referringDoctorId;
    private String        referredToDoctorId;
    private String        reason;
    private String        status;
    private LocalDateTime createdAt;

    public ReferralDTO() {}

    public ReferralDTO(String referralId, String appointmentId, String referringDoctorId,
                       String referredToDoctorId, String reason, String status,
                       LocalDateTime createdAt) {
        this.referralId         = referralId;
        this.appointmentId      = appointmentId;
        this.referringDoctorId  = referringDoctorId;
        this.referredToDoctorId = referredToDoctorId;
        this.reason             = reason;
        this.status             = status;
        this.createdAt          = createdAt;
    }

    public String getReferralId() { return referralId; }
    public void setReferralId(String referralId) { this.referralId = referralId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getReferringDoctorId() { return referringDoctorId; }
    public void setReferringDoctorId(String referringDoctorId) { this.referringDoctorId = referringDoctorId; }

    public String getReferredToDoctorId() { return referredToDoctorId; }
    public void setReferredToDoctorId(String referredToDoctorId) { this.referredToDoctorId = referredToDoctorId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ReferralDTO{referralId='" + referralId + "', status='" + status + "'}";
    }
}