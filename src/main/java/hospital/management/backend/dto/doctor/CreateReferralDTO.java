package hospital.management.backend.dto.doctor;

public class CreateReferralDTO {

    private String appointmentId;
    private String referringDoctorId;
    private String referredToDoctorId;
    private String reason;

    public CreateReferralDTO() {}

    public CreateReferralDTO(String appointmentId, String referringDoctorId,
                             String referredToDoctorId, String reason) {
        this.appointmentId      = appointmentId;
        this.referringDoctorId  = referringDoctorId;
        this.referredToDoctorId = referredToDoctorId;
        this.reason             = reason;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getReferringDoctorId() { return referringDoctorId; }
    public void setReferringDoctorId(String referringDoctorId) { this.referringDoctorId = referringDoctorId; }

    public String getReferredToDoctorId() { return referredToDoctorId; }
    public void setReferredToDoctorId(String referredToDoctorId) { this.referredToDoctorId = referredToDoctorId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "CreateReferralDTO{appointmentId='" + appointmentId + "'}";
    }
}