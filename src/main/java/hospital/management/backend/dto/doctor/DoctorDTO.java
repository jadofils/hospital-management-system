package hospital.management.backend.dto.doctor;

import java.time.LocalDateTime;

public class DoctorDTO {

    private String        doctorId;
    private String        departmentId;
    private String        firstName;
    private String        lastName;
    private String        specialization;
    private String        phone;
    private String        email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DoctorDTO() {}

    public DoctorDTO(String doctorId, String departmentId, String firstName,
                     String lastName, String specialization, String phone,
                     String email, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.doctorId       = doctorId;
        this.departmentId   = departmentId;
        this.firstName      = firstName;
        this.lastName       = lastName;
        this.specialization = specialization;
        this.phone          = phone;
        this.email          = email;
        this.createdAt      = createdAt;
        this.updatedAt      = updatedAt;
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return "Dr. " + firstName + " " + lastName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "DoctorDTO{doctorId='" + doctorId + "', name='" + getFullName() + "'}";
    }
}