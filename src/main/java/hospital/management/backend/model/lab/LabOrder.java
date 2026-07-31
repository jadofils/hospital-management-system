package hospital.management.backend.model.lab;

import hospital.management.backend.model.base.BaseEntity;

import java.time.LocalDateTime;

public class LabOrder extends BaseEntity {

    private String appointmentId;
    private String doctorId;
    private String testName;
    private String status;

    public LabOrder() {}

    public LabOrder(String labOrderId, String appointmentId, String doctorId,
                    String testName, String status, LocalDateTime orderedAt,
                    LocalDateTime updatedAt, LocalDateTime deletedAt) {
        super(labOrderId);
        this.appointmentId = appointmentId;
        this.doctorId      = doctorId;
        this.testName      = testName;
        this.status        = status;
        setCreatedAt(orderedAt);   // orderedAt is this table's created_at
        setUpdatedAt(updatedAt);
        setDeletedAt(deletedAt);
    }

    // ── BaseEntity contracts ──────────────────────────────────────────────────

    @Override
    public String getEntityType() { return "lab_order"; }

    @Override
    public String getSummary() {
        return "LabOrder[" + testName + "] — " + status;
    }

    // ── Domain alias for ID ───────────────────────────────────────────────────

    public String getLabOrderId() { return getId(); }
    public void setLabOrderId(String id) { setId(id); }

    /** Alias for getCreatedAt() — matches the DB column name ordered_at. */
    public LocalDateTime getOrderedAt() { return getCreatedAt(); }
    public void setOrderedAt(LocalDateTime orderedAt) { setCreatedAt(orderedAt); }

    // ── LabOrder-specific fields ──────────────────────────────────────────────

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}