package hospital.management.backend.dto.patient;

import java.time.LocalDate;

public class CreatePatientFeedbackDTO {

    private String    patientId;
    private String    appointmentId;
    private Integer   rating;
    private String    comments;
    private LocalDate dateSubmitted;

    public CreatePatientFeedbackDTO() {}

    public CreatePatientFeedbackDTO(String patientId, String appointmentId,
                                    Integer rating, String comments, LocalDate dateSubmitted) {
        this.patientId     = patientId;
        this.appointmentId = appointmentId;
        this.rating        = rating;
        this.comments      = comments;
        this.dateSubmitted = dateSubmitted;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDate getDateSubmitted() { return dateSubmitted; }
    public void setDateSubmitted(LocalDate dateSubmitted) { this.dateSubmitted = dateSubmitted; }

    @Override
    public String toString() {
        return "CreatePatientFeedbackDTO{patientId='" + patientId + "', rating=" + rating + "}";
    }
}