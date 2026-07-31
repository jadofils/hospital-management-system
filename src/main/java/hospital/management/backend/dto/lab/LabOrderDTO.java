package hospital.management.backend.dto.lab;

import java.time.LocalDateTime;

public class LabOrderDTO {

    private String        labOrderId;
    private String        appointmentId;
    private String        doctorId;
    private String        testName;
    private String        status;
    private LocalDateTime orderedAt;

    public LabOrderDTO() {}

    public LabOrderDTO(String labOrderId, String appointmentId, String doctorId,
                       String testName, String status, LocalDateTime orderedAt) {
        this.labOrderId    = labOrderId;
        this.appointmentId = appointmentId;
        this.doctorId      = doctorId;
        this.testName      = testName;
        this.status        = status;
        this.orderedAt     = orderedAt;
    }

    public String getLabOrderId() { return labOrderId; }
    public void setLabOrderId(String labOrderId) { this.labOrderId = labOrderId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getOrderedAt() { return orderedAt; }
    public void setOrderedAt(LocalDateTime orderedAt) { this.orderedAt = orderedAt; }

    @Override
    public String toString() {
        return "LabOrderDTO{labOrderId='" + labOrderId + "', testName='" + testName + "', status='" + status + "'}";
    }
}