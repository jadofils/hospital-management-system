package hospital.management.backend.dto.doctor;

import java.time.LocalDateTime;

public class DepartmentDTO {

    private String        departmentId;
    private String        name;
    private String        location;
    private String        phone;
    private LocalDateTime createdAt;

    public DepartmentDTO() {}

    public DepartmentDTO(String departmentId, String name, String location,
                         String phone, LocalDateTime createdAt) {
        this.departmentId = departmentId;
        this.name         = name;
        this.location     = location;
        this.phone        = phone;
        this.createdAt    = createdAt;
    }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "DepartmentDTO{departmentId='" + departmentId + "', name='" + name + "'}";
    }
}