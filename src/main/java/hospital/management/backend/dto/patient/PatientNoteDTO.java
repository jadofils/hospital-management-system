package hospital.management.backend.dto.patient;

import java.time.LocalDateTime;

public class PatientNoteDTO {

    private String noteId;
    private String patientId;
    private String appointmentId;
    private String authorUserId;
    private String authorRole;
    private String noteText;
    private String source;
    private LocalDateTime createdAt;

    public PatientNoteDTO() {}

    public PatientNoteDTO(String noteId, String patientId, String appointmentId,
                          String authorUserId, String authorRole, String noteText,
                          String source, LocalDateTime createdAt) {
        this.noteId        = noteId;
        this.patientId     = patientId;
        this.appointmentId = appointmentId;
        this.authorUserId  = authorUserId;
        this.authorRole    = authorRole;
        this.noteText      = noteText;
        this.source        = source;
        this.createdAt     = createdAt;
    }

    public String getNoteId()        { return noteId; }
    public void setNoteId(String v)  { this.noteId = v; }

    public String getPatientId()        { return patientId; }
    public void setPatientId(String v)  { this.patientId = v; }

    public String getAppointmentId()        { return appointmentId; }
    public void setAppointmentId(String v)  { this.appointmentId = v; }

    public String getAuthorUserId()        { return authorUserId; }
    public void setAuthorUserId(String v)  { this.authorUserId = v; }

    public String getAuthorRole()        { return authorRole; }
    public void setAuthorRole(String v)  { this.authorRole = v; }

    public String getNoteText()        { return noteText; }
    public void setNoteText(String v)  { this.noteText = v; }

    public String getSource()        { return source; }
    public void setSource(String v)  { this.source = v; }

    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime v)  { this.createdAt = v; }
}