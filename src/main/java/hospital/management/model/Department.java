package hospital.management.model;

import java.time.LocalDateTime;

/**
 * Entity model for the `departments` table.
 * Every column in the schema is represented as a field below.
 */
public class Department {

    /** PK */
    private Integer departmentId;
    /** NOT NULL, UNIQUE */
    private String name;
    private String location;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** soft delete marker, null = active */
    private LocalDateTime deletedAt;

    public Department() {
    }

    public Department(Integer departmentId, String name, String location, String phone, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.departmentId = departmentId;
        this.name = name;
        this.location = location;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "Department{" + "departmentId=" + departmentId + ", " + "name=" + name + ", " + "location=" + location + ", " + "phone=" + phone + ", " + "createdAt=" + createdAt + ", " + "updatedAt=" + updatedAt + ", " + "deletedAt=" + deletedAt + "}";
    }
}