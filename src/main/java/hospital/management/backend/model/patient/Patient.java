package hospital.management.backend.model.patient;

import hospital.management.backend.model.base.Person;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Patient extends Person {

    private LocalDate dob;
    private String    gender;
    private String    address;
    private String    status;

    public Patient() {}

    public Patient(String patientId, String firstName, String lastName,
                   LocalDate dob, String gender, String phone, String email,
                   String address, String status, LocalDateTime createdAt,
                   LocalDateTime updatedAt, LocalDateTime deletedAt) {
        super(patientId, firstName, lastName, phone, email);
        this.dob     = dob;
        this.gender  = gender;
        this.address = address;
        this.status  = status;
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
        setDeletedAt(deletedAt);
    }

    // ── BaseEntity contracts ──────────────────────────────────────────────────

    @Override
    public String getEntityType() { return "patient"; }

    @Override
    public String getDisplayTitle() { return "Patient"; }

    // ── Domain alias for ID ───────────────────────────────────────────────────

    /** Alias for getId() — kept for PropertyValueFactory compatibility. */
    public String getPatientId() { return getId(); }
    public void setPatientId(String id) { setId(id); }

    // ── Patient-specific fields ───────────────────────────────────────────────

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}