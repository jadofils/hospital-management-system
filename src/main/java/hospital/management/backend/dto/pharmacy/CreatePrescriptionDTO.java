package hospital.management.backend.dto.pharmacy;

import java.time.LocalDate;
import java.util.List;

public class CreatePrescriptionDTO {

    private String                     appointmentId;
    private LocalDate                  dateIssued;
    private List<CreatePrescriptionItemDTO> items;

    public CreatePrescriptionDTO() {}

    public CreatePrescriptionDTO(String appointmentId, LocalDate dateIssued,
                                 List<CreatePrescriptionItemDTO> items) {
        this.appointmentId = appointmentId;
        this.dateIssued    = dateIssued;
        this.items         = items;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public LocalDate getDateIssued() { return dateIssued; }
    public void setDateIssued(LocalDate dateIssued) { this.dateIssued = dateIssued; }

    public List<CreatePrescriptionItemDTO> getItems() { return items; }
    public void setItems(List<CreatePrescriptionItemDTO> items) { this.items = items; }

    @Override
    public String toString() {
        return "CreatePrescriptionDTO{appointmentId='" + appointmentId + "'}";
    }
}