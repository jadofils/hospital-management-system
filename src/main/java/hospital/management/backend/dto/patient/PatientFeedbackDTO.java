package hospital.management.backend.dto.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PatientFeedbackDTO {

    private String        feedbackId;
    private String        submittedBy;    // User who submitted feedback
    private String        patientId;      // Patient feedback is about (nullable)
    private String        appointmentId;
    private Integer       rating;
    private String        comments;
    private LocalDate     dateSubmitted;
    private LocalDateTime createdAt;

    public PatientFeedbackDTO() {}

    public PatientFeedbackDTO(String feedbackId, String submittedBy, String patientId, String appointmentId,
                              Integer rating, String comments, LocalDate dateSubmitted,
                              LocalDateTime createdAt) {
        this.feedbackId    = feedbackId;
        this.submittedBy   = submittedBy;
        this.patientId     = patientId;
        this.appointmentId = appointmentId;
        this.rating        = rating;
        this.comments      = comments;
        this.dateSubmitted = dateSubmitted;
        this.createdAt     = createdAt;
    }

    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "PatientFeedbackDTO{feedbackId='" + feedbackId + "', rating=" + rating + "}";
    }
}