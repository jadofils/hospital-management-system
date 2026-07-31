package hospital.management.backend.dto.patient;

import java.time.LocalDateTime;

public class PatientAllergyDTO {

    private String        allergyId;
    private String        patientId;
    private String        allergen;
    private String        reaction;
    private String        severity;
    private LocalDateTime createdAt;

    public PatientAllergyDTO() {}

    public PatientAllergyDTO(String allergyId, String patientId, String allergen,
                             String reaction, String severity, LocalDateTime createdAt) {
        this.allergyId = allergyId;
        this.patientId = patientId;
        this.allergen  = allergen;
        this.reaction  = reaction;
        this.severity  = severity;
        this.createdAt = createdAt;
    }

    public String getAllergyId() { return allergyId; }
    public void setAllergyId(String allergyId) { this.allergyId = allergyId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAllergen() { return allergen; }
    public void setAllergen(String allergen) { this.allergen = allergen; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "PatientAllergyDTO{allergyId='" + allergyId + "', allergen='" + allergen + "'}";
    }
}