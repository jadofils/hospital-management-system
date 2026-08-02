package hospital.management.backend.dto.doctor;

public class CreateDepartmentDTO {

    private String name;
    private String location;
    private String phone;

    public CreateDepartmentDTO() {}

    public CreateDepartmentDTO(String name, String location, String phone) {
        this.name     = name;
        this.location = location;
        this.phone    = phone;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return "CreateDepartmentDTO{name='" + name + "'}";
    }
}