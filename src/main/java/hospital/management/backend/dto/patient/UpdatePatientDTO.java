package hospital.management.backend.dto.patient;

public class UpdatePatientDTO {

    private String patientId;
    private String phone;
    private String email;
    private String address;

    public UpdatePatientDTO() {}

    public UpdatePatientDTO(String patientId, String phone, String email, String address) {
        this.patientId = patientId;
        this.phone     = phone;
        this.email     = email;
        this.address   = address;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return "UpdatePatientDTO{patientId='" + patientId + "'}";
    }
}