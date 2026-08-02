package hospital.management.backend.dto.patient;

import java.time.LocalDate;

public class CreatePatientDTO {

    private String    firstName;
    private String    lastName;
    private LocalDate dob;
    private String    gender;
    private String    phone;
    private String    email;
    private String    address;

    public CreatePatientDTO() {}

    public CreatePatientDTO(String firstName, String lastName, LocalDate dob,
                            String gender, String phone, String email, String address) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.dob       = dob;
        this.gender    = gender;
        this.phone     = phone;
        this.email     = email;
        this.address   = address;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

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

    @Override
    public String toString() {
        return "CreatePatientDTO{firstName='" + firstName + "', lastName='" + lastName + "'}";
    }
}