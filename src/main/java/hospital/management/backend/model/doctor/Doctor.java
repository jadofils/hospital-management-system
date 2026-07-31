package hospital.management.backend.model.doctor;

import hospital.management.backend.model.base.Person;

import java.time.LocalDateTime;

public class Doctor extends Person {

    private String departmentId;
    private String specialization;

    public Doctor() {}

    public Doctor(String doctorId, String departmentId, String firstName,
                  String lastName, String specialization, String phone,
                  String email, LocalDateTime createdAt,
                  LocalDateTime updatedAt, LocalDateTime deletedAt) {
        super(doctorId, firstName, lastName, phone, email);
        this.departmentId   = departmentId;
        this.specialization = specialization;
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
        setDeletedAt(deletedAt);
    }

    // ── BaseEntity contracts ──────────────────────────────────────────────────

    @Override
    public String getEntityType() { return "doctor"; }

    @Override
    public String getDisplayTitle() { return "Dr."; }

    @Override
    public String getSummary() {
        String spec = specialization != null ? " | " + specialization : "";
        return "Dr. " + getFullName() + spec;
    }

    // ── Domain alias for ID ───────────────────────────────────────────────────

    public String getDoctorId() { return getId(); }
    public void setDoctorId(String id) { setId(id); }

    // ── Doctor-specific fields ────────────────────────────────────────────────

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
}