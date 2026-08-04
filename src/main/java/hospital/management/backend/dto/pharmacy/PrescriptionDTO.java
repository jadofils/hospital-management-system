package hospital.management.backend.dto.pharmacy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PrescriptionDTO {

    private String               prescriptionId;
    private String               appointmentId;
    private LocalDate            dateIssued;
    private LocalDateTime        createdAt;
    private List<PrescriptionItemDTO> items;
    private String               status = "PENDING";

    public PrescriptionDTO() {}

    public PrescriptionDTO(String prescriptionId, String appointmentId,
                           LocalDate dateIssued, LocalDateTime createdAt,
                           List<PrescriptionItemDTO> items) {
        this.prescriptionId = prescriptionId;
        this.appointmentId  = appointmentId;
        this.dateIssued     = dateIssued;
        this.createdAt      = createdAt;
        this.items          = items;
    }

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public LocalDate getDateIssued() { return dateIssued; }
    public void setDateIssued(LocalDate dateIssued) { this.dateIssued = dateIssued; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<PrescriptionItemDTO> getItems() { return items; }
    public void setItems(List<PrescriptionItemDTO> items) { this.items = items; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "PrescriptionDTO{prescriptionId='" + prescriptionId + "', appointmentId='" + appointmentId + "'}";
    }
}