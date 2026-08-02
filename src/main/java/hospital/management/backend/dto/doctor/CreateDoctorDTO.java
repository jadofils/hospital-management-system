package hospital.management.backend.dto.doctor;

public class CreateDoctorDTO {

    private String departmentId;
    private String firstName;
    private String lastName;
    private String specialization;
    private String phone;
    private String email;

    public CreateDoctorDTO() {}

    public CreateDoctorDTO(String departmentId, String firstName, String lastName,
                           String specialization, String phone, String email) {
        this.departmentId   = departmentId;
        this.firstName      = firstName;
        this.lastName       = lastName;
        this.specialization = specialization;
        this.phone          = phone;
        this.email          = email;
    }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "CreateDoctorDTO{firstName='" + firstName + "', lastName='" + lastName + "'}";
    }
}