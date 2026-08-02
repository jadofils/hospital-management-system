package hospital.management.backend.dto.auth;

public class CreateUserDTO {

    private String  doctorId;
    private String  username;
    private String  password;
    private String  email;

    public CreateUserDTO() {}

    public CreateUserDTO(String doctorId, String username, String password, String email) {
        this.doctorId  = doctorId;
        this.username  = username;
        this.password  = password;
        this.email     = email;
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "CreateUserDTO{username='" + username + "', email='" + email + "'}";
    }
}