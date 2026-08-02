package hospital.management.backend.dto.patient;

public class PatientSummaryDTO {

    private String patientId;
    private String fullName;
    private String gender;
    private String phone;
    private String email;

    public PatientSummaryDTO() {}

    public PatientSummaryDTO(String patientId, String fullName, String gender,
                             String phone, String email) {
        this.patientId = patientId;
        this.fullName  = fullName;
        this.gender    = gender;
        this.phone     = phone;
        this.email     = email;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "PatientSummaryDTO{patientId='" + patientId + "', fullName='" + fullName + "'}";
    }
}