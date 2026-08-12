package hospital.management.backend.dto.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PatientDTO {

    private String        patientId;
    private String        firstName;
    private String        lastName;
    private LocalDate     dob;
    private String        gender;
    private String        phone;
    private String        email;
    private String        address;
    private String        status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PatientDTO() {}

    public PatientDTO(String patientId, String firstName, String lastName,
                      LocalDate dob, String gender, String phone, String email,
                      String address, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName  = lastName;
        this.dob       = dob;
        this.gender    = gender;
        this.phone     = phone;
        this.email     = email;
        this.address   = address;
        this.status    = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return firstName + " " + lastName; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "PatientDTO{patientId='" + patientId + "', name='" + getFullName() + "'}";
    }
}