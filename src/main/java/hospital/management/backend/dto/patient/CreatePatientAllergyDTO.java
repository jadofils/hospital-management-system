package hospital.management.backend.dto.patient;

public class CreatePatientAllergyDTO {

    private String patientId;
    private String allergen;
    private String reaction;
    private String severity;

    public CreatePatientAllergyDTO() {}

    public CreatePatientAllergyDTO(String patientId, String allergen,
                                   String reaction, String severity) {
        this.patientId = patientId;
        this.allergen  = allergen;
        this.reaction  = reaction;
        this.severity  = severity;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getAllergen() { return allergen; }
    public void setAllergen(String allergen) { this.allergen = allergen; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    @Override
    public String toString() {
        return "CreatePatientAllergyDTO{patientId='" + patientId + "', allergen='" + allergen + "'}";
    }
}